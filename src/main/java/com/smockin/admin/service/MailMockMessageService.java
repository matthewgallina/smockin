package com.smockin.admin.service;

import com.smockin.admin.exception.ValidationException;
import com.smockin.mockserver.dto.MailServerMessageInboxAttachmentDTO;
import com.smockin.mockserver.dto.MailServerMessageInboxAttachmentLiteDTO;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface MailMockMessageService {

    String saveMailMessage(final String mailMockExtId,
                     final String sender,
                     final String subject,
                     final String body,
                     final Date dateReceived,
                     final Optional<String> token) throws ValidationException;

    void deleteMailMessage(final String mailExtId, final String mailMessageId, final String token) throws ValidationException;

    void deleteAllMailMessages(final String mailExtId, final String token) throws ValidationException;

    void deleteAllMailMessagesOnServer(final String mailExtId, final String token) throws ValidationException;

    void saveMailMessageAttachment(final String mailMockMessageExtId,
                                   final String fileName,
                                   final String mimeType,
                                   final String base64Content);

    List<MailServerMessageInboxAttachmentLiteDTO> findAllMessageAttachments(
            final String mailMockExtId,
            final String messageId,
            final String token) throws ValidationException;

    MailServerMessageInboxAttachmentDTO findMessageAttachment(
            final String mailMockExtId,
            final String messageId,
            final String attachmentIdOrName,
            final String token) throws ValidationException;

}
