package com.smockin.admin.controller;

import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.mockserver.exception.MockServerException;
import com.smockin.mockserver.service.WebSocketService;
import com.smockin.mockserver.service.dto.PushClientDTO;
import com.smockin.mockserver.service.dto.WebSocketDTO;
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
public class WebSocketController {

    @Autowired
    private WebSocketService webSocketService;

    @RequestMapping(path="/ws/{id}/client", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<List<PushClientDTO>> getClients(@PathVariable("id") final String id,
                                                                        @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
                                                                            throws RecordNotFoundException, ValidationException {

        return new ResponseEntity<>(webSocketService.getClientConnections(id, GeneralUtils.extractOAuthToken(bearerToken)), HttpStatus.OK);
    }

    @RequestMapping(path="/ws/{id}", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> sendMessage(@PathVariable("id") final String id,
                                                       @RequestBody final WebSocketDTO dto)
                                                            throws MockServerException {

        webSocketService.sendMessage(id, dto);

        return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
    }

}
