package com.smockin.admin.controller;

import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.mockserver.service.ProxyService;
import com.smockin.mockserver.service.WebSocketService;
import com.smockin.mockserver.service.dto.ProxiedDTO;
import com.smockin.mockserver.service.dto.WebSocketClientDTO;
import com.smockin.mockserver.service.dto.WebSocketDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

/**
 * Created by mgallina.
 */
@Controller
public class WebSocketController {

    @Autowired
    private WebSocketService webSocketService;

    @RequestMapping(path="/ws/{wsPath}/client", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<List<WebSocketClientDTO>> getClients(@PathVariable("wsPath") final String wsPath) throws IOException {

        return new ResponseEntity<List<WebSocketClientDTO>>(webSocketService.getClientConnections(wsPath), HttpStatus.OK);
    }

    @RequestMapping(path="/ws", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> create(@RequestBody final WebSocketDTO dto) throws IOException {

        webSocketService.pushMessage(dto);

        return new ResponseEntity<String>(HttpStatus.NO_CONTENT);
    }

}
