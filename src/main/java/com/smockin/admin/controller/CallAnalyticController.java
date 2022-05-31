package com.smockin.admin.controller;

import com.smockin.admin.dto.CallAnalyticDTO;
import com.smockin.admin.dto.response.CallAnalyticsResponseDTO;
import com.smockin.admin.dto.response.CallAnalyticsResponseLiteDTO;
import com.smockin.admin.dto.response.SimpleMessageResponseDTO;
import com.smockin.admin.exception.AuthException;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.service.CallAnalyticService;
import com.smockin.utils.GeneralUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class CallAnalyticController {

    private CallAnalyticService callAnalyticService;

    @Autowired
    public CallAnalyticController(final CallAnalyticService callAnalyticService) {
        this.callAnalyticService = callAnalyticService;
    }


    @RequestMapping(path="/call-analytic", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody
    ResponseEntity<List<CallAnalyticsResponseLiteDTO>> getAll(@RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException {

        return ResponseEntity.ok(callAnalyticService.getAll(GeneralUtils.extractOAuthToken(bearerToken)));
    }

    @RequestMapping(path="/call-analytic/{extId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody
    ResponseEntity<CallAnalyticsResponseDTO> get(@PathVariable("extId") final String extId,
                                                 @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException, ValidationException {

        return ResponseEntity.ok(callAnalyticService.getById(extId, GeneralUtils.extractOAuthToken(bearerToken)));
    }

    @RequestMapping(path="/call-analytic", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody
    ResponseEntity<SimpleMessageResponseDTO<String>> create(@RequestBody final CallAnalyticDTO dto,
                                                            @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws AuthException, ValidationException {

        return new ResponseEntity<>(
                new SimpleMessageResponseDTO(callAnalyticService.create(dto, GeneralUtils.extractOAuthToken(bearerToken))),
                HttpStatus.CREATED);
    }

    @RequestMapping(path="/call-analytic/{extId}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody
    ResponseEntity<?> update(@PathVariable("extId") final String extId,
                             @RequestBody final CallAnalyticDTO dto,
                             @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException, ValidationException {

        callAnalyticService.update(extId, dto, GeneralUtils.extractOAuthToken(bearerToken));

        return ResponseEntity.noContent().build();
    }

    @RequestMapping(path="/call-analytic/{extId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody
    ResponseEntity<?> delete(@PathVariable("extId") final String extId,
                             @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException, ValidationException {

        callAnalyticService.delete(extId, GeneralUtils.extractOAuthToken(bearerToken));

        return ResponseEntity.noContent().build();
    }

}
