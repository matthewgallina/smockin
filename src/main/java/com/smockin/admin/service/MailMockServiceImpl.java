package com.smockin.admin.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.icegreen.greenmail.store.FolderException;
import com.smockin.admin.dto.MailMockDTO;
import com.smockin.admin.dto.response.MailMockMessageResponseDTO;
import com.smockin.admin.dto.response.MailMockResponseDTO;
import com.smockin.admin.dto.response.MailMockResponseLiteDTO;
import com.smockin.admin.dto.response.PagingResponseDTO;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.MailMockDAO;
import com.smockin.admin.persistence.dao.MailMockMessageDAO;
import com.smockin.admin.persistence.entity.*;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import com.smockin.mockserver.dto.MailMessageSearchDTO;
import com.smockin.mockserver.dto.MailServerMessageInboxAttachmentDTO;
import com.smockin.mockserver.dto.MailServerMessageInboxDTO;
import com.smockin.mockserver.engine.MockedMailServerEngine;
import com.smockin.utils.GeneralUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    private MailMockMessageDAO mailMockMessageDAO;

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

    long retrieveReceivedMessageCount(final MailMock mailMock) {

        if (mailMock.isSaveReceivedMail()) {

            final Integer messageCountInDBInt = mailMockMessageDAO.countAllMessageByMailMockId(mailMock.getId());

            return (messageCountInDBInt != null)
                        ? messageCountInDBInt.intValue()
                        : 0;
        }

        return (mockedServerEngineService.getMailServerState().isRunning())
                    ? mockedMailServerEngine.getMessageCountFromMailServerInbox(mailMock.getExtId(), Optional.empty())
                    : 0;
    }

    public MailMockResponseDTO loadByIdWithFilteredMessages(final String externalId,
                                                            final Optional<String> sender,
                                                            final Optional<String> subject,
                                                            final Optional<String> dateReceived,
                                                            final int pageStart,
                                                            final String search,
                                                            final String token) throws RecordNotFoundException {

        final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);


        final MailMock mailMock = loadById(externalId, smockinUser);

        MailMockResponseDTO dto = new MailMockResponseDTO(
                mailMock.getExtId(),
                mailMock.getDateCreated(),
                0,
                mailMock.getAddress(),
                mailMock.getStatus(),
                mailMock.isSaveReceivedMail());

        final Pageable pageable = PageRequest.of(pageStart, GeneralUtils.DEFAULT_RECORDS_PER_PAGE);

        final Optional<MailMessageSearchDTO> mailMessageSearchDTO = toMailMessageSearchDTO(search);

        // Just supporting 'subject' in search for now...
        final Page<MailMockMessage> messages =
                (mailMessageSearchDTO.isPresent()
                    && StringUtils.isNotBlank(mailMessageSearchDTO.get().getSubject()))
                        ? mailMockMessageDAO.findAllMessageByMailMockIdAndMatchingSubject(
                            mailMock.getId(),
                            mailMessageSearchDTO.get().getSubject(),
                            pageable)
                        : mailMockMessageDAO.findAllMessageByMailMockId(mailMock.getId(), pageable);

        dto.setMessageCount(messages.getTotalElements());

        dto.setMessages(
                new PagingResponseDTO<>(
                        messages.getTotalElements(),
                    pageStart,
                    GeneralUtils.DEFAULT_RECORDS_PER_PAGE,
                    messages.getContent()
                            .stream()
                            .map(m ->
                                    new MailMockMessageResponseDTO(
                                            m.getExtId(),
                                            m.getFrom(),
                                            m.getSubject(),
                                            m.getBody(),
                                            m.getDateReceived(),
                                            m.getAttachments().size()))
                            .collect(Collectors.toList())));

        return dto;
    }

    public String create(final MailMockDTO mailMockDTO, final String token) throws RecordNotFoundException, ValidationException {

        final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);

        final MailMock mailMock = new MailMock(mailMockDTO.getAddress(), mailMockDTO.getStatus(), smockinUser, mailMockDTO.isSaveReceivedMail());

        final String extId = mailMockDAO.save(mailMock).getExtId();

        if (RecordStatusEnum.ACTIVE.equals(mailMockDTO.getStatus())
                && mockedServerEngineService.getMailServerState().isRunning()) {
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
        final boolean saveReceivedMailCurrentValue = mailMock.isSaveReceivedMail();

        // Purge all existing saved messages if moving away from auto save
        if (saveReceivedMailCurrentValue && !mailMockDTO.isSaveReceivedMail()) {
            mailMock.getMessages().clear();
        }

        mailMock.setAddress(mailMockDTO.getAddress());
        mailMock.setStatus(mailMockDTO.getStatus());
        mailMock.setSaveReceivedMail(mailMockDTO.isSaveReceivedMail());

        mailMockDAO.save(mailMock);

        if (mockedServerEngineService.getMailServerState().isRunning()) {

            // Mail Status change
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

            // Enabling auto-save mode: optionally saves cached mail and then purges this from the cache.
            if (!saveReceivedMailCurrentValue && mailMockDTO.isSaveReceivedMail()) {

                if (retainCachedMail != null && retainCachedMail) {
                    handleSaveCurrentInbox(mailMock);
                }

                mockedMailServerEngine.purgeAllMailServerInboxMessages(mailMock.getExtId());
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

    public PagingResponseDTO<MailServerMessageInboxDTO> loadMessagesFromMailServerInbox(final String externalId,
                                                                                        final int pageStart,
                                                                                        final String search,
                                                                                        final String token)
            throws ValidationException {

        final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);

        final MailMock mailMock = loadById(externalId, smockinUser);

        if (!mockedServerEngineService.getMailServerState().isRunning()) {
            throw new ValidationException("Mail server is not running");
        }

        final Optional<MailMessageSearchDTO> mailMessageSearchDTO = toMailMessageSearchDTO(search);

        final List<MailServerMessageInboxDTO> mailServerMessages = mockedMailServerEngine.getMessagesFromMailServerInbox(
                mailMock.getExtId(),
                mailMessageSearchDTO,
                Optional.of(pageStart));

        final long total = mockedMailServerEngine.getMessageCountFromMailServerInbox(mailMock.getExtId(), mailMessageSearchDTO);

        return new PagingResponseDTO<>(total, pageStart, GeneralUtils.DEFAULT_RECORDS_PER_PAGE, mailServerMessages);
    }

    private Optional<MailMessageSearchDTO> toMailMessageSearchDTO(final String search) {

        return (StringUtils.isNotBlank(search))
                ? Optional.of(GeneralUtils.deserialiseJson(search, new TypeReference<MailMessageSearchDTO>() {}))
                : Optional.empty();
    }

    private void handleSaveCurrentInbox(final MailMock mailMock) {

        final List<MailServerMessageInboxDTO> mailMessages = mockedMailServerEngine.getMessagesFromMailServerInbox(
                mailMock.getExtId(),
                Optional.empty(),
                Optional.empty());

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
