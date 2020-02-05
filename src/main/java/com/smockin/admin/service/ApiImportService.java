package com.smockin.admin.service;

import com.smockin.admin.dto.ApiImportDTO;
import com.smockin.admin.exception.MockImportException;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.nio.file.Files;

public interface ApiImportService {

    Logger logger = LoggerFactory.getLogger(ApiImportService.class);

    void handleApiDocImport(final ApiImportDTO dto, final File tempDir, final String token) throws MockImportException, ValidationException;

    default void validate(final ApiImportDTO dto) throws ValidationException {

        if (dto == null)
            throw new ValidationException("No data was provided");

        if (dto.getFile() == null)
            throw new ValidationException("No file found");

        if (dto.getConfig() == null)
            throw new ValidationException("No config found");

    }

    default void processFileImport(final ApiImportDTO dto, final String token) throws MockImportException, ValidationException {

        validate(dto);

        File tempDir = null;

        try {

            tempDir = Files.createTempDirectory(Long.toString(System.nanoTime())).toFile();

            handleApiDocImport(dto, tempDir, token);

        } catch (RecordNotFoundException ex) {
            throw new MockImportException("Unauthorized user access");
        } catch (MockImportException ex) {
            throw ex;
        } catch (Throwable ex) {
            logger.error("Unexpected error whilst importing API file", ex);
            throw new MockImportException("Unexpected error whilst importing API file");
        } finally {
            if (!FileUtils.deleteQuietly(tempDir)) {
                logger.error("Error deleting temp dir used for API import");
            }
        }

    }

}
