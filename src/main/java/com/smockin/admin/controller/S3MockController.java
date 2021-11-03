package com.smockin.admin.controller;

import com.smockin.admin.dto.S3MockBucketDTO;
import com.smockin.admin.dto.S3MockDirDTO;
import com.smockin.admin.dto.response.S3MockBucketResponseDTO;
import com.smockin.admin.dto.response.S3MockBucketResponseLiteDTO;
import com.smockin.admin.dto.response.SimpleMessageResponseDTO;
import com.smockin.admin.enums.S3MockTypeEnum;
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


    @RequestMapping(path="/s3mock/bucket", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<SimpleMessageResponseDTO<String>> createBucket(@RequestBody final S3MockBucketDTO dto,
                                                                                       @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException, ValidationException {

        return new ResponseEntity<>(new SimpleMessageResponseDTO<>(s3MockService.createS3Bucket(dto, GeneralUtils.extractOAuthToken(bearerToken))), HttpStatus.CREATED);
    }

    @RequestMapping(path="/s3mock/dir", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<SimpleMessageResponseDTO<String>> createDir(@RequestBody final S3MockDirDTO dto,
                                                                                    @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException, ValidationException {

        return new ResponseEntity<>(new SimpleMessageResponseDTO<>(s3MockService.createS3BucketDir(dto, GeneralUtils.extractOAuthToken(bearerToken))), HttpStatus.CREATED);
    }

    @RequestMapping(path="/s3mock/bucket/{extId}/upload", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public @ResponseBody ResponseEntity<SimpleMessageResponseDTO> uploadFileToDir(@PathVariable("extId") final String extId,
                                                                                  @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken,
                                                                                  @RequestParam("file") final MultipartFile file)
                                                                                    throws ValidationException {

        return new ResponseEntity<>(new SimpleMessageResponseDTO<>(s3MockService.uploadS3BucketFile(extId, S3MockTypeEnum.BUCKET, file, GeneralUtils.extractOAuthToken(bearerToken))), HttpStatus.CREATED);
    }

    @RequestMapping(path="/s3mock/dir/{extId}/upload", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public @ResponseBody ResponseEntity<SimpleMessageResponseDTO> uploadFiletoDir(@PathVariable("extId") final String extId,
                                                                                  @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken,
                                                                                  @RequestParam("file") final MultipartFile file)
            throws ValidationException {

        return new ResponseEntity<>(new SimpleMessageResponseDTO<>(s3MockService.uploadS3BucketFile(extId, S3MockTypeEnum.DIR, file, GeneralUtils.extractOAuthToken(bearerToken))), HttpStatus.CREATED);
    }

    @RequestMapping(path = "/s3mock/bucket/{extId}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<String> updateBucket(@PathVariable("extId") final String extId,
                                                             @RequestBody final S3MockBucketDTO dto,
                                                             @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException, ValidationException {

        s3MockService.updateS3Bucket(extId, dto, GeneralUtils.extractOAuthToken(bearerToken));

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(path = "/s3mock/dir/{extId}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<String> updateDir(@PathVariable("extId") final String extId,
                                                          @RequestBody final S3MockDirDTO dto,
                                                          @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException, ValidationException {

        s3MockService.updateS3Dir(extId, dto, GeneralUtils.extractOAuthToken(bearerToken));

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(path = "/s3mock/bucket/{extId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<String> deleteBucket(@PathVariable("extId") final String extId,
                                                             @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
                                                            throws RecordNotFoundException, ValidationException {

        s3MockService.deleteS3BucketOrFile(extId, S3MockTypeEnum.BUCKET, GeneralUtils.extractOAuthToken(bearerToken));

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(path = "/s3mock/dir/{extId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<String> deleteDir(@PathVariable("extId") final String extId,
                                                          @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException, ValidationException {

        s3MockService.deleteS3BucketOrFile(extId, S3MockTypeEnum.DIR, GeneralUtils.extractOAuthToken(bearerToken));

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(path = "/s3mock/file/{extId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<String> deleteFile(@PathVariable("extId") final String extId,
                                                           @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws RecordNotFoundException, ValidationException {

        s3MockService.deleteS3BucketOrFile(extId, S3MockTypeEnum.FILE, GeneralUtils.extractOAuthToken(bearerToken));

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(path="/s3mock/bucket", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<List<S3MockBucketResponseLiteDTO>> getAllBuckets(@RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
                                                                                throws RecordNotFoundException {

        return new ResponseEntity<>(s3MockService.loadAll(GeneralUtils.extractOAuthToken(bearerToken)), HttpStatus.OK);
    }

    @RequestMapping(path="/s3mock/bucket/{extId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<S3MockBucketResponseDTO> getBucket(@PathVariable("extId") final String extId,
                                                                           @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws ValidationException, RecordNotFoundException {

        return new ResponseEntity<>(s3MockService.loadById(extId, GeneralUtils.extractOAuthToken(bearerToken)), HttpStatus.OK);
    }

}

