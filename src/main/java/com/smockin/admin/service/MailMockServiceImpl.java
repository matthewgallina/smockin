package com.smockin.admin.service;

import com.icegreen.greenmail.store.FolderException;
import com.smockin.admin.dto.MailMockDTO;
import com.smockin.admin.dto.response.MailMockMessageResponseDTO;
import com.smockin.admin.dto.response.MailMockResponseDTO;
import com.smockin.admin.dto.response.MailMockResponseLiteDTO;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.MailMockDAO;
import com.smockin.admin.persistence.entity.*;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import com.smockin.mockserver.dto.MailServerMessageInboxAttachmentDTO;
import com.smockin.mockserver.dto.MailServerMessageInboxDTO;
import com.smockin.mockserver.engine.MockedMailServerEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;
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

        final Integer messageCountInDBInt = mailMockDAO.findMessageCountByMailMockId(mailMock.getId());
        final int messageCountInDB = (messageCountInDBInt != null)
                                        ? messageCountInDBInt.intValue()
                                        : 0;

        final int messageCountInServer =
                (!mailMock.isSaveReceivedMail()
                        && mockedServerEngineService.getMailServerState().isRunning())
                            ? mockedMailServerEngine.getMessageCountFromMailServerInbox(mailMock.getExtId())
                            : 0;

        return messageCountInDB + messageCountInServer;
    }

    public MailMockResponseDTO loadByIdWithFilteredMessages(final String externalId,
                                                            final Optional<String> sender,
                                                            final Optional<String> subject,
                                                            final Optional<String> dateReceived,
                                                            final String token) throws RecordNotFoundException {

        final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);

        final MailMock mailMock = loadById(externalId, smockinUser);

        MailMockResponseDTO dto = new MailMockResponseDTO(
                mailMock.getExtId(),
                mailMock.getDateCreated(),
                mailMock.getMessages().size(),
                mailMock.getAddress(),
                mailMock.getStatus(),
                mailMock.isSaveReceivedMail());

        // TODO filter messages

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

    public void update(final String externalId,
                       final MailMockDTO mailMockDTO,
                       final Boolean retainCachedMail,
                       final String token) throws RecordNotFoundException, ValidationException {

        final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);

        final MailMock mailMock = loadById(externalId, smockinUser);

        final RecordStatusEnum currentStatus = mailMock.getStatus();

        mailMock.setAddress(mailMockDTO.getAddress());
        mailMock.setStatus(mailMockDTO.getStatus());
        mailMock.setSaveReceivedMail(mailMockDTO.isSaveReceivedMail());

        mailMockDAO.save(mailMock);

        if (mockedServerEngineService.getMailServerState().isRunning()) {

            if (!currentStatus.equals(mailMockDTO.getStatus())) {

                if (RecordStatusEnum.ACTIVE.equals(mailMockDTO.getStatus())) {

                    try {
                        mockedMailServerEngine.enableMailUser(mailMock);
                    } catch (FolderException e) {
                        logger.error("Error adding activated user to mail server", e);
                    }

                } else if (RecordStatusEnum.INACTIVE.equals(mailMockDTO.getStatus())) {

                    mockedMailServerEngine.suspendMailUser(mailMock);
                }

                return;
            }

            // Update user's mail box listener (DB or Cache) on mail server
            mockedMailServerEngine.removeListenerForMailUser(mailMock);
            mockedMailServerEngine.addListenerForMailUser(mailMock);

            if (mailMockDTO.isSaveReceivedMail() && (retainCachedMail != null && retainCachedMail)) {
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

    public List<MailServerMessageInboxDTO> searchForMessagesFromMailServerInbox(
            final String externalId,
            final Optional<String> sender,
            final Optional<String> subject,
            final Optional<String> dateReceived,
            final String token) throws ValidationException {

        final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);

        final MailMock mailMock = loadById(externalId, smockinUser);

        if (!mockedServerEngineService.getMailServerState().isRunning()) {
            throw new ValidationException("Mail server is not running");
        }

        // TODO

        return null;
    }

    private void handleSaveCurrentInbox(final MailMock mailMock) {

        final List<MailServerMessageInboxDTO> mailMessages = mockedMailServerEngine.getMessagesFromMailServerInbox(mailMock.getExtId());

        mailMessages.stream()
                .forEach(m ->
                    saveMockMessage(
                    mailMock,
                    m.getCacheID(),
                    m.getFrom(),
                    m.getSubject(),
                    m.getBody(),
                    m.getDateReceived()));

        mockedMailServerEngine.purgeAllMailServerInboxMessages(mailMock.getExtId());
    }

    private void saveMockMessage(final MailMock mailMock,
                                final String messageId,
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

        final List<MailMockMessageAttachment> attachments = mockedMailServerEngine
                .getMessageAttachmentsFromMailServerInbox(mailMock.getExtId(), messageId)
                .stream()
                .map(dto ->
                        toMailMockMessageAttachment(dto, mailMockMessage))
                .collect(Collectors.toList());

        mailMockMessage.getAttachments().addAll(attachments);

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

    private MailMockMessageAttachment toMailMockMessageAttachment(final MailServerMessageInboxAttachmentDTO dto,
                                                   final MailMockMessage mailMockMessage) {

        final MailMockMessageAttachment attachment = new MailMockMessageAttachment();
        attachment.setName(dto.getName());
        attachment.setMimeType(dto.getMimeType());
        attachment.setMailMockMessage(mailMockMessage);

        final MailMockMessageAttachmentContent attachmentContent = new MailMockMessageAttachmentContent();
        attachmentContent.setContent(dto.getBase64Content());
        attachmentContent.setMailMockMessageAttachment(attachment);

        attachment.setMailMockMessageAttachmentContent(attachmentContent);

        return attachment;
    }

}
