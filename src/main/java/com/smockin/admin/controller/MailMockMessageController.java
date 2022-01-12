package com.smockin.admin.controller;

import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.service.MailMockService;
import com.smockin.mockserver.dto.MailMessageDTO;
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


    @RequestMapping(path="/mailmock/{extId}/messages", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody
    ResponseEntity<List<MailMessageDTO>> getAll(
                @PathVariable("extId") final String extId,
                @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException, ValidationException {

        return ResponseEntity.ok(mailMockService.loadAllInboxAddressMessages(extId, GeneralUtils.extractOAuthToken(bearerToken)));
    }

}
