package com.smockin.mockserver.engine;

import com.icegreen.greenmail.foedus.util.MsgRangeFilter;
import com.icegreen.greenmail.imap.ImapHostManager;
import com.icegreen.greenmail.imap.commands.IdRange;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.store.FolderListener;
import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.store.StoredMessage;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.persistence.entity.MailMock;
import com.smockin.admin.service.MailMockMessageService;
import com.smockin.mockserver.dto.*;
import com.smockin.mockserver.exception.MockServerException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.util.MimeMessageParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.mail.*;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Transactional
public class MockedMailServerEngine {

    private final Logger logger = LoggerFactory.getLogger(MockedMailServerEngine.class);

    private String host = "0.0.0.0";
    private String defaultMailUserPassword = "letmein";
    private static final String MULTIPART = "multipart";

    private GreenMail greenMail;
    private ImapHostManager imapHostManager;
    private final Object serverStateMonitor = new Object();
    private MockServerState serverState = new MockServerState(false, 0);
    private final ConcurrentHashMap<String, GreenMailUserWrapper> mailUsersMap = new ConcurrentHashMap<>(0);

    @Autowired
    private MailMockMessageService mailMockMessageService;


    public void start(final MockedServerConfigDTO configDTO,
                      final List<MailMock> mailInboxes) throws MockServerException {
        logger.debug("started called");

        synchronized (serverStateMonitor) {

            try {

                final ServerSetup mailServerSetup = new ServerSetup(configDTO.getPort(), host, ServerSetup.PROTOCOL_SMTP);
                greenMail = new GreenMail(mailServerSetup);
                imapHostManager = greenMail.getManagers().getImapHostManager();

                createAllMailUsers(mailInboxes);

                greenMail.start();

                serverState.setRunning(true);
                serverState.setPort(configDTO.getPort());

            } catch (Exception e) {
                throw new MockServerException("Error starting mail mock engine", e);
            }

        }

    }

    public MockServerState getCurrentState() throws MockServerException {
        synchronized (serverStateMonitor) {
            return serverState;
        }
    }

    public void shutdown() throws MockServerException {
        logger.debug("shutdown called");

        if (greenMail == null) {
            return;
        }

        try {

            synchronized (serverStateMonitor) {
                greenMail.stop();
                serverState.setRunning(false);
            }

        } catch (Exception e) {
            throw new MockServerException("Error shutting down mail mock engine", e);
        }

    }

    void createAllMailUsers(final List<MailMock> mailAddresses) throws MessagingException, FolderException {
        logger.debug("createMailUsers called");

        mailUsersMap.clear();

        if (mailAddresses == null) {
            return;
        }

        for (final MailMock mailMock : mailAddresses) {
            addMailUser(mailMock);
        }

    }

    public void addMailUser(final MailMock mailMock) throws FolderException {
        logger.debug("addMailUser called");

        final GreenMailUser user =
                greenMail.setUser(mailMock.getAddress(), mailMock.getAddress(), defaultMailUserPassword);

        final FolderListener folderListener = (mailMock.isSaveReceivedMail())
            ? applySaveMailListener(user, mailMock)
            : null;

        mailUsersMap.putIfAbsent(mailMock.getAddress(), new GreenMailUserWrapper(user, folderListener));
    }

    public void removeMailUser(final MailMock mailMock) {
        logger.debug("removeMailUser called");

        final GreenMailUserWrapper user = findGreenMailUser(mailMock.getAddress());

        user.getUser().delete();

        mailUsersMap.remove(mailMock.getAddress());
    }

    public void addListenerForMailUser(final MailMock mailMock) {
        logger.debug("addListenerForMailUser called");

        final GreenMailUserWrapper user = findGreenMailUser(mailMock.getAddress());

        try {
            user.setListener(applySaveMailListener(user.getUser(), mailMock));
        } catch (Exception e) {
            logger.error("Error adding listener for user: " + mailMock.getAddress(), e);
        }

    }

