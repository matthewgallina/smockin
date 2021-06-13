package com.smockin.admin.controller;

import com.smockin.admin.dto.LiveLoggingBlockingEndpointDTO;
import com.smockin.admin.exception.AuthException;
import com.smockin.admin.exception.MockImportException;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.admin.persistence.enums.ServerTypeEnum;
import com.smockin.admin.service.MockedServerEngineService;
import com.smockin.mockserver.dto.MockServerState;
import com.smockin.mockserver.dto.MockedServerConfigDTO;
import com.smockin.mockserver.dto.ProxyForwardConfigDTO;
import com.smockin.mockserver.dto.ProxyForwardConfigResponseDTO;
import com.smockin.mockserver.exception.MockServerException;
import com.smockin.utils.GeneralUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

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
    // Toggle Proxy Forward
    @RequestMapping(
            path="/mockedserver/config/{serverType}/proxy/mode",
            method = RequestMethod.PUT,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> putServerProxyMode(
                                                @PathVariable("serverType") final String serverType,
                                                @RequestParam("enableProxyMode") final boolean enableProxyMode,
                                                @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
                                                    throws AuthException {

        mockedServerEngineService.updateProxyMode(enableProxyMode, GeneralUtils.extractOAuthToken(bearerToken));

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }


    //
    // Proxy Forward Export / Import
    @RequestMapping(path="/mockedserver/config/{serverType}/proxy/mappings/export", method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
    public @ResponseBody ResponseEntity<String> getServerConfigProxyMappingsExport(@PathVariable("serverType") final String serverType,
                                                                                   @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws AuthException {

        final Optional<String> exportOpt = mockedServerEngineService.exportProxyMappings(GeneralUtils.extractOAuthToken(bearerToken));

        if (!exportOpt.isPresent()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok()
                .body(exportOpt.get());

    }

    @RequestMapping(path="/mockedserver/config/{serverType}/proxy/mappings/import", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public @ResponseBody ResponseEntity<String> postServerConfigProxyMappingsImport(@PathVariable("serverType") final String serverType,
                                                                                    @RequestHeader(value = GeneralUtils.KEEP_EXISTING_HEADER_NAME) final boolean keepExisting,
                                                                                    @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken,
                                                                                    @RequestParam("file") final MultipartFile file)
            throws MockImportException, ValidationException {

            mockedServerEngineService.importProxyMappingsFile(file, keepExisting, GeneralUtils.extractOAuthToken(bearerToken));

            return ResponseEntity.noContent().build();
    }


    //
    // Proxy Forward User Mappings
    @RequestMapping(
            path="/mockedserver/config/{serverType}/user/proxy",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<ProxyForwardConfigResponseDTO> getServerConfigUserProxyMappings(
                                                                    @PathVariable("serverType") final String serverType,
                                                                    @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException {

        return new ResponseEntity<>(mockedServerEngineService.loadProxyForwardMappingsForUser(GeneralUtils.extractOAuthToken(bearerToken)), HttpStatus.OK);
    }

    @RequestMapping(
            path="/mockedserver/config/{serverType}/user/proxy",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> postServerConfigUserProxyMappings(
                                                @PathVariable("serverType") final String serverType,
                                                @RequestBody final ProxyForwardConfigDTO proxyForwardConfigDTO,
                                                @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws AuthException, RecordNotFoundException, ValidationException {

        mockedServerEngineService.saveProxyForwardMappingsForUser(
                proxyForwardConfigDTO,
                GeneralUtils.extractOAuthToken(bearerToken));

        return new ResponseEntity<>(HttpStatus.OK);
    }


    //
    // Live Logging
    @RequestMapping(
            path="/mockedserver/config/{serverType}/live-logging-block/endpoint",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> addLiveLoggingPathToBlock(@PathVariable("serverType") final String serverType,
                                                                     @RequestBody final LiveLoggingBlockingEndpointDTO liveLoggingBlockingEndpoint,
                                                                     @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws AuthException, ValidationException {

        mockedServerEngineService.addLiveLoggingPathToBlock(
                liveLoggingBlockingEndpoint.getMethod(),
                liveLoggingBlockingEndpoint.getPath(),
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
            throws AuthException, ValidationException {

        mockedServerEngineService.removeLiveLoggingPathToBlock(
                RestMethodEnum.findByName(method),
                path,
                GeneralUtils.extractOAuthToken(bearerToken));

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
