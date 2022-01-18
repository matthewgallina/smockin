package com.smockin.admin.controller;

import com.smockin.admin.dto.MailMockDTO;
import com.smockin.admin.dto.response.MailMockResponseDTO;
import com.smockin.admin.dto.response.MailMockResponseLiteDTO;
import com.smockin.admin.dto.response.SimpleMessageResponseDTO;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.service.MailMockService;
import com.smockin.utils.GeneralUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Controller
public class MailMockController {

    @Autowired
    private MailMockService mailMockService;


    @RequestMapping(path="/mailmock", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody
    ResponseEntity<List<MailMockResponseLiteDTO>> getAll(@RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException {

        return ResponseEntity.ok(mailMockService.loadAll(GeneralUtils.extractOAuthToken(bearerToken)));
    }

    @RequestMapping(path="/mailmock/{extId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody
    ResponseEntity<MailMockResponseDTO> get(@PathVariable("extId") final String extId,
                                            @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException {

        return ResponseEntity.ok(mailMockService.loadById(extId, GeneralUtils.extractOAuthToken(bearerToken)));
    }

    @RequestMapping(path="/mailmock", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody
    ResponseEntity<SimpleMessageResponseDTO<String>> create(@RequestBody final MailMockDTO dto,
                                                            @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException, ValidationException {

        return new ResponseEntity<>(new SimpleMessageResponseDTO(mailMockService.create(dto, GeneralUtils.extractOAuthToken(bearerToken))), HttpStatus.CREATED);
    }

    @RequestMapping(path="/mailmock/{extId}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody
    ResponseEntity<?> update(@PathVariable("extId") final String extId,
                             @RequestBody final MailMockDTO dto,
                             @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException, ValidationException {

        mailMockService.update(extId, dto, GeneralUtils.extractOAuthToken(bearerToken));

        return ResponseEntity.noContent().build();
    }

    @RequestMapping(path="/mailmock/{extId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody
    ResponseEntity<?> delete(@PathVariable("extId") final String extId,
                             @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException {

        mailMockService.delete(extId, GeneralUtils.extractOAuthToken(bearerToken));

        return ResponseEntity.noContent().build();
    }

}
