package com.smockin.admin.controller;

import com.smockin.admin.dto.ProxyRestDuplicatePriorityDTO;
import com.smockin.admin.dto.response.ProxyRestDuplicateDTO;
import com.smockin.admin.exception.AuthException;
import com.smockin.admin.exception.RecordNotFoundException;
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
public class ProxyRestDuplicateController {

    @Autowired
    private RestfulMockService restfulMockService;

    @RequestMapping(path="/proxyconfig/duplicate", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<Void> postProxyDuplicateConfig(@RequestBody final ProxyRestDuplicatePriorityDTO priorityMocksDTO,
                                                                       @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
                                                                            throws RecordNotFoundException, AuthException {
        restfulMockService.saveUserPathDuplicatePriorities(priorityMocksDTO, GeneralUtils.extractOAuthToken(bearerToken));
        return ResponseEntity.noContent().build();
    }

    @RequestMapping(path="/proxyconfig/duplicate", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<List<ProxyRestDuplicateDTO>> getProxyDuplicates(@RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
                                                                                            throws RecordNotFoundException, AuthException {
        return new ResponseEntity<>(restfulMockService.loadAllUserPathDuplicates(GeneralUtils.extractOAuthToken(bearerToken)), HttpStatus.OK);
    }

}

