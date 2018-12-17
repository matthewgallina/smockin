package com.smockin.admin.service;

import com.smockin.admin.dto.MockImportConfigDTO;
import com.smockin.admin.exception.MockExportException;
import com.smockin.admin.exception.MockImportException;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.enums.ServerTypeEnum;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MockDefinitionImportExportService {

    String exportZipFileNamePrefix = "smockin_export_";
    String exportZipFileNameExt = ".zip";
    String restExportFileName = "rest_export";
    String jmsExportFileName = "jms_export";
    String ftpExportFileName = "ftp_export";
    String exportFileNameExt = ".json";

    String importFile(final MultipartFile file, final MockImportConfigDTO config, final String token) throws MockImportException, ValidationException, RecordNotFoundException;
    String export(final ServerTypeEnum serverType, final List<String> selectedExports, final String token) throws MockExportException, RecordNotFoundException;

}
