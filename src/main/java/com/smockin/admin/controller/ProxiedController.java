package com.smockin.admin.controller;

import com.smockin.admin.dto.ProxiedDTO;
import com.smockin.admin.dto.RestfulMockDTO;
import com.smockin.admin.dto.response.RestfulMockResponseDTO;
import com.smockin.admin.dto.response.SimpleMessageResponseDTO;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.service.RestfulMockService;
import com.smockin.mockserver.service.ProxyService;
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
public class ProxiedController {

    @Autowired
    private ProxyService proxyService;

    @RequestMapping(path="/proxy", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> create(@RequestBody final ProxiedDTO dto) throws RecordNotFoundException {

        proxyService.addResponse(dto);

        return new ResponseEntity<String>(HttpStatus.NO_CONTENT);
    }

}
