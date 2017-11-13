package com.smockin.admin.controller;

import com.smockin.admin.dto.PathDTO;
import com.smockin.mockserver.service.dto.HttpProxiedDTO;
import com.smockin.mockserver.service.HttpProxyService;
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

    @RequestMapping(path="/proxy", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> create(@RequestBody final HttpProxiedDTO dto) {

        httpProxyService.addResponse(dto);

        return new ResponseEntity<String>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(path="/proxy/clear", method = RequestMethod.PATCH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> clearSession(@RequestBody final PathDTO dto) {

        httpProxyService.clearSession(dto.getPath());

        return new ResponseEntity<String>(HttpStatus.NO_CONTENT);
    }

}
