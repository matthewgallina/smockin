package com.smockin.admin.controller;

import com.smockin.admin.dto.S3MockDTO;
import com.smockin.admin.dto.response.S3MockResponseDTO;
import com.smockin.admin.dto.response.S3MockResponseLiteDTO;
import com.smockin.admin.dto.response.SimpleMessageResponseDTO;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.service.S3MockService;
import com.smockin.utils.GeneralUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Created by mgallina.
 */
@Controller
public class S3MockController {

    @Autowired
    private S3MockService s3MockService;


    @RequestMapping(path="/s3mock", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<SimpleMessageResponseDTO<String>> create(@RequestBody final S3MockDTO dto,
                                                                                 @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
                                                                                    throws RecordNotFoundException, ValidationException {

        return new ResponseEntity<>(new SimpleMessageResponseDTO<>(s3MockService.createS3Bucket(dto, GeneralUtils.extractOAuthToken(bearerToken))), HttpStatus.CREATED);
    }


    @RequestMapping(path="/s3mock/{extId}/upload", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public @ResponseBody ResponseEntity<Void> uploadFile(@PathVariable("extId") final String extId,
                                                         @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken,
                                                         @RequestParam("file") final MultipartFile file)
                                                            throws ValidationException {

        s3MockService.uploadS3BucketFile(extId, file, GeneralUtils.extractOAuthToken(bearerToken));

        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @RequestMapping(path = "/s3mock/{extId}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<String> update(@PathVariable("extId") final String extId,
                                                       @RequestBody final S3MockDTO dto,
                                                       @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
                                                            throws RecordNotFoundException, ValidationException {

        s3MockService.updateS3Bucket(extId, dto, GeneralUtils.extractOAuthToken(bearerToken));

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(path = "/s3mock/{extId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<String> delete(@PathVariable("extId") final String extId,
                                                       @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
                                                            throws RecordNotFoundException, ValidationException {

        s3MockService.deleteS3Bucket(extId, GeneralUtils.extractOAuthToken(bearerToken));

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(path="/s3mock", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<List<S3MockResponseLiteDTO>> getAll(@RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
                                                                                throws RecordNotFoundException {

        return new ResponseEntity<>(s3MockService.loadAll(GeneralUtils.extractOAuthToken(bearerToken)), HttpStatus.OK);
    }

    @RequestMapping(path="/s3mock/{extId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<S3MockResponseDTO> get(@PathVariable("extId") final String extId,
                                                               @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws ValidationException, RecordNotFoundException {

        return new ResponseEntity<>(s3MockService.loadById(extId, GeneralUtils.extractOAuthToken(bearerToken)), HttpStatus.OK);
    }

}

