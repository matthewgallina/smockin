package com.smockin.admin.controller;

import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.enums.ServerTypeEnum;
import com.smockin.admin.service.MockedServerEngineService;
import com.smockin.mockserver.dto.MockServerState;
import com.smockin.mockserver.dto.MockedServerConfigDTO;
import com.smockin.mockserver.exception.MockServerException;
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
public class MockedServerEngineController {

    @Autowired
    private MockedServerEngineService mockedServerEngineService;

    @RequestMapping(path="/mockedserver/rest/start", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> startRest() throws ValidationException, MockServerException {
        return new ResponseEntity<MockedServerConfigDTO>(mockedServerEngineService.startRest(), HttpStatus.OK);
    }

    @RequestMapping(path="/mockedserver/rest/stop", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> stopRest() throws MockServerException {
        mockedServerEngineService.shutdownRest();
        return new ResponseEntity<String>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(path="/mockedserver/rest/restart", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> restartRest() throws MockServerException {
        return new ResponseEntity<MockedServerConfigDTO>(mockedServerEngineService.restartRest(), HttpStatus.OK);
    }

    @RequestMapping(path="/mockedserver/rest/status", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<MockServerState> restStatus() throws MockServerException {
        return new ResponseEntity<MockServerState>(mockedServerEngineService.getRestServerState(), HttpStatus.OK);
    }

    @RequestMapping(path="/mockedserver/config/{serverType}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> getServerConfig(@PathVariable("serverType") final String serverType) throws ValidationException, RecordNotFoundException {
        final ServerTypeEnum type = convertServerType(serverType);
        return new ResponseEntity<MockedServerConfigDTO>(mockedServerEngineService.loadServerConfig(type), HttpStatus.OK);
    }

    @RequestMapping(path="/mockedserver/config/{serverType}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> putServerConfig(@PathVariable("serverType") final String serverType, @RequestBody final MockedServerConfigDTO dto) throws ValidationException {
        mockedServerEngineService.saveServerConfig(convertServerType(serverType), dto);
        return new ResponseEntity<String>(HttpStatus.NO_CONTENT);
    }

    ServerTypeEnum convertServerType(final String serverType) throws ValidationException {

        try {
            return ServerTypeEnum.valueOf(serverType);
        } catch (Throwable ex) {
            throw new ValidationException("Invalid serverType value: " + serverType);
        }

    }

}
