package com.smockin.admin.controller;

import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.mockserver.service.dto.HttpProxiedDTO;
import com.smockin.mockserver.service.HttpProxyService;
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
public class HttpProxiedController {

    @Autowired
    private HttpProxyService httpProxyService;

    @RequestMapping(path="/proxy/{extId}", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> create(@PathVariable("extId") final String extId,
                                                  @RequestBody final HttpProxiedDTO dto,
                                                  @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
                                                        throws RecordNotFoundException, ValidationException {

        httpProxyService.addResponse(extId, dto, GeneralUtils.extractOAuthToken(bearerToken));

        return new ResponseEntity<String>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(path="/proxy/{extId}/clear", method = RequestMethod.PATCH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> clearSession(@PathVariable("extId") final String extId,
                                                        @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
                                                            throws RecordNotFoundException, ValidationException {

        httpProxyService.clearSession(extId, GeneralUtils.extractOAuthToken(bearerToken));

        return new ResponseEntity<String>(HttpStatus.NO_CONTENT);
    }

}
