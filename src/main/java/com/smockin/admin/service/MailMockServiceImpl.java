package com.smockin.admin.service;

import com.icegreen.greenmail.store.FolderException;
import com.smockin.admin.dto.MailMockDTO;
import com.smockin.admin.dto.response.MailMockMessageResponseDTO;
import com.smockin.admin.dto.response.MailMockResponseDTO;
import com.smockin.admin.dto.response.MailMockResponseLiteDTO;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.MailMockDAO;
import com.smockin.admin.persistence.entity.MailMock;
import com.smockin.admin.persistence.entity.MailMockMessage;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import com.smockin.mockserver.dto.MailServerMessageInboxDTO;
import com.smockin.mockserver.engine.MockedMailServerEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class MailMockServiceImpl implements MailMockService {

    private final Logger logger = LoggerFactory.getLogger(MailMockServiceImpl.class);

    @Autowired
    private UserTokenServiceUtils userTokenServiceUtils;

    @Autowired
    private MailMockDAO mailMockDAO;

    @Autowired
    private MockedServerEngineService mockedServerEngineService;

    @Autowired
    private MockedMailServerEngine mockedMailServerEngine;


    public List<MailMockResponseLiteDTO> loadAll(final String token) throws RecordNotFoundException {

        final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);

        return mailMockDAO.findAllByUser(smockinUser.getId())
                .stream()
                .map(m ->
                    new MailMockResponseLiteDTO(
                            m.getExtId(),
                            m.getDateCreated(),
                            retrieveReceivedMessageCount(m),
                            m.getAddress(),
                            m.getStatus(),
                            m.isSaveReceivedMail()))
                .collect(Collectors.toList());
    }

    int retrieveReceivedMessageCount(final MailMock mailMock) {

        return (mailMock.isSaveReceivedMail())
                ? mailMockDAO.findMessageCountByMailMockId(mailMock.getId())
                : ((mockedServerEngineService.getMailServerState().isRunning()))
                    ? mockedMailServerEngine.getMessageCountFromMailServerInbox(mailMock.getExtId())
                    : 0;
    }

    public MailMockResponseDTO loadById(final String externalId, final String token) throws RecordNotFoundException {

        final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);

        final MailMock mailMock = loadById(externalId, smockinUser);

        MailMockResponseDTO dto = new MailMockResponseDTO(
                mailMock.getExtId(),
                mailMock.getDateCreated(),
                mailMock.getMessages().size(),
                mailMock.getAddress(),
                mailMock.getStatus(),
                mailMock.isSaveReceivedMail());

        dto.setMessages(mailMock
                .getMessages()
                .stream()
                .map(m ->
                        new MailMockMessageResponseDTO(
                            m.getExtId(),
                            m.getFrom(),
                            m.getSubject(),
                            m.getBody(),
                            m.getDateReceived(),
                            m.getAttachments().size()))
                .collect(Collectors.toList()));

        return dto;
    }

    public String create(final MailMockDTO mailMockDTO, final String token) throws RecordNotFoundException, ValidationException {

        final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);

        final MailMock mailMock = new MailMock(mailMockDTO.getAddress(), mailMockDTO.getStatus(), smockinUser, mailMockDTO.isSaveReceivedMail());

        final String extId = mailMockDAO.save(mailMock).getExtId();

        if (mockedServerEngineService.getMailServerState().isRunning()) {
            // Add user to mail server
            try {
                mockedMailServerEngine.addMailUser(mailMock);
            } catch (FolderException e) {
                logger.error("Error adding user to mail server", e);
            }
        }

        return extId;
    }

    public void update(final String externalId, final MailMockDTO mailMockDTO, final String token) throws RecordNotFoundException, ValidationException {

        final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);

        final MailMock mailMock = loadById(externalId, smockinUser);

        mailMock.setAddress(mailMockDTO.getAddress());
        mailMock.setStatus(mailMockDTO.getStatus());
        mailMock.setSaveReceivedMail(mailMockDTO.isSaveReceivedMail());

        mailMockDAO.save(mailMock);

        if (mockedServerEngineService.getMailServerState().isRunning()) {

            // Update user's mail box listener (DB or Cache) on mail server
            mockedMailServerEngine.removeListenerForMailUser(mailMock);
            mockedMailServerEngine.addListenerForMailUser(mailMock);

            if (mailMockDTO.isSaveReceivedMail()) {
                handleSaveCurrentInbox(mailMock);
//                mockedMailServerEngine.purgeAllMailServerInboxMessages(mailMock.getAddress());
            }
        }
    }

    public void delete(final String externalId, final String token) throws RecordNotFoundException {

        final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);

        final MailMock mailMock = loadById(externalId, smockinUser);

        if (mockedServerEngineService.getMailServerState().isRunning()) {
            // Delete user's mail box from mail server
            mockedMailServerEngine.removeMailUser(mailMock);
        }

        mailMockDAO.delete(mailMock);
    }

    public List<MailServerMessageInboxDTO> loadMessagesFromMailServerInbox(final String externalId, final String token)
            throws ValidationException {

        final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);

        final MailMock mailMock = loadById(externalId, smockinUser);

        if (!mockedServerEngineService.getMailServerState().isRunning()) {
            throw new ValidationException("Mail server is not running");
        }

        return mockedMailServerEngine.getMessagesFromMailServerInbox(mailMock.getExtId());
    }

    private void handleSaveCurrentInbox(final MailMock mailMock) {

        final List<MailServerMessageInboxDTO> mailMessages = mockedMailServerEngine.getMessagesFromMailServerInbox(mailMock.getExtId());

        mailMessages.stream()
                .forEach(m ->
                    saveMockMessage(mailMock,
                    m.getFrom(),
                    m.getSubject(),
                    m.getBody(),
                    m.getDateReceived()));

    }

    private void saveMockMessage(final MailMock mailMock,
                                final String sender,
                                final String subject,
                                final String body,
                                final Date dateReceived) {

        final MailMockMessage mailMockMessage = new MailMockMessage();
        mailMockMessage.setFrom(sender);
        mailMockMessage.setDateReceived(dateReceived);
        mailMockMessage.setSubject(subject);
        mailMockMessage.setBody(body);
        mailMockMessage.setMailMock(mailMock);

        mailMock.getMessages().add(mailMockMessage);

        mailMockDAO.save(mailMock);
    }

    private MailMock loadById(final String extId, final SmockinUser smockinUser) throws RecordNotFoundException {

        final MailMock mailMock = mailMockDAO.findByExtIdAndUser(extId, smockinUser.getId());

        if (mailMock == null) {
            throw new RecordNotFoundException();
        }

        return mailMock;
    }

}
