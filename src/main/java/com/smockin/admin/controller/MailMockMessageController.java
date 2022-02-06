package com.smockin.admin.controller;

import com.smockin.admin.dto.response.SimpleMessageResponseDTO;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.service.MailMockMessageService;
import com.smockin.admin.service.MailMockService;
import com.smockin.mockserver.dto.MailServerMessageInboxAttachmentDTO;
import com.smockin.mockserver.dto.MailServerMessageInboxAttachmentLiteDTO;
import com.smockin.mockserver.dto.MailServerMessageInboxDTO;
import com.smockin.utils.GeneralUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
public class MailMockMessageController {

    @Autowired
    private MailMockService mailMockService;

    @Autowired
    private MailMockMessageService mailMockMessageService;


    @RequestMapping(
            path="/mailmock/{mailExtId}/inbox",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody
    ResponseEntity<List<MailServerMessageInboxDTO>> getInboxMessages(
                @PathVariable("mailExtId") final String mailExtId,
                @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
                    throws RecordNotFoundException, ValidationException {

        return ResponseEntity.ok(mailMockService.loadMessagesFromMailServerInbox(mailExtId, GeneralUtils.extractOAuthToken(bearerToken)));
    }

    @RequestMapping(
            path="/mailmock/{mailExtId}/inbox",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<SimpleMessageResponseDTO> createInboxMessage(
                @PathVariable("mailExtId") final String mailExtId,
                @RequestBody final MailServerMessageInboxDTO dto,
                @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
                    throws RecordNotFoundException, ValidationException {

        final String extId = mailMockMessageService.saveMailMessage(mailExtId,
                dto.getFrom(),
                dto.getSubject(),
                dto.getBody(),
                dto.getDateReceived(),
                Optional.of(GeneralUtils.extractOAuthToken(bearerToken)));

        return new ResponseEntity(new SimpleMessageResponseDTO(extId), HttpStatus.CREATED);
    }

    @RequestMapping(
            path="/mailmock/{mailExtId}/inbox/{messageExtId}",
            method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> deleteInboxMessage(
                @PathVariable("mailExtId") final String mailExtId,
                @PathVariable("messageExtId") final String messageExtId,
                @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
                    throws RecordNotFoundException, ValidationException {

        mailMockMessageService.deleteMailMessage(messageExtId, GeneralUtils.extractOAuthToken(bearerToken));

        return ResponseEntity.noContent().build();
    }

    @RequestMapping(
            path="/mailmock/{mailExtId}/inbox",
            method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> deleteAllInboxMessages(
            @PathVariable("mailExtId") final String mailExtId,
            @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException, ValidationException {

        mailMockMessageService.deleteAllMailMessages(mailExtId, GeneralUtils.extractOAuthToken(bearerToken));

        return ResponseEntity.noContent().build();
    }

    @RequestMapping(
            path="/mailmock/{mailExtId}/server/inbox",
            method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> deleteAllInboxMessagesOnServer(
            @PathVariable("mailExtId") final String mailExtId,
            @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException, ValidationException {

        mailMockMessageService.deleteAllMailMessagesOnServer(mailExtId, GeneralUtils.extractOAuthToken(bearerToken));

        return ResponseEntity.noContent().build();
    }

    @RequestMapping(
            path="/mailmock/{mailExtId}/message/{messageId}/attachments",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<List<MailServerMessageInboxAttachmentLiteDTO>> getAllAttachments(
                @PathVariable("mailExtId") final String mailExtId,
                @PathVariable("messageId") final String messageId,
                @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
                    throws RecordNotFoundException, ValidationException {

        return ResponseEntity.ok(mailMockMessageService
                .findAllMessageAttachments(mailExtId, messageId, GeneralUtils.extractOAuthToken(bearerToken)));
    }

    @RequestMapping(
            path="/mailmock/{mailExtId}/message/{messageId}/attachment/{attachmentIdOrName}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<MailServerMessageInboxAttachmentDTO> getAttachmentByIdOrName(
            @PathVariable("mailExtId") final String mailExtId,
            @PathVariable("messageId") final String messageId,
            @PathVariable("attachmentIdOrName") final String attachmentIdOrName,
            @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException, ValidationException {

        return ResponseEntity.ok(mailMockMessageService
                .findMessageAttachment(
                        mailExtId,
                        messageId,
                        attachmentIdOrName,
                        GeneralUtils.extractOAuthToken(bearerToken)));
    }

}
