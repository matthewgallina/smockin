package com.smockin.admin.controller;

import com.smockin.admin.dto.FtpMockDTO;
import com.smockin.admin.dto.response.FtpMockResponseDTO;
import com.smockin.admin.dto.response.SimpleMessageResponseDTO;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.service.FtpMockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;

/**
 * Created by mgallina.
 */
@Controller
public class FtpController {


    @Autowired
    private FtpMockService ftpMockService;

    @RequestMapping(path="/ftpmock", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<SimpleMessageResponseDTO<String>> create(@RequestBody final FtpMockDTO dto) {
        return new ResponseEntity<SimpleMessageResponseDTO<String>>(new SimpleMessageResponseDTO<String>(ftpMockService.createEndpoint(dto)), HttpStatus.CREATED);
    }

    @RequestMapping(path = "/ftpmock/{extId}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<String> update(@PathVariable("extId") final String extId, @RequestBody final FtpMockDTO dto) throws RecordNotFoundException {
        ftpMockService.updateEndpoint(extId, dto);
        return new ResponseEntity<String>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(path = "/ftpmock/{extId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<String> delete(@PathVariable("extId") final String extId) throws RecordNotFoundException {
        ftpMockService.deleteEndpoint(extId);
        return new ResponseEntity<String>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(path="/ftpmock", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<List<FtpMockResponseDTO>> get() {
        return new ResponseEntity<List<FtpMockResponseDTO>>(ftpMockService.loadAll(), HttpStatus.OK);
    }

    @RequestMapping(path="/ftpmock/{extId}/upload", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public @ResponseBody ResponseEntity<Void> uploadFile(@PathVariable("extId") final String extId, @RequestParam("file") MultipartFile file)
            throws RecordNotFoundException, ValidationException, IOException {
        ftpMockService.uploadFile(extId, file);
        return new ResponseEntity<Void>(HttpStatus.CREATED);
    }

}
