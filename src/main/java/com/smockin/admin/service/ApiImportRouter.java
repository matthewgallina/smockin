package com.smockin.admin.service;

import com.smockin.admin.dto.ApiImportDTO;
import com.smockin.admin.exception.ApiImportException;
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

    public void route(final ApiImportDTO dto, final String token) throws ApiImportException, ValidationException {
        logger.debug("route called");

        if (dto == null) {
            throw new ValidationException("No data found");
        }

        if (dto.getType() == null) {
            throw new ValidationException("Import Type is required");
        }

        if (logger.isDebugEnabled()) {
            logger.debug("API import type " + dto.getType());
        }

        switch (dto.getType()) {
            case RAML:
                ramlApiImportService.importApiDoc(dto, token);
                break;
            default:
                throw new ValidationException("Unsupported import type");
        }

    }

}
