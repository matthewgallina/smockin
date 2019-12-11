package com.smockin.admin.controller;

import com.smockin.admin.dto.JmsMockDTO;
import com.smockin.admin.dto.response.JmsMockResponseDTO;
import com.smockin.admin.dto.response.SimpleMessageResponseDTO;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.service.JmsMockService;
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
public class JmsMockController {

    @Autowired
    private JmsMockService jmsMockService;

    @RequestMapping(path="/jmsmock", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<SimpleMessageResponseDTO<String>> create(@RequestBody final JmsMockDTO dto,
                                                                                 @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
                                                                                    throws RecordNotFoundException {
            return new ResponseEntity<>(new SimpleMessageResponseDTO<>(jmsMockService.createEndpoint(dto, GeneralUtils.extractOAuthToken(bearerToken))), HttpStatus.CREATED);
    }

    @RequestMapping(path = "/jmsmock/{extId}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<String> update(@PathVariable("extId") final String extId,
                                                       @RequestBody final JmsMockDTO dto,
                                                       @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
                                                            throws RecordNotFoundException, ValidationException {
        jmsMockService.updateEndpoint(extId, dto, GeneralUtils.extractOAuthToken(bearerToken));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(path = "/jmsmock/{extId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<String> delete(@PathVariable("extId") final String extId,
                                                       @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
                                                            throws RecordNotFoundException, ValidationException {
        jmsMockService.deleteEndpoint(extId, GeneralUtils.extractOAuthToken(bearerToken));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(path="/jmsmock", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<List<JmsMockResponseDTO>> get(@RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken) throws RecordNotFoundException {
        return new ResponseEntity<>(jmsMockService.loadAll(GeneralUtils.extractOAuthToken(bearerToken)), HttpStatus.OK);
    }

}
