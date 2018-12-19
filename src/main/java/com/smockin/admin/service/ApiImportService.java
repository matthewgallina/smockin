package com.smockin.admin.service;

import com.smockin.admin.dto.ApiImportDTO;
import com.smockin.admin.exception.MockImportException;
import com.smockin.admin.exception.ValidationException;

public interface ApiImportService {

    void importApiDoc(final ApiImportDTO dto, final String token) throws MockImportException, ValidationException;

}
