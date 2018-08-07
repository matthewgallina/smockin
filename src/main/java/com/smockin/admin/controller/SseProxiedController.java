package com.smockin.admin.controller;

import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.mockserver.service.ServerSideEventService;
import com.smockin.mockserver.service.dto.SseMessageDTO;
import com.smockin.mockserver.service.dto.PushClientDTO;
import com.smockin.utils.GeneralUtils;
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
public class SseProxiedController {

    @Autowired
    private ServerSideEventService serverSideEventService;

    @RequestMapping(path="/sse/{id}/client", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<List<PushClientDTO>> getClients(@PathVariable("id") final String id,
                                                                        @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
                                                                            throws RecordNotFoundException, ValidationException {

        return new ResponseEntity<>(serverSideEventService.getClientConnections(id, GeneralUtils.extractOAuthToken(bearerToken)), HttpStatus.OK);
    }

    @RequestMapping(path="/sse/{id}", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> send(@PathVariable("id") final String id,
                                                @RequestBody final SseMessageDTO dto) {

        serverSideEventService.addMessage(id, dto);

        return new ResponseEntity<String>(HttpStatus.NO_CONTENT);
    }

}