    public void removeListenerForMailUser(final MailMock mailMock) {
        logger.debug("removeListenerForMailUser called");

        final GreenMailUserWrapper user = findGreenMailUser(mailMock.getAddress());

        try {

            if (user.getListener() != null) {
                imapHostManager.getInbox(user.getUser()).removeListener(user.getListener());
                user.setListener(null);
            }

        } catch (Exception e) {
            logger.error("Error removing listener for user: " + mailMock.getAddress(), e);
        }

    }

    FolderListener applySaveMailListener(final GreenMailUser user,
                                         final MailMock mailMock) throws FolderException {

        // Note, we cannot pass the MailMock entity into this listener as there will be not be a
        // transaction available at the time this executes, so will pass in the externalId.
        final String mailMockExtId = mailMock.getExtId();

        FolderListener folderListener;

        imapHostManager.getInbox(user)
                .addListener(folderListener = new FolderListener() {

            public void added(final int i) {

                try {

                    final MailFolder inbox = imapHostManager.getInbox(user);
                    final StoredMessage storedMessage = inbox.getMessage(i);
                    final MimeMessage message = storedMessage.getMimeMessage();

                    final String externalId = mailMockMessageService.saveMailMessage(
                            mailMockExtId,
                            extractMailSender(message),
                            message.getSubject(),
                            extractMailBodyContent(message),
                            message.getReceivedDate(),
                            Optional.empty());

                    extractAndSaveAllAttachments(externalId, message);

                    inbox.expunge(new IdRange[] { new IdRange(i) });

                } catch (Exception e) {
                    logger.error("Error saving received mail message to DB", e);
                }

            }

            public void mailboxDeleted() {}
            public void expunged(final int i) {}
            public void flagsUpdated(final int i, final Flags flags, final Long aLong) {}

        });

        return folderListener;
    }

    public List<MailServerMessageInboxDTO> getMessagesFromMailServerInbox(final String address) throws MockServerException {
        logger.debug("getAllEmailsForAddress called");

        final GreenMailUserWrapper user = findGreenMailUser(address);

        final List<StoredMessage> storedMessages;

        try {
            storedMessages = imapHostManager.getInbox(user.getUser()).getMessages();
        } catch (FolderException e) {
            throw new MockServerException(String.format("Error loading inbox for user '%s'", address), e);
        }

        return storedMessages
                .stream()
                .map(m -> {

                    final MimeMessage message = m.getMimeMessage();

                    try {

                        return new MailServerMessageInboxDTO(
                                m.getUid(),
                                extractMailSender(message),
                                message.getSubject(),
                                extractMailBodyContent(message),
                                message.getReceivedDate(),
                                countMessageAttachments(message));

                    } catch (Exception e) {
                        logger.error(String.format("Error reading message from mail server for address '%s'", address), e);
                        return null;
                    }

                })
                .collect(Collectors.toList());
    }

    public int getMessageCountFromMailServerInbox(final String address) throws MockServerException {
        logger.debug("getMessageCountFromMailServerInbox called");

        final GreenMailUserWrapper user = findGreenMailUser(address);

        try {
            return imapHostManager.getInbox(user.getUser()).getMessageCount();
        } catch (FolderException e) {
            throw new MockServerException(String.format("Error loading inbox for user '%s'", address), e);
        }

    }

    public List<MailServerMessageInboxAttachmentDTO> getMessageAttachmentsFromMailServerInbox(final String address,
                                                                                              final String messageId) throws MockServerException {
        logger.debug("getMessageAttachmentsFromMailServerInbox called");

        final GreenMailUserWrapper user = findGreenMailUser(address);

        final Optional<StoredMessage> storedMessageOpt;

        try {

            storedMessageOpt = imapHostManager.getInbox(user.getUser())
                    .getMessages(new MsgRangeFilter(messageId, true))
                    .stream()
                    .findFirst();

        } catch (Exception e) {
            throw new MockServerException(String.format("Error loading inbox for user '%s'", address), e);
        }

        if (!storedMessageOpt.isPresent()) {
            throw new MockServerException(String.format("Error locating inbox message for user '%s'", address));
        }

        return extractAllMessageAttachments(storedMessageOpt.get().getMimeMessage());
    }

