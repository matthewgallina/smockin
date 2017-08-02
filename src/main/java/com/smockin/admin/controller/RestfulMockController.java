package com.smockin.admin.controller;

import com.smockin.admin.dto.RestfulMockDTO;
import com.smockin.admin.dto.response.SimpleMessageResponseDTO;
import com.smockin.admin.dto.response.RestfulMockResponseDTO;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.service.RestfulMockService;
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

    @RequestMapping(path="/restmock", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<SimpleMessageResponseDTO<String>> create(@RequestBody final RestfulMockDTO dto) {
            return new ResponseEntity<SimpleMessageResponseDTO<String>>(new SimpleMessageResponseDTO<String>(restfulMockService.createEndpoint(dto)), HttpStatus.CREATED);
    }

    @RequestMapping(path = "/restmock/{extId}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<String> update(@PathVariable("extId") final String extId, @RequestBody final RestfulMockDTO dto) throws RecordNotFoundException {
        restfulMockService.updateEndpoint(extId, dto);
        return new ResponseEntity<String>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(path = "/restmock/{extId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<String> delete(@PathVariable("extId") final String extId) throws RecordNotFoundException {
        restfulMockService.deleteEndpoint(extId);
        return new ResponseEntity<String>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(path="/restmock", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<List<RestfulMockResponseDTO>> get() {
        return new ResponseEntity<List<RestfulMockResponseDTO>>(restfulMockService.loadAll(), HttpStatus.OK);
    }

}
