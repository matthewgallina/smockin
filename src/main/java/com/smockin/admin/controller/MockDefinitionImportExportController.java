package com.smockin.admin.controller;

import com.smockin.admin.dto.ExportMockDTO;
import com.smockin.admin.dto.MockImportConfigDTO;
import com.smockin.admin.exception.MockExportException;
import com.smockin.admin.exception.MockImportException;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.service.MockDefinitionImportExportService;
import com.smockin.utils.GeneralUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Optional;

/**
 * Created by mgallina.
 */
@Controller
public class MockDefinitionImportExportController {

    @Autowired
    private MockDefinitionImportExportService mockDefinitionImportExportService;

    @RequestMapping(path="/mock/import", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public @ResponseBody ResponseEntity<Void> importMocks(@RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken,
                                                          @RequestParam("file") final MultipartFile file)
                                                            throws ValidationException, MockImportException, RecordNotFoundException {

        final String token = GeneralUtils.extractOAuthToken(bearerToken);
        mockDefinitionImportExportService.importFile(file, new MockImportConfigDTO(), token);

        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @RequestMapping(path="/mock/export", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<String> exportMocks(@RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken,
                                                            @RequestBody final List<ExportMockDTO> exports)
                                                                throws MockExportException, RecordNotFoundException {

        final String token = GeneralUtils.extractOAuthToken(bearerToken);
        final String zipArchiveBase64 = mockDefinitionImportExportService.export((!exports.isEmpty()) ? Optional.of(exports) : Optional.empty(), token);

        return ResponseEntity.ok(zipArchiveBase64);
    }

}
