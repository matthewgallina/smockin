package com.smockin.admin.service;

import com.smockin.admin.dto.ApiImportDTO;
import com.smockin.admin.exception.ApiImportException;
import com.smockin.admin.exception.ValidationException;

public interface ApiImportService {

    void importApiDoc(final ApiImportDTO dto) throws ApiImportException, ValidationException;

}
