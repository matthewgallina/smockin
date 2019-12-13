package com.smockin.admin.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.smockin.admin.dto.MockImportConfigDTO;
import com.smockin.admin.dto.response.RestfulMockResponseDTO;
import com.smockin.admin.exception.MockExportException;
import com.smockin.admin.exception.MockImportException;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.persistence.enums.ServerTypeEnum;
import com.smockin.admin.service.utils.RestfulMockServiceUtils;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import com.smockin.utils.GeneralUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import javax.transaction.Transactional;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional
public class MockDefinitionImportExportServiceImpl implements MockDefinitionImportExportService {

    private final Logger logger = LoggerFactory.getLogger(MockDefinitionImportExportServiceImpl.class);

    @Autowired
    private UserTokenServiceUtils userTokenServiceUtils;

    @Autowired
    private RestfulMockService restfulMockService;

    @Autowired
    private RestfulMockServiceUtils restfulMockServiceUtils;

    @Override
    public String importFile(final MultipartFile file, final MockImportConfigDTO config, final String token)
            throws MockImportException, ValidationException, RecordNotFoundException {
        logger.debug("importFile called");

        final SmockinUser currentUser = userTokenServiceUtils.loadCurrentUser(token);
        File tempDir = null;

        try {

            tempDir = Files.createTempDirectory(Long.toString(System.nanoTime())).toFile();
            final File uploadedFile = new File(tempDir + File.separator + file.getOriginalFilename());
            FileUtils.copyInputStreamToFile(file.getInputStream(), uploadedFile);

            final String conflictCtxPath = "import_" + GeneralUtils.createFileNameUniqueTimeStamp();

            return readImportArchiveFile(uploadedFile)
                    .entrySet()
                    .stream()
                    .map(m -> handleMockImport(m.getKey(), m.getValue(), config, currentUser, conflictCtxPath))
                    .collect(Collectors.joining());

        } catch (IOException ex) {
            throw new MockExportException("Error importing mock file");
        } finally {
            if (tempDir != null) {
                try {
                    FileUtils.deleteDirectory(tempDir);
                } catch (IOException ex) {
                    logger.error("Error deleting temp directory used for mock def import", ex);
                }
            }
        }

    }

    @Override
    public String export(final ServerTypeEnum serverType, final List<String> selectedExports, final String token)
            throws MockExportException, RecordNotFoundException {
        logger.debug("export called");

        final File exportFile;

        try {

            switch (serverType) {
                case RESTFUL:
                    exportFile = handleHTTPExport(selectedExports, token);
                    break;
                default:
                    throw new MockImportException("Unsupported server type: " + serverType);
            }

            final byte[] archiveBytes = GeneralUtils.createArchive(new File[] {
                exportFile
            });

            return Base64.getEncoder().encodeToString(archiveBytes);

        } catch (IOException ex) {
            throw new MockExportException("Error generating export file");
        }

    }

    //
    // Export related functions
    private File handleHTTPExport(final List<String> selectedExports, final String token) throws IOException {

        final List<RestfulMockResponseDTO> allRestfulMocks = restfulMockService.loadAll(token);

        final List<RestfulMockResponseDTO> restfulMocksToExport = (!selectedExports.isEmpty())
                ?
                selectedExports
                        .stream()
                        .map(r -> findRestByExternalId(r, allRestfulMocks))
                        .collect(Collectors.toList())
                :
                allRestfulMocks;

        final File restTempFile = File.createTempFile(restExportFileName, exportFileNameExt);

        FileUtils.writeStringToFile(restTempFile, GeneralUtils.serialiseJson(restfulMocksToExport), Charset.defaultCharset());

        return restTempFile;
    }

    private RestfulMockResponseDTO findRestByExternalId(final String externalId, final List<RestfulMockResponseDTO> allRestfulMocks) throws RecordNotFoundException {
        return allRestfulMocks
                .stream()
                .filter(r -> r.getExtId().equals(externalId))
                .findFirst()
                .orElseThrow(() -> new RecordNotFoundException());
    }

    //
    // Import related functions
    private Map<ServerTypeEnum, String> readImportArchiveFile(final File zipFile) throws MockImportException, ValidationException {

        if (zipFile == null || !zipFile.exists()) {
            throw new ValidationException("Cannot locate import file");
        }

        if (!zipFile.getName().endsWith(".zip")) {
            throw new ValidationException("Invalid file type. Expected archive .zip file type.");
        }

        try {

            final File tempDir = Files.createTempDirectory("smockin_tmp_import").toFile();
            GeneralUtils.unpackArchive(zipFile.getAbsolutePath(), tempDir.getAbsolutePath());

            return Stream.of(tempDir.listFiles()).collect(
                    Collectors.toMap(
                        f -> getServerTypeForFile(f),
                        f -> {
                            try {
                                return FileUtils.readFileToString(f, Charset.defaultCharset());
                            } catch (IOException e) {
                                throw new MockImportException("Error reading export file " + f.getName(), e);
                            }
                        })
            );

        } catch (IOException e) {
            throw new MockImportException("Error reading archive file " + zipFile.getName(), e);
        }
    }

    private ServerTypeEnum getServerTypeForFile(final File f) {

        final String fileName = f.getName();

        if (fileName.startsWith(restExportFileName)
                && fileName.endsWith(exportFileNameExt)) {
            return ServerTypeEnum.RESTFUL;
        }

        throw new MockImportException("Unable to determine server type for file: " + f.getName());
    }

    // TODO Ensure existing exports are backwards compatible with new projects field...
    private String handleMockImport(final ServerTypeEnum type, final String content, final MockImportConfigDTO config, final SmockinUser currentUser, final String conflictCtxPath) {

        final StringBuilder outcome = new StringBuilder();

        switch (type) {

            case RESTFUL:
                GeneralUtils.deserialiseJson(content, new TypeReference<List<RestfulMockResponseDTO>>() {})
                        .stream()
                        .forEach(rm -> {
                            restfulMockServiceUtils.preHandleExistingEndpoints(rm, config, currentUser, conflictCtxPath);
                            try {
                                restfulMockService.createEndpoint(rm, currentUser.getSessionToken());
                                outcome.append(handleImportPass(type, rm.getMethod() + " " + rm.getPath()));
                            } catch (Throwable ex) {
                                outcome.append(handleImportFail(type, rm.getMethod() + " " + rm.getPath(), ex));
                            }
                        });
                break;

            default:
                throw new MockImportException("Unsupported server type: " + type);
        }

        return outcome.toString();
    }

    private String handleImportPass(final ServerTypeEnum type, final String name) {

        return type.name() + " mock: " + name + " successfully imported\n";
    }

    private String handleImportFail(final ServerTypeEnum type, final String info, final Throwable cause) {

        final String msg = "Error importing " + type.name() + " mock: " + info;

        logger.error(msg, cause);

        return msg + "\n";
    }

}
