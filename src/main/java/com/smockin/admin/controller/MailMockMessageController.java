package com.smockin.admin.controller;

import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.service.MailMockService;
import com.smockin.mockserver.dto.MailServerMessageInboxDTO;
import com.smockin.utils.GeneralUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class MailMockMessageController {

    @Autowired
    private MailMockService mailMockService;


    @RequestMapping(path="/mailmock/{extId}/inbox", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody
    ResponseEntity<List<MailServerMessageInboxDTO>> getInboxMessages(
                @PathVariable("extId") final String extId,
                @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException, ValidationException {

        return ResponseEntity.ok(mailMockService.loadMessagesFromMailServerInbox(extId, GeneralUtils.extractOAuthToken(bearerToken)));
    }

    @RequestMapping(path="/mailmock/{mailExtId}/message/{messageExtId}/attachments", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody
    ResponseEntity<List<MailServerMessageInboxDTO>> getAllAttachments(
            @PathVariable("mailExtId") final String mailExtId,
            @PathVariable("messageExtId") final String messageExtId,
            @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException {

        return ResponseEntity.notFound().build();
    }

}
