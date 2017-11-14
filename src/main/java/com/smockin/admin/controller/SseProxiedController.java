package com.smockin.admin.controller;

import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.mockserver.service.ServerSideEventService;
import com.smockin.mockserver.service.dto.SseMessageDTO;
import com.smockin.mockserver.service.dto.PushClientDTO;
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
public class SseProxiedController {

    @Autowired
    private ServerSideEventService serverSideEventService;

    @RequestMapping(path="/sse/{id}/client", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<List<PushClientDTO>> getClients(@PathVariable("id") final String id) throws IOException, RecordNotFoundException {

        return new ResponseEntity<List<PushClientDTO>>(serverSideEventService.getClientConnections(id), HttpStatus.OK);
    }

    @RequestMapping(path="/sse", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> broadcast(@RequestBody final SseMessageDTO dto) {

        serverSideEventService.broadcastMessage(dto);

        return new ResponseEntity<String>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(path="/sse/{id}", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> send(@PathVariable("id") final String id, @RequestBody final SseMessageDTO dto) {

        serverSideEventService.addMessage(id, dto);

        return new ResponseEntity<String>(HttpStatus.NO_CONTENT);
    }

    /*
    @RequestMapping(path="/sse/clear", method = RequestMethod.PATCH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> clear(@RequestBody final PathDTO dto) {

        serverSideEventService.clear(dto.getPath());

        return new ResponseEntity<String>(HttpStatus.NO_CONTENT);
    }
    */

}
