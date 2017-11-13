package com.smockin.admin.controller;

import com.smockin.admin.dto.PathDTO;
import com.smockin.mockserver.service.HttpProxyService;
import com.smockin.mockserver.service.ServerSideEventService;
import com.smockin.mockserver.service.dto.HttpProxiedDTO;
import com.smockin.mockserver.service.dto.SseMessageDTO;
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
public class SseProxiedController {

    @Autowired
    private ServerSideEventService serverSideEventService;

    @RequestMapping(path="/sse", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> create(@RequestBody final SseMessageDTO dto) {

        serverSideEventService.addMessage(dto);

        return new ResponseEntity<String>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(path="/sse/clear", method = RequestMethod.PATCH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> clear(@RequestBody final PathDTO dto) {

        serverSideEventService.clear(dto.getPath());

        return new ResponseEntity<String>(HttpStatus.NO_CONTENT);
    }

}
