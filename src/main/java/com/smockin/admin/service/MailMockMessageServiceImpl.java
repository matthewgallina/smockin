package com.smockin.admin.service;

import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.MailMockDAO;
import com.smockin.admin.persistence.dao.MailMockMessageDAO;
import com.smockin.admin.persistence.entity.MailMock;
import com.smockin.admin.persistence.entity.MailMockMessage;
import com.smockin.admin.persistence.entity.MailMockMessageAttachment;
import com.smockin.admin.persistence.entity.MailMockMessageAttachmentContent;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import com.smockin.mockserver.dto.MailServerMessageInboxAttachmentDTO;
import com.smockin.mockserver.dto.MailServerMessageInboxAttachmentLiteDTO;
import com.smockin.mockserver.engine.MockedMailServerEngine;
import com.smockin.utils.GeneralUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class MailMockMessageServiceImpl implements MailMockMessageService {

    private final Logger logger = LoggerFactory.getLogger(MailMockMessageServiceImpl.class);

    @Autowired
    private MailMockDAO mailMockDAO;

    @Autowired
    private MailMockMessageDAO mailMockMessageDAO;

    @Autowired
    private UserTokenServiceUtils userTokenServiceUtils;

    @Autowired
    private MockedServerEngineService mockedServerEngineService;

    @Autowired
    private MockedMailServerEngine mockedMailServerEngine;


    @Override
    public String saveMailMessage(final String mailMockExtId,
                            final String sender,
                            final String subject,
                            final String body,
                            final Date dateReceived,
                            final Optional<String> tokenOpt) throws ValidationException {

        final MailMock mailMock = mailMockDAO.findByExtId(mailMockExtId);

        if (mailMock == null) {

            logger.error("Error locating mail mock with external ID: " + mailMockExtId);

            if (tokenOpt.isPresent()) {
                throw new RecordNotFoundException();
            }
        }

        if (tokenOpt.isPresent()) {
            userTokenServiceUtils.validateRecordOwner(mailMock.getCreatedBy(), tokenOpt.get());
        }

        final MailMockMessage mailMockMessage = new MailMockMessage();
        mailMockMessage.setFrom(sender);
        mailMockMessage.setDateReceived(dateReceived);
        mailMockMessage.setSubject(subject);
        mailMockMessage.setBody(body);
        mailMockMessage.setMailMock(mailMock);

        return mailMockMessageDAO.save(mailMockMessage).getExtId();
    }

    @Override
    public void deleteMailMessage(final String mailMessageExtId,
                                  final String token)
            throws ValidationException {

        final MailMockMessage mailMockMessage = mailMockMessageDAO.findByExtId(mailMessageExtId);

        if (mailMockMessage == null) {
            throw new RecordNotFoundException();
        }

        userTokenServiceUtils.validateRecordOwner(mailMockMessage.getMailMock().getCreatedBy(), token);

        mailMockMessageDAO.delete(mailMockMessage);
    }

    @Override
    public void deleteAllMailMessages(final String mailExtId, final String token) throws ValidationException {

        final MailMock mailMock = mailMockDAO.findByExtId(mailExtId);

        if (mailMock == null) {
            throw new RecordNotFoundException();
        }

        userTokenServiceUtils.validateRecordOwner(mailMock.getCreatedBy(), token);

        mailMock.getMessages().clear();

        mailMockDAO.save(mailMock);
    }

    @Override
    public void deleteAllMailMessagesOnServer(final String mailExtId, final String token) throws ValidationException {

        final MailMock mailMock = mailMockDAO.findByExtId(mailExtId);

        if (mailMock == null) {
            throw new RecordNotFoundException();
        }

        userTokenServiceUtils.validateRecordOwner(mailMock.getCreatedBy(), token);

        if (!mockedServerEngineService.getMailServerState().isRunning()) {
            throw new ValidationException("Mail mock server is not currently running");
        }

        mockedMailServerEngine.purgeAllMailServerInboxMessages(mailMock.getAddress());
    }

    public void saveMailMessageAttachment(final String mailMockMessageExtId,
                                          final String fileName,
                                          final String mimeType,
                                          final String content) {

        final MailMockMessage mailMockMessage = mailMockMessageDAO.findByExtId(mailMockMessageExtId);

        if (mailMockMessage == null) {
            throw new RecordNotFoundException();
        }

        final MailMockMessageAttachment attachment = new MailMockMessageAttachment();
        attachment.setName(fileName);
        attachment.setMimeType(mimeType);

        final MailMockMessageAttachmentContent attachmentContent = new MailMockMessageAttachmentContent();
        attachmentContent.setContent(GeneralUtils.base64Encode(content));
        attachmentContent.setMailMockMessageAttachment(attachment);

        attachment.setMailMockMessageAttachmentContent(attachmentContent);
        attachment.setMailMockMessage(mailMockMessage);

        mailMockMessage.getAttachments().add(attachment);

        mailMockMessageDAO.save(mailMockMessage);
    }

    public List<MailServerMessageInboxAttachmentLiteDTO> findAllMessageAttachments(final String mailMockExtId,
                                                                                   final String messageId,
                                                                                   final String token)
            throws ValidationException {

        final MailMock mailMock = mailMockDAO.findByExtId(mailMockExtId);

        if (mailMock == null) {
            throw new RecordNotFoundException();
        }

        userTokenServiceUtils.validateRecordOwner(mailMock.getCreatedBy(), token);


        if (mailMock.isSaveReceivedMail()) {

            final MailMockMessage mailMockMessage = mailMockMessageDAO.findByExtId(mailMock.getId(), messageId);

            if (mailMockMessage == null) {
                throw new RecordNotFoundException();
            }

            return mailMockMessage.getAttachments()
                    .stream()
                    .map(a ->
                            new MailServerMessageInboxAttachmentLiteDTO(Optional.of(a.getExtId()), a.getName(), a.getMimeType()))
                    .collect(Collectors.toList());

        } else if (mockedServerEngineService.getMailServerState().isRunning()) {

            // Find message and attachments from mail server...
            return mockedMailServerEngine.getMessageAttachmentsFromMailServerInbox(mailMock.getAddress(), messageId)
                    .stream()
                    .map(a ->
                            new MailServerMessageInboxAttachmentLiteDTO(a.getExtId(), a.getName(), a.getMimeType()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }

    public MailServerMessageInboxAttachmentDTO findMessageAttachment(final String mailMockExtId,
                                                                     final String messageId,
                                                                     final String attachmentIdOrName,
                                                                     final String token)
            throws RecordNotFoundException, ValidationException {

        final MailMock mailMock = mailMockDAO.findByExtId(mailMockExtId);

        if (mailMock == null) {
            throw new RecordNotFoundException();
        }

        userTokenServiceUtils.validateRecordOwner(mailMock.getCreatedBy(), token);


        if (mailMock.isSaveReceivedMail()) {

            final MailMockMessage mailMockMessage = mailMockMessageDAO.findByExtId(mailMock.getId(), messageId);

            if (mailMockMessage == null) {
                throw new RecordNotFoundException();
            }

            return mailMockMessage.getAttachments()
                    .stream()
                    .filter(a ->
                            StringUtils.equalsIgnoreCase(a.getExtId(), attachmentIdOrName))
                    .map(a ->
                            new MailServerMessageInboxAttachmentDTO(
                                    Optional.of(a.getExtId()),
                                    a.getName(),
                                    a.getMimeType(),
                                    a.getMailMockMessageAttachmentContent().getContent()))
                    .findFirst()
                    .orElseThrow(() ->
                            new RecordNotFoundException());

        } else if (mockedServerEngineService.getMailServerState().isRunning()) {

            // Load message attachments from mail server and then look up attachment by name...
            final Optional<MailServerMessageInboxAttachmentDTO> attachmentDTO
                    = mockedMailServerEngine.getMessageAttachmentsFromMailServerInbox(mailMock.getAddress(), messageId)
                        .stream()
                        .filter(a ->
                            StringUtils.equals(a.getName(), attachmentIdOrName))
                        .findFirst();

            if (!attachmentDTO.isPresent()) {
                new RecordNotFoundException();
            }

            return attachmentDTO.get();
        }

        return null;
    }

}