    public void purgeAllMailServerInboxMessages(final String address) throws MockServerException {
        logger.debug("purgeAllMailServerInboxMessages called");

        final GreenMailUserWrapper user = findGreenMailUser(address);

        try {
            imapHostManager.getInbox(user.getUser()).deleteAllMessages();
        } catch (FolderException e) {
            throw new MockServerException(String.format("Error deleting inbox for user '%s'", address), e);
        }

    }

    String extractMailSender(final MimeMessage message) throws MessagingException {

        if (message.getSender() != null) {
            return message.getSender().toString();
        }

        final Address[] fromAddresses = message.getFrom();

        if (fromAddresses != null && fromAddresses.length != 0) {
            return fromAddresses[0].toString();
        }

        throw new MessagingException("Unable to determine mail sender");
    }

    GreenMailUserWrapper findGreenMailUser(final String address) {

        final GreenMailUserWrapper user = mailUsersMap.get(address);

        if (user == null) {
            throw new RecordNotFoundException();
        }

        return user;
    }

    List<MailServerMessageInboxAttachmentDTO> extractAllMessageAttachments(final MimeMessage mimeMessage) throws MockServerException {

        try {

            final List<MailServerMessageInboxAttachmentDTO> attachmentDTOs = new ArrayList<>();

            if (!mimeMessage.getContentType().contains(MULTIPART)
                    || !(mimeMessage.getContent() instanceof Multipart)) {
                return attachmentDTOs;
            }

            final Multipart multipart = (Multipart) mimeMessage.getContent();

            for (int i=0; i < multipart.getCount(); i++) {

                if (multipart.getBodyPart(i) instanceof MimeBodyPart) {

                    final MimeBodyPart mimeBodyPart = (MimeBodyPart)multipart.getBodyPart(i);

                    if (Part.ATTACHMENT.equalsIgnoreCase(mimeBodyPart.getDisposition())) {

                        attachmentDTOs.add(new MailServerMessageInboxAttachmentDTO(
                                Optional.empty(),
                                mimeBodyPart.getFileName(),
                                sanitizeContentType(mimeBodyPart.getContentType()),
                                IOUtils.toString(mimeBodyPart.getInputStream(), Charset.defaultCharset())));
                    }

                }

            }

            return attachmentDTOs;

        } catch (Exception e) {
            throw new MockServerException("Error extracting attachment(s) from mail message", e);
        }

    }

    int countMessageAttachments(final MimeMessage mimeMessage) throws MockServerException {

        try {

            if (!mimeMessage.getContentType().contains(MULTIPART)
                    || !(mimeMessage.getContent() instanceof Multipart)) {
                return 0;
            }

            final Multipart multipart = (Multipart) mimeMessage.getContent();

            int counter = 0;

            for (int i=0; i < multipart.getCount(); i++) {

                if (multipart.getBodyPart(i) instanceof MimeBodyPart
                        && Part.ATTACHMENT.equalsIgnoreCase((multipart.getBodyPart(i)).getDisposition())) {
                    counter++;
                }
            }

            return counter;

        } catch (Exception e) {
            throw new MockServerException("Error extracting attachment count for mail message", e);
        }

    }

    String extractMailBodyContent(final MimeMessage message) throws Exception {

        final String content = extractHtmlContent(message);

        if (content != null) {
            return content;
        }

        return extractPlainContent(message);
    }

    String extractHtmlContent(final MimeMessage message) throws Exception {
        return new MimeMessageParser(message).parse().getHtmlContent();
    }

    String extractPlainContent(final MimeMessage message) throws Exception {
        return new MimeMessageParser(message).parse().getPlainContent();
    }

    void extractAndSaveAllAttachments(final String messageExternalId,
                                      final MimeMessage mimeMessage) {

        extractAllMessageAttachments(mimeMessage)
                .stream()
                .forEach(a ->
                        mailMockMessageService.saveMailMessageAttachment(
                            messageExternalId,
                            a.getName(),
                            a.getMimeType(),
                            a.getContent()));

    }

    String sanitizeContentType(final String contentType) {
        return StringUtils.substringBefore(contentType, ";");
    }

}
