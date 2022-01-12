package com.smockin.mockserver.engine;

import com.icegreen.greenmail.imap.ImapHostManager;
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
import com.smockin.admin.persistence.entity.MailMockMessage;
import com.smockin.mockserver.dto.MailMessageDTO;
import com.smockin.mockserver.dto.MockServerState;
import com.smockin.mockserver.dto.MockedServerConfigDTO;
import com.smockin.mockserver.exception.MockServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class MockedMailServerEngine {

    private final Logger logger = LoggerFactory.getLogger(MockedMailServerEngine.class);

    private String host = "0.0.0.0";
    private String defaultMailUserPassword = "letmein";

    private GreenMail greenMail;
    private ImapHostManager imapHostManager;
    private final Object serverStateMonitor = new Object();
    private MockServerState serverState = new MockServerState(false, 0);
    private final ConcurrentHashMap<String, GreenMailUser> mailUsersMap = new ConcurrentHashMap<>(0);

    public void start(final MockedServerConfigDTO configDTO,
                      final List<MailMock> mailInboxes) throws MockServerException {
        logger.debug("started called");

        synchronized (serverStateMonitor) {

            try {

                final ServerSetup mailServerSetup = new ServerSetup(configDTO.getPort(), host, ServerSetup.PROTOCOL_SMTP);
                greenMail = new GreenMail(mailServerSetup);
                imapHostManager = greenMail.getManagers().getImapHostManager();

                createMailUsers(mailInboxes);

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

    void createMailUsers(final List<MailMock> mailAddresses) throws MessagingException, FolderException {
        logger.debug("createMailUsers called");

        mailUsersMap.clear();

        if (mailAddresses == null) {
            return;
        }

        for (final MailMock mailMock : mailAddresses) {
            addMailUser(mailMock);
        }

    }

    public void addMailUser(final MailMock mailMock) throws MessagingException, FolderException {
        logger.debug("addMailUser called");

        final GreenMailUser user =
                greenMail.setUser(mailMock.getAddress(), mailMock.getAddress(), defaultMailUserPassword);

        imapHostManager.getInbox(user).addListener(new FolderListener() {
            @Override
            public void expunged(int i) {
                System.out.println("expunged " + i);
            }

            @Override
            public void added(int i) {
                System.out.println("added " + i);
            }

            @Override
            public void flagsUpdated(int i, Flags flags, Long aLong) {
                System.out.println("flagsUpdated " + i);
                System.out.println(flags);
                System.out.println(aLong);
            }

            @Override
            public void mailboxDeleted() {
                System.out.println("mailboxDeleted");
            }
        });

        mailUsersMap.putIfAbsent(mailMock.getAddress(), user);

        populateAddressInboxHistory(mailMock, user);

    }

    public List<MailMessageDTO> getAllEmailsForAddress(final String address) throws MockServerException {
        logger.debug("getAllEmailsForAddress called");

        final GreenMailUser user = mailUsersMap.get(address);

        if (user == null) {
            throw new RecordNotFoundException();
        }

        final List<StoredMessage> storedMessages;

        try {
            storedMessages = imapHostManager.getInbox(user).getMessages();
        } catch (FolderException e) {
            throw new MockServerException(String.format("Error loading inbox for user '%s'", address), e);
        }

        return storedMessages
                .stream()
                .map(m -> {

                    final MimeMessage message = m.getMimeMessage();

                    try {

                        return new MailMessageDTO(
                                extractMailSender(message),
                                message.getReceivedDate(),
                                message.getSubject(),
                                GreenMailUtil.getBody(message),
                                null);

                    } catch (MessagingException e) {
                        logger.error(String.format("Error reading message from mail server for user '%s'", address), e);
                        return null;
                    }

                })
                .collect(Collectors.toList());
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

    void populateAddressInboxHistory(final MailMock mailMock,
                                     final GreenMailUser user) throws MessagingException, FolderException {
        logger.debug("populateAddressInboxHistory called");

        final List<MailMockMessage> messages = mailMock.getMessages();

        if (!messages.isEmpty()) {
            return;
        }

        // todo

        // hardcoded example
        final MailFolder inbox = imapHostManager.getInbox(user);
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        mimeMessage.setFrom(new InternetAddress("from1@localhost.com"));
//        mimeMessage.setSender(new InternetAddress("sender2@localhost.com"));
        mimeMessage.setFrom("bob@smockin-testlabs.com");
        mimeMessage.setSubject("Foo Subject");
        mimeMessage.setSentDate(new Date());

        try {

            // Body 1 (with attachment)
            BodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText("Foo Mail Body");
            MimeBodyPart attachmentPart = new MimeBodyPart();
            attachmentPart.attachFile(new File("/Users/gallina/Downloads/comments-regular.svg"));
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);
            multipart.addBodyPart(attachmentPart);
            mimeMessage.setContent(multipart);

            mimeMessage.saveChanges();

            inbox.store(mimeMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
