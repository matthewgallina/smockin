package com.smockin.admin.controller;

import com.smockin.admin.dto.RestfulMockDTO;
import com.smockin.admin.dto.response.SimpleMessageResponseDTO;
import com.smockin.admin.dto.response.RestfulMockResponseDTO;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.service.RestfulMockService;
import com.smockin.utils.GeneralUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Created by mgallina.
 */
@Controller
public class RestfulMockController {

    @Autowired
    private RestfulMockService restfulMockService;

    @RequestMapping(path="/restmock/{extId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<RestfulMockResponseDTO> get(@PathVariable("extId") final String extId,
                                                                    @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException, ValidationException {
        return new ResponseEntity<>(restfulMockService.loadEndpoint(extId, GeneralUtils.extractOAuthToken(bearerToken)), HttpStatus.OK);
    }

    @RequestMapping(path="/restmock", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<SimpleMessageResponseDTO<String>> create(@RequestBody final RestfulMockDTO dto,
                                                                                 @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
                                                                                    throws RecordNotFoundException {
        return new ResponseEntity<>(new SimpleMessageResponseDTO<>(restfulMockService.createEndpoint(dto, GeneralUtils.extractOAuthToken(bearerToken))), HttpStatus.CREATED);
    }

    @RequestMapping(path = "/restmock/{extId}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<String> update(@PathVariable("extId") final String extId,
                                                       @RequestBody final RestfulMockDTO dto,
                                                       @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
                                                            throws RecordNotFoundException, ValidationException {
        restfulMockService.updateEndpoint(extId, dto, GeneralUtils.extractOAuthToken(bearerToken));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(path = "/restmock/{extId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<String> delete(@PathVariable("extId") final String extId,
                                                       @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
                                                            throws RecordNotFoundException, ValidationException {
        restfulMockService.deleteEndpoint(extId, GeneralUtils.extractOAuthToken(bearerToken));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(path="/restmock", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<List<RestfulMockResponseDTO>> getAll(@RequestParam(value = "filter", required = false) final String searchFilter,
                                                                          @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
                                                                                throws RecordNotFoundException {
        return new ResponseEntity<>(restfulMockService.loadAll(searchFilter, GeneralUtils.extractOAuthToken(bearerToken)), HttpStatus.OK);
    }

}

