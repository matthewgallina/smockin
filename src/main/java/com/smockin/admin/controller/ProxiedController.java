package com.smockin.admin.controller;

import com.smockin.mockserver.service.dto.ProxiedDTO;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.mockserver.service.ProxyService;
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
public class ProxiedController {

    @Autowired
    private ProxyService proxyService;

    @RequestMapping(path="/proxy", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> create(@RequestBody final ProxiedDTO dto) {

        proxyService.addResponse(dto);

        return new ResponseEntity<String>(HttpStatus.NO_CONTENT);
    }

}
