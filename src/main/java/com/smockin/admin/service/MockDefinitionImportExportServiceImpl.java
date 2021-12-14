package com.smockin.admin.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.smockin.admin.dto.MockImportConfigDTO;
import com.smockin.admin.dto.response.RestfulMockResponseDTO;
import com.smockin.admin.dto.response.S3MockBucketResponseDTO;
import com.smockin.admin.exception.MockExportException;
import com.smockin.admin.exception.MockImportException;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.RestfulMockDAO;
import com.smockin.admin.persistence.dao.S3MockDAO;
import com.smockin.admin.persistence.entity.S3Mock;
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
import java.util.Base64;
import java.util.List;
import java.util.Map;
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
    private S3MockService s3MockService;

    @Autowired
    private RestfulMockServiceUtils restfulMockServiceUtils;

    @Autowired
    private RestfulMockDAO restfulMockDAO;

    @Autowired
    private S3MockDAO s3MockDAO;


    @Override
    public String importFile(final MultipartFile file, final MockImportConfigDTO config, final String token)
            throws MockImportException, ValidationException, RecordNotFoundException {
        logger.debug("importFile called");

        final SmockinUser currentUser = userTokenServiceUtils.loadCurrentActiveUser(token);
        File tempDir = null;

        try {

            tempDir = Files.createTempDirectory(Long.toString(System.nanoTime())).toFile();
            final File uploadedFile = new File(tempDir + File.separator + file.getOriginalFilename());
            FileUtils.copyInputStreamToFile(file.getInputStream(), uploadedFile);

            final String conflictCtxPath = "import_" + GeneralUtils.createFileNameUniqueTimeStamp();

            return readImportArchiveFile(uploadedFile)
                    .entrySet()
                    .stream()
                    .map(m -> handleMockImport(m.getValue(), config, currentUser, conflictCtxPath))
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
    public String export(final List<String> selectedExports,
                         final String serverType,
                         final String token)
            throws MockExportException, RecordNotFoundException, ValidationException {

        final ServerTypeEnum serverTypeEnum = ServerTypeEnum.toServerType(serverType);

        if (serverTypeEnum == null) {
            throw new ValidationException("Invalid Server Type: " + serverType);
        }
        if (selectedExports == null || selectedExports.isEmpty()) {
            throw new ValidationException(String.format("No %s mocks are selected", serverTypeEnum.name()));
        }

        final String exportContent;
        final String exportFileName;

        final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);

        if (ServerTypeEnum.RESTFUL.equals(serverTypeEnum)) {
            exportContent = loadHTTPExportContent(selectedExports, smockinUser);
            exportFileName = restExportFileName + exportFileNameExt;
        } else if (ServerTypeEnum.S3.equals(serverTypeEnum)) {
            exportContent = loadS3ExportContent(selectedExports, smockinUser);
            exportFileName = s3ExportFileName + exportFileNameExt;
        } else {
            throw new ValidationException("Unsupported Server Type: " + serverTypeEnum);
        }

        final byte[] archiveBytes = GeneralUtils.createArchive(exportFileName, exportContent.getBytes());

        return Base64.getEncoder().encodeToString(archiveBytes);
    }

    //
    // Export related functions
    private String loadS3ExportContent(final List<String> selectedExports,
                                       final SmockinUser smockinUser) {

        final List<S3Mock> mocks = s3MockDAO.loadAllActiveByIds(selectedExports, smockinUser.getId());

        List<S3MockBucketResponseDTO> mockDTOs =
                mocks.stream()
                    .map(m ->
                        s3MockService.buildBucketDtoTree(m, true))
                    .collect(Collectors.toList());

        return GeneralUtils.serialiseJson(mockDTOs);
    }

    private String loadHTTPExportContent(final List<String> selectedExports,
                                         final SmockinUser smockinUser) {


        final List<RestfulMockResponseDTO> allRestfulMocks =
                restfulMockServiceUtils.buildRestfulMockDefinitionDTOs(restfulMockDAO.loadAllActiveByIds(selectedExports, smockinUser.getId()));

        final List<RestfulMockResponseDTO> restfulMocksToExport =
                (!selectedExports.isEmpty())
                    ? selectedExports.stream()
                        .map(r -> findRestByExternalId(r, allRestfulMocks))
                        .collect(Collectors.toList())
                    : allRestfulMocks;

        return GeneralUtils.serialiseJson(restfulMocksToExport);
    }

    private RestfulMockResponseDTO findRestByExternalId(final String externalId,
                                                        final List<RestfulMockResponseDTO> allRestfulMocks) throws RecordNotFoundException {
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

    private String handleMockImport(final String content, final MockImportConfigDTO config,
                                    final SmockinUser currentUser, final String conflictCtxPath) {

        final StringBuilder outcome = new StringBuilder();

        GeneralUtils.deserialiseJson(content, new TypeReference<List<RestfulMockResponseDTO>>() {})
            .stream()
            .forEach(rm -> {

                if (outcome.length() == 0) {
                    outcome.append("Successful Imports:\n\n");
                }

                restfulMockServiceUtils.preHandleExistingEndpoints(rm, config, currentUser, conflictCtxPath);

                try {

                    restfulMockService.createEndpoint(rm, currentUser.getSessionToken());

                    outcome.append(rm.getMethod());
                    outcome.append(" ");
                    outcome.append(rm.getPath());
                    outcome.append("\n");

                } catch (Throwable ex) {
                    outcome.append(handleImportFail(rm.getMethod() + " " + rm.getPath(), ex));
                }
            });

        return outcome.toString();
    }

    private String handleImportFail(final String info, final Throwable cause) {

        final String msg = "Error importing " + info;

        logger.error(msg, cause);

        return msg + "\n";
    }

}
