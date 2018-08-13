package com.smockin.admin.controller;

import com.smockin.admin.dto.ApiImportConfigDTO;
import com.smockin.admin.dto.ApiImportDTO;
import com.smockin.admin.enums.ApiKeepStrategyEnum;
import com.smockin.admin.exception.ApiImportException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.service.ApiImportRouter;
import com.smockin.utils.GeneralUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Created by mgallina.
 */
@Controller
public class ApiImportController {

    @Autowired
    private ApiImportRouter apiImportRouter;

    /*
    @RequestMapping(path="/api/import", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<Void> create(@RequestBody final ApiImportDTO dto,
                                                     @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
                                                        throws ApiImportException, ValidationException {

        apiImportRouter.route(dto, GeneralUtils.extractOAuthToken(bearerToken));

        return new ResponseEntity<>(HttpStatus.CREATED);
    }
    */

    @RequestMapping(path="/api/{type}/import", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public @ResponseBody ResponseEntity<Void> importApiFile(@PathVariable("type") final String importType,
                                                            @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken,
                                                            @RequestParam("file") final MultipartFile file)
                                                                throws ValidationException, ApiImportException {

        final ApiImportDTO dto = new ApiImportDTO(file, new ApiImportConfigDTO(ApiKeepStrategyEnum.RENAME_NEW));

        apiImportRouter.route(importType, dto, GeneralUtils.extractOAuthToken(bearerToken));

        return new ResponseEntity<>(HttpStatus.CREATED);
    }

}
