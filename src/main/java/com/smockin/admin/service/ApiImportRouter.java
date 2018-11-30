package com.smockin.admin.service;

import com.smockin.admin.dto.ApiImportDTO;
import com.smockin.admin.enums.ApiImportTypeEnum;
import com.smockin.admin.exception.MockImportException;
import com.smockin.admin.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class ApiImportRouter {

    private final Logger logger = LoggerFactory.getLogger(ApiImportRouter.class);

    @Autowired
    @Qualifier("ramlApiImportService")
    private ApiImportService ramlApiImportService;

    public void route(final String importType, final ApiImportDTO dto, final String token) throws MockImportException, ValidationException {
        logger.debug("route called");

        validate(importType, dto, token);

        switch (ApiImportTypeEnum.valueOf(importType)) {
            case RAML:
                ramlApiImportService.importApiDoc(dto, token);
                break;
            default:
                throw new ValidationException("Unsupported import type");
        }

    }

    void validate(final String importType, final ApiImportDTO dto, final String token) throws ValidationException {

        if (importType == null) {
            throw new ValidationException("Import Type is required");
        }

        try {
             ApiImportTypeEnum.valueOf(importType);
        } catch (Throwable ex) {
            throw new ValidationException("Invalid Import Type: " + importType);
        }

        if (dto == null) {
            throw new ValidationException("Inbound dto is undefined");
        }

        if (dto.getFile() == null) {
            throw new ValidationException("Inbound file (in dto) is undefined");
        }

        if (dto.getConfig() == null) {
            throw new ValidationException("Inbound config (in dto) is undefined");
        }

    }

}
