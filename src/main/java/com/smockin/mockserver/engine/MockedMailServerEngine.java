package com.smockin.mockserver.engine;

import com.icegreen.greenmail.imap.ImapHostManager;
import com.icegreen.greenmail.imap.commands.IdRange;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.store.FolderListener;
import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.store.StoredMessage;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetup;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.persistence.entity.MailMock;
import com.smockin.mockserver.dto.GreenMailUserWrapper;
import com.smockin.mockserver.dto.MailServerMessageInboxDTO;
import com.smockin.mockserver.dto.MockServerState;
import com.smockin.mockserver.dto.MockedServerConfigDTO;
import com.smockin.mockserver.exception.MockServerException;
import com.smockin.mockserver.service.MailMockMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Transactional
public class MockedMailServerEngine {

    private final Logger logger = LoggerFactory.getLogger(MockedMailServerEngine.class);

    private String host = "0.0.0.0";
    private String defaultMailUserPassword = "letmein";

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

//        populateAddressInboxHistory(mailMock, user);

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
            final FolderListener folderListener = applySaveMailListener(user.getUser(), mailMock);
            user.setListener(folderListener);
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

//                    if (!storedMessage.getFlags().contains(Flags.Flag.SEEN)) {

                        final MimeMessage message = storedMessage.getMimeMessage();

                        mailMockMessageService.saveMessage(
                                mailMockExtId,
                                extractMailSender(message),
                                message.getSubject(), GreenMailUtil.getBody(message),
                                message.getReceivedDate());

                        inbox.expunge(new IdRange[] { new IdRange(i) });
//                    }

                    /*

                    System.out.println("inbox.getUnseenCount(): " + inbox.getUnseenCount());

                    final long[] nos = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));

                    for (long no : nos) {
                        System.out.println("no: " + no);
                    }

                    System.out.println("inbox.getFirstUnseen() " + inbox.getFirstUnseen());

                    if (inbox.getFirstUnseen() > -1) {
                        StoredMessage storedMessage = inbox.getMessage(inbox.getFirstUnseen());
                        if (storedMessage != null) {
                            System.out.println("subject: " + storedMessage.getMimeMessage().getSubject());
                        }
                    }
                    */

                } catch (Exception e) {
                    logger.error("Error saving received mail message to DB", e);
                }

            }

            public void mailboxDeleted() {}
            public void expunged(int i) {}
            public void flagsUpdated(int i, Flags flags, Long aLong) {}

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
                                extractMailSender(message),
                                message.getReceivedDate(),
                                message.getSubject(),
                                GreenMailUtil.getBody(message));

                    } catch (MessagingException e) {
                        logger.error(String.format("Error reading message from mail server for address '%s'", address), e);
                        return null;
                    }

                })
                .collect(Collectors.toList());
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

    /*
    // TODO is this necessary, adding messages from DB to the mail server...?!
    void populateAddressInboxHistory(final MailMock mailMock,
                                     final GreenMailUser user) throws MessagingException, FolderException {
        logger.debug("populateAddressInboxHistory called");

        final List<MailMockMessage> messages = mailMock.getMessages();

        if (!messages.isEmpty()) {
            return;
        }

        messages.stream()
                .forEach(m ->
                    createMessage(m, user));
    }

    void createMessage(final MailMockMessage message,
                       final GreenMailUser user) {

        try {

            final MailFolder inbox = imapHostManager.getInbox(user);
            final MimeMessage mimeMessage = new MimeMessage((Session) null);
            mimeMessage.setFrom(new InternetAddress(message.getFrom()));
            mimeMessage.setSubject(message.getSubject());
            mimeMessage.setSentDate(message.getDateReceived());

            final BodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText(message.getBody());

            final Multipart multipart = new MimeMultipart();

            // Attachments
//          final MimeBodyPart attachmentPart = new MimeBodyPart();
//          attachmentPart.attachFile(new File("/Users/gallina/Downloads/comments-regular.svg"));;
//          multipart.addBodyPart(attachmentPart);

            mimeMessage.setContent(multipart);
            multipart.addBodyPart(messageBodyPart);

            mimeMessage.setFlag(Flags.Flag.SEEN, true);
            mimeMessage.saveChanges();

            inbox.store(mimeMessage);

        } catch (Exception e) {
            logger.error("Error creating mock mail message on mail server", e);
        }

    }
    */

}
