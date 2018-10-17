package com.smockin.admin.controller;

import com.smockin.admin.exception.AuthException;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.enums.ServerTypeEnum;
import com.smockin.admin.service.MockedServerEngineService;
import com.smockin.mockserver.dto.MockServerState;
import com.smockin.mockserver.dto.MockedServerConfigDTO;
import com.smockin.mockserver.exception.MockServerException;
import com.smockin.utils.GeneralUtils;
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

    //
    // REST Server
    @RequestMapping(path="/mockedserver/rest/start", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> startRest(@RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken) throws MockServerException, RecordNotFoundException, AuthException {
        return new ResponseEntity<>(mockedServerEngineService.startRest(GeneralUtils.extractOAuthToken(bearerToken)), HttpStatus.OK);
    }

    @RequestMapping(path="/mockedserver/rest/stop", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> stopRest(@RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken) throws MockServerException, RecordNotFoundException, AuthException {
        mockedServerEngineService.shutdownRest(GeneralUtils.extractOAuthToken(bearerToken));
        return new ResponseEntity<String>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(path="/mockedserver/rest/restart", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> restartRest(@RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken) throws MockServerException, RecordNotFoundException, AuthException {
        return new ResponseEntity<>(mockedServerEngineService.restartRest(GeneralUtils.extractOAuthToken(bearerToken)), HttpStatus.OK);
    }

    @RequestMapping(path="/mockedserver/rest/status", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<MockServerState> restStatus() throws MockServerException {
        return new ResponseEntity<>(mockedServerEngineService.getRestServerState(), HttpStatus.OK);
    }

    //
    // JMS Server
    @RequestMapping(path="/mockedserver/jms/start", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> startJms(@RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken) throws MockServerException, RecordNotFoundException, AuthException {
        return new ResponseEntity<>(mockedServerEngineService.startJms(GeneralUtils.extractOAuthToken(bearerToken)), HttpStatus.OK);
    }

    @RequestMapping(path="/mockedserver/jms/stop", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> stopJms(@RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken) throws MockServerException, RecordNotFoundException, AuthException {
        mockedServerEngineService.shutdownJms(GeneralUtils.extractOAuthToken(bearerToken));
        return new ResponseEntity<String>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(path="/mockedserver/jms/restart", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> restartJms(@RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken) throws MockServerException, RecordNotFoundException, AuthException {
        return new ResponseEntity<>(mockedServerEngineService.restartJms(GeneralUtils.extractOAuthToken(bearerToken)), HttpStatus.OK);
    }

    @RequestMapping(path="/mockedserver/jms/status", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<MockServerState> jmsStatus() throws MockServerException {
        return new ResponseEntity<>(mockedServerEngineService.getJmsServerState(), HttpStatus.OK);
    }


    //
    // FTP Server
    @RequestMapping(path="/mockedserver/ftp/start", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> startFtp(@RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken) throws MockServerException, RecordNotFoundException, AuthException {
        return new ResponseEntity<>(mockedServerEngineService.startFtp(GeneralUtils.extractOAuthToken(bearerToken)), HttpStatus.OK);
    }

    @RequestMapping(path="/mockedserver/ftp/stop", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> stopFtp(@RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken) throws MockServerException, RecordNotFoundException, AuthException {
        mockedServerEngineService.shutdownFtp(GeneralUtils.extractOAuthToken(bearerToken));
        return new ResponseEntity<String>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(path="/mockedserver/ftp/restart", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> restartFtp(@RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken) throws MockServerException, RecordNotFoundException, AuthException {
        return new ResponseEntity<>(mockedServerEngineService.restartFtp(GeneralUtils.extractOAuthToken(bearerToken)), HttpStatus.OK);
    }

    @RequestMapping(path="/mockedserver/ftp/status", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<MockServerState> ftpStatus() throws MockServerException {
        return new ResponseEntity<>(mockedServerEngineService.getFtpServerState(), HttpStatus.OK);
    }


    //
    // Server Config
    @RequestMapping(path="/mockedserver/config/{serverType}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> getServerConfig(@PathVariable("serverType") final String serverType) throws ValidationException, RecordNotFoundException {
        final ServerTypeEnum type = convertServerType(serverType);
        return new ResponseEntity<>(mockedServerEngineService.loadServerConfig(type), HttpStatus.OK);
    }

    @RequestMapping(path="/mockedserver/config/{serverType}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<String> putServerConfig(@PathVariable("serverType") final String serverType,
                                                           @RequestBody final MockedServerConfigDTO dto,
                                                           @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
                                                                throws RecordNotFoundException, AuthException, ValidationException {
        mockedServerEngineService.saveServerConfig(convertServerType(serverType), dto, GeneralUtils.extractOAuthToken(bearerToken));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    ServerTypeEnum convertServerType(final String serverType) throws ValidationException {

        try {
            return ServerTypeEnum.valueOf(serverType);
        } catch (Throwable ex) {
            throw new ValidationException("Invalid serverType value: " + serverType);
        }

    }

}
