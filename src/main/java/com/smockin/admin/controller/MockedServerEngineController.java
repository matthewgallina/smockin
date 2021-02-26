package com.smockin.admin.controller;

import com.smockin.admin.dto.LiveLoggingBlockingEndpointDTO;
import com.smockin.admin.exception.AuthException;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.enums.ServerTypeEnum;
import com.smockin.admin.service.MockedServerEngineService;
import com.smockin.mockserver.dto.MockServerState;
import com.smockin.mockserver.dto.MockedServerConfigDTO;
import com.smockin.mockserver.dto.ProxyForwardConfigDTO;
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
    public @ResponseBody ResponseEntity<MockedServerConfigDTO> startRest(@RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken) throws MockServerException, RecordNotFoundException, AuthException {
        return new ResponseEntity<>(mockedServerEngineService.startRest(GeneralUtils.extractOAuthToken(bearerToken)), HttpStatus.OK);
    }

    @RequestMapping(path="/mockedserver/rest/stop", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> stopRest(@RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken) throws MockServerException, RecordNotFoundException, AuthException {
        mockedServerEngineService.shutdownRest(GeneralUtils.extractOAuthToken(bearerToken));
        return new ResponseEntity<String>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(path="/mockedserver/rest/restart", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<MockedServerConfigDTO> restartRest(@RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken) throws MockServerException, RecordNotFoundException, AuthException {
        return new ResponseEntity<>(mockedServerEngineService.restartRest(GeneralUtils.extractOAuthToken(bearerToken)), HttpStatus.OK);
    }

    @RequestMapping(path="/mockedserver/rest/status", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<MockServerState> restStatus() throws MockServerException {
        return new ResponseEntity<>(mockedServerEngineService.getRestServerState(), HttpStatus.OK);
    }


    //
    // Server Config
    @RequestMapping(path="/mockedserver/config/{serverType}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<MockedServerConfigDTO> getServerConfig(@PathVariable("serverType") final String serverType) throws ValidationException, RecordNotFoundException {
        final ServerTypeEnum type = convertServerType(serverType);
        return new ResponseEntity<>(mockedServerEngineService.loadServerConfig(type), HttpStatus.OK);
    }

    @RequestMapping(path="/mockedserver/config/{serverType}", method = RequestMethod.PUT,
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> putServerConfig(@PathVariable("serverType") final String serverType,
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


    //
    // Proxy Forward Mappings
    @RequestMapping(
            path="/mockedserver/config/{serverType}/proxy",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<ProxyForwardConfigDTO> getServerConfigProxyConfig(@PathVariable("serverType") final String serverType)
            throws ValidationException, RecordNotFoundException {

        final ServerTypeEnum type = convertServerType(serverType);

        return new ResponseEntity<>(mockedServerEngineService.loadProxyForwardConfig(type), HttpStatus.OK);
    }

    @RequestMapping(
            path="/mockedserver/config/{serverType}/proxy",
            method = RequestMethod.PUT,
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> putServerConfigProxyConfig(@PathVariable("serverType") final String serverType,
                                                                      @RequestBody final ProxyForwardConfigDTO proxyForwardConfig,
                                                                      @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException, AuthException, ValidationException {

        final ServerTypeEnum type = convertServerType(serverType);

        mockedServerEngineService.saveProxyForwardMappings(type, proxyForwardConfig, GeneralUtils.extractOAuthToken(bearerToken));

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(
            path="/mockedserver/config/{serverType}/live-logging-block/endpoint",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> addLiveLoggingPathToBlock(@PathVariable("serverType") final String serverType,
                                                                     @RequestBody final LiveLoggingBlockingEndpointDTO liveLoggingBlockingEndpoint,
                                                                     @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws AuthException {

        mockedServerEngineService.addLiveLoggingPathToBlock(
                liveLoggingBlockingEndpoint,
                GeneralUtils.extractOAuthToken(bearerToken));

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(
            path="/mockedserver/config/{serverType}/live-logging-block/endpoint",
            method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> removeLiveLoggingPathToBlock(@PathVariable("serverType") final String serverType,
                                                                        @RequestParam("method") final String method,
                                                                        @RequestParam("path") final String path,
                                                                        @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws AuthException {

        mockedServerEngineService.removeLiveLoggingPathToBlock(
                method,
                path,
                GeneralUtils.extractOAuthToken(bearerToken));

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
