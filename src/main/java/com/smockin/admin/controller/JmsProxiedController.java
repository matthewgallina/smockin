package com.smockin.admin.controller;

import com.smockin.admin.dto.QueueDTO;
import com.smockin.admin.exception.ValidationException;
import com.smockin.mockserver.service.JmsProxyService;
import com.smockin.mockserver.service.dto.JmsProxiedDTO;
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

    @RequestMapping(path="/jms/queue", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> postToQueue(@RequestBody final JmsProxiedDTO dto) throws ValidationException {

        jmsProxyService.pushToQueue(dto.getName(), dto.getBody(), dto.getMimeType(), 0);

        return new ResponseEntity<String>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(path="/jms/topic", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> postToTopic(@RequestBody final JmsProxiedDTO dto) throws ValidationException {

        jmsProxyService.pushToTopic(dto.getName(), dto.getBody(), dto.getMimeType());

        return new ResponseEntity<String>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(path="/jms/clear", method = RequestMethod.PATCH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> clearJms(@RequestBody final QueueDTO dto) throws ValidationException {

        jmsProxyService.clearQueue(dto.getName());

        return new ResponseEntity<String>(HttpStatus.NO_CONTENT);
    }

}
