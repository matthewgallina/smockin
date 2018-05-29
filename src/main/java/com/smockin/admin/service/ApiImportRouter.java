package com.smockin.admin.service;

import com.smockin.admin.dto.ApiImportDTO;
import com.smockin.admin.exception.ApiImportException;
import com.smockin.admin.exception.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class ApiImportRouter {

    @Autowired
    @Qualifier("ramlApiImportService")
    private ApiImportService ramlApiImportService;

    public void route(final ApiImportDTO dto) throws ApiImportException, ValidationException {

        if (dto == null) {
            throw new ValidationException("No data found");
        }

        if (dto.getType() == null) {
            throw new ValidationException("Import Type is required");
        }

        switch (dto.getType()) {
            case RAML:
                ramlApiImportService.importApiDoc(dto);
                break;
            default:
                throw new ValidationException("Unsupported import type");
        }

    }

}
