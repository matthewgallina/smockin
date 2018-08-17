package com.smockin.admin.controller;

import com.smockin.admin.dto.FtpMockDTO;
import com.smockin.admin.dto.response.FtpMockResponseDTO;
import com.smockin.admin.dto.response.SimpleMessageResponseDTO;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.service.FtpMockService;
import com.smockin.utils.GeneralUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Created by mgallina.
 */
@Controller
public class FtpMockController {


    @Autowired
    private FtpMockService ftpMockService;

    @RequestMapping(path="/ftpmock", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<SimpleMessageResponseDTO<String>> create(@RequestBody final FtpMockDTO dto,
                                                                                 @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
                                                                                    throws RecordNotFoundException {
        return new ResponseEntity<>(new SimpleMessageResponseDTO<String>(ftpMockService.createEndpoint(dto, GeneralUtils.extractOAuthToken(bearerToken))), HttpStatus.CREATED);
    }

    @RequestMapping(path = "/ftpmock/{extId}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<Void> update(@PathVariable("extId") final String extId,
                                                     @RequestBody final FtpMockDTO dto,
                                                     @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
                                                        throws RecordNotFoundException, ValidationException {
        ftpMockService.updateEndpoint(extId, dto, GeneralUtils.extractOAuthToken(bearerToken));
        return ResponseEntity.noContent().build();
    }

    @RequestMapping(path = "/ftpmock/{extId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<Void> delete(@PathVariable("extId") final String extId,
                                                     @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
                                                        throws RecordNotFoundException, IOException, ValidationException {
        ftpMockService.deleteEndpoint(extId, GeneralUtils.extractOAuthToken(bearerToken));
        return ResponseEntity.noContent().build();
    }

    @RequestMapping(path="/ftpmock", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<List<FtpMockResponseDTO>> get(@RequestParam(value = "filter", required = false) final String searchFilter,
                                                                      @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
                                                                           throws RecordNotFoundException {
        return ResponseEntity.ok(ftpMockService.loadAll(searchFilter, GeneralUtils.extractOAuthToken(bearerToken)));
    }

    @RequestMapping(path="/ftpmock/{extId}/file/upload", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public @ResponseBody ResponseEntity<Void> uploadFile(@PathVariable("extId") final String extId,
                                                         @RequestParam("file") MultipartFile file)
            throws RecordNotFoundException, IOException {
        ftpMockService.uploadFile(extId, file);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @RequestMapping(path="/ftpmock/{extId}/file", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<List<String>> loadUploadFiles(@PathVariable("extId") final String extId)
            throws RecordNotFoundException, IOException {
        return ResponseEntity.ok(ftpMockService.loadUploadFiles(extId));
    }

    @RequestMapping(path="/ftpmock/{extId}/file", method = RequestMethod.DELETE)
    public @ResponseBody ResponseEntity<Void> deleteUploadedFile(@PathVariable("extId") final String extId,
                                                                 @RequestParam("uri") final String uri)
            throws RecordNotFoundException, ValidationException, IOException {
        ftpMockService.deleteUploadedFile(extId, uri);
        return ResponseEntity.noContent().build();
    }

}
