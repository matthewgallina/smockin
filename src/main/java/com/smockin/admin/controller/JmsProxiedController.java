package com.smockin.admin.controller;

import com.smockin.admin.dto.QueueDTO;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.mockserver.service.JmsProxyService;
import com.smockin.mockserver.service.dto.JmsProxiedDTO;
import com.smockin.utils.GeneralUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * Created by mgallina.
 */
@Controller
public class JmsProxiedController {

    @Autowired
    private JmsProxyService jmsProxyService;

    @RequestMapping(path="/jmsmock/{extId}/queue", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> postToQueue(@PathVariable("extId") final String extId,
                                                       @RequestBody final JmsProxiedDTO dto,
                                                       @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
                                                            throws RecordNotFoundException, ValidationException {

        jmsProxyService.pushToQueue(extId, dto.getBody(), dto.getMimeType(), 0, GeneralUtils.extractOAuthToken(bearerToken));

        return new ResponseEntity<String>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(path="/jmsmock/{extId}/topic", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> postToTopic(@PathVariable("extId") final String extId,
                                                       @RequestBody final JmsProxiedDTO dto,
                                                       @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
                                                            throws RecordNotFoundException, ValidationException {

        jmsProxyService.pushToTopic(extId, dto.getBody(), dto.getMimeType(), GeneralUtils.extractOAuthToken(bearerToken));

        return new ResponseEntity<String>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(path="/jmsmock/{extId}/queue/clear", method = RequestMethod.PATCH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> clearJmsQueue(@PathVariable("extId") final String extId,
                                                         @RequestBody final QueueDTO dto,
                                                         @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
                                                            throws RecordNotFoundException, ValidationException {

        jmsProxyService.clearQueue(extId, GeneralUtils.extractOAuthToken(bearerToken));

        return new ResponseEntity<String>(HttpStatus.NO_CONTENT);
    }

}
