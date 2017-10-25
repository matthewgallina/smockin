package com.smockin.admin.controller;

import com.smockin.admin.dto.JmsMockDTO;
import com.smockin.admin.dto.response.JmsMockResponseDTO;
import com.smockin.admin.dto.response.SimpleMessageResponseDTO;
import com.smockin.admin.exception.RecordNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mgallina.
 */
@Controller
public class JmsMockController {

    @RequestMapping(path="/jmsmock", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<SimpleMessageResponseDTO<String>> create(@RequestBody final JmsMockDTO dto) {
            return new ResponseEntity<SimpleMessageResponseDTO<String>>(new SimpleMessageResponseDTO<String>(null), HttpStatus.CREATED);
    }

    @RequestMapping(path = "/jmsmock/{extId}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<String> update(@PathVariable("extId") final String extId, @RequestBody final JmsMockDTO dto) throws RecordNotFoundException {
        return new ResponseEntity<String>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(path = "/jmsmock/{extId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<String> delete(@PathVariable("extId") final String extId) throws RecordNotFoundException {
        return new ResponseEntity<String>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(path="/jmsmock", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<List<JmsMockResponseDTO>> get() {
        return new ResponseEntity<List<JmsMockResponseDTO>>(new ArrayList<JmsMockResponseDTO>(), HttpStatus.OK);
    }

}
