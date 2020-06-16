package com.smockin.admin.controller;

import com.smockin.admin.dto.HttpClientCallDTO;
import com.smockin.admin.dto.response.HttpClientResponseDTO;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.service.HttpClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Created by mgallina.
 */
@Controller
public class HttpClientController {

    @Autowired
    private HttpClientService httpClientService;

    @RequestMapping(path="/httpclientcall", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<HttpClientResponseDTO> httpClientCall(@RequestBody final HttpClientCallDTO httpClientCallDTO) throws ValidationException {
        return new ResponseEntity<>(httpClientService.handleCallToMock(httpClientCallDTO), HttpStatus.OK);
    }

}
