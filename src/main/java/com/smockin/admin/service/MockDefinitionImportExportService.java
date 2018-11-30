package com.smockin.admin.service;

import com.smockin.admin.dto.ExportMockDTO;
import com.smockin.admin.dto.MockImportConfigDTO;
import com.smockin.admin.exception.MockExportException;
import com.smockin.admin.exception.MockImportException;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Optional;

public interface MockDefinitionImportExportService {

    String restExportFileName = "rest_export";
    String jmsExportFileName = "jms_export";
    String ftpExportFileName = "ftp_export";
    String exportFileNameExt = ".json";

    void importFile(final MultipartFile file, final MockImportConfigDTO config, final String token) throws MockImportException, ValidationException, RecordNotFoundException;
    String export(final Optional<List<ExportMockDTO>> selectedExports, final String token) throws MockExportException, RecordNotFoundException;

}
