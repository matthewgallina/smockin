package com.smockin.admin.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.smockin.admin.dto.MockImportConfigDTO;
import com.smockin.admin.dto.S3MockBucketDTO;
import com.smockin.admin.dto.response.RestfulMockResponseDTO;
import com.smockin.admin.dto.response.S3MockBucketResponseDTO;
import com.smockin.admin.dto.response.S3MockDirResponseDTO;
import com.smockin.admin.dto.response.S3MockFileResponseDTO;
import com.smockin.admin.exception.MockExportException;
import com.smockin.admin.exception.MockImportException;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.RestfulMockDAO;
import com.smockin.admin.persistence.dao.S3MockDAO;
import com.smockin.admin.persistence.entity.S3Mock;
import com.smockin.admin.persistence.entity.S3MockDir;
import com.smockin.admin.persistence.entity.S3MockFile;
import com.smockin.admin.persistence.entity.S3MockFileContent;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.persistence.enums.ServerTypeEnum;
import com.smockin.admin.service.utils.RestfulMockServiceUtils;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import com.smockin.mockserver.dto.MockServerState;
import com.smockin.mockserver.engine.MockedS3ServerEngineUtils;
import com.smockin.mockserver.service.S3Client;
import com.smockin.utils.GeneralUtils;
import jakarta.transaction.Transactional;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    @Autowired
    private MockedS3ServerEngineUtils mockedS3ServerEngineUtils;

    @Autowired
    private MockedServerEngineService mockedServerEngineService;


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
    public String export(final List<String> selectedExports,
                         final ServerTypeEnum serverTypeEnum,
                         final String token)
            throws MockExportException, RecordNotFoundException, ValidationException {

        if (serverTypeEnum == null) {
            throw new ValidationException("Invalid Server Type");
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
        } else if (ServerTypeEnum.MAIL.equals(serverTypeEnum)) {
            // TODO
            throw new MockExportException("Unsupported Server Type: " + serverTypeEnum);
        } else {
            throw new ValidationException("Unsupported Server Type: " + serverTypeEnum);
        }

        final byte[] archiveBytes = GeneralUtils.createArchive(exportFileName, exportContent.getBytes());

        return GeneralUtils.base64Encode(archiveBytes);
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

        if (fileName.startsWith(s3ExportFileName)
                && fileName.endsWith(exportFileNameExt)) {
            return ServerTypeEnum.S3;
        }

        throw new MockImportException("Unable to determine server type for file: " + f.getName());
    }

    private String handleMockImport(final ServerTypeEnum serverType,
                                    final String content,
                                    final MockImportConfigDTO config,
                                    final SmockinUser currentUser,
                                    final String conflictCtxPath) {

        if (ServerTypeEnum.RESTFUL.equals(serverType)) {
            return handleRestImport(content, config, currentUser, conflictCtxPath);
        } else if (ServerTypeEnum.S3.equals(serverType)) {
            return handleS3Import(content, config, currentUser);
        } else if (ServerTypeEnum.MAIL.equals(serverType)) {
            // TODO
            throw new MockExportException("Unsupported Server Type: " + serverType);
        } else {
            throw new MockExportException("Unsupported Server Type: " + serverType);
        }

    }

    private String handleRestImport(final String content,
                                    final MockImportConfigDTO config,
                                    final SmockinUser currentUser,
                                    final String conflictCtxPath) {

        final StringBuilder outcome = new StringBuilder();

        GeneralUtils.deserialiseJson(content, new TypeReference<List<RestfulMockResponseDTO>>() {})
                .stream()
                .forEach(rm ->
                    processRestImport(outcome, config, rm, currentUser, conflictCtxPath));

        return outcome.toString();
    }

    void processRestImport(final StringBuilder outcome,
                           final MockImportConfigDTO config,
                           final RestfulMockResponseDTO rm,
                           final SmockinUser currentUser,
                           final String conflictCtxPath) {

        if (outcome.length() == 0) {
            outcome.append("Successful Imports:" + GeneralUtils.CARRIAGE + GeneralUtils.CARRIAGE);
        }

        restfulMockServiceUtils.preHandleExistingEndpoints(rm, config, currentUser, conflictCtxPath);

        try {

            restfulMockService.createEndpoint(rm, currentUser.getSessionToken());

            outcome.append(rm.getMethod());
            outcome.append(" ");
            outcome.append(rm.getPath());
            outcome.append(GeneralUtils.CARRIAGE);

        } catch (Throwable ex) {
            outcome.append(handleImportFail(rm.getMethod() + " " + rm.getPath(), ex));
        }

    }

    private String handleS3Import(final String content,
                                  final MockImportConfigDTO config,
                                  final SmockinUser currentUser) {

        final StringBuilder outcome = new StringBuilder();

        final List<String> newBuckets =
            GeneralUtils.deserialiseJson(content, new TypeReference<List<S3MockBucketResponseDTO>>() {})
                .stream()
                .map(s3 ->
                        processS3Import(outcome, s3, config, currentUser))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        // Update mock server with new imported buckets (if running)
        final MockServerState s3MockServerState = mockedServerEngineService.getS3ServerState();

        if (s3MockServerState.isRunning()) {
            GeneralUtils.executeAfterTransactionCommits(() -> {
                final S3Client s3Client = mockedS3ServerEngineUtils.buildS3Client(s3MockServerState.getPort());
                mockedS3ServerEngineUtils.loadAndInitBucketContentAsync(s3Client, newBuckets, currentUser.getId());
            });
        }

        return outcome.toString();
    }


    Optional<String> processS3Import(final StringBuilder outcome,
                                     final S3MockBucketResponseDTO s3BucketDTO,
                                     final MockImportConfigDTO config,
                                     final SmockinUser currentUser) {

        if (outcome.length() == 0) {
            outcome.append("Successful Imports:" + GeneralUtils.CARRIAGE + GeneralUtils.CARRIAGE);
        }

        final String bucketName = s3BucketDTO.getBucket();

        try {

            // Handle any existing buckets
            handleExistingBucketClash(s3BucketDTO, config, currentUser);

            // Create bucket
            final S3Mock bucket = createS3Bucket(s3BucketDTO, currentUser);

            // Add files to bucket root
            s3BucketDTO.getFiles()
                    .forEach(file ->
                        createS3BucketFile(file, Optional.of(bucket), Optional.empty()));

            // Start to build bucket dir structure and file content
            s3BucketDTO.getChildren()
                    .forEach(dir ->
                        populateS3BucketContent(dir, Optional.of(bucket), Optional.empty()));

            outcome.append(bucketName);
            outcome.append(GeneralUtils.CARRIAGE);

            // Update bucket (cascade persist all child content)
            return Optional.of(s3MockDAO.saveAndFlush(bucket).getExtId());

        } catch (Throwable ex) {

            outcome.append(handleImportFail(bucketName, ex));
            return Optional.empty();
        }

    }

    void handleExistingBucketClash(final S3MockBucketResponseDTO s3BucketDTO,
                                   final MockImportConfigDTO config,
                                   final SmockinUser currentUser) {
        logger.debug("handleExistingBucketClash called");

        Optional<Pair<String, String>> existingBucketIdOpt = s3MockService.doesBucketAlreadyExist(s3BucketDTO.getBucket());

        if (logger.isDebugEnabled()) {
            logger.debug("Keep Existing: "  + config.isKeepExisting());
            logger.debug("Existing Bucket: " + existingBucketIdOpt.isPresent());
        }

        if (config.isKeepExisting()
                && existingBucketIdOpt.isPresent()) {

            renameDuplicatingBucket(s3BucketDTO);

        } else if (existingBucketIdOpt.isPresent()) {

            if (StringUtils.equals(existingBucketIdOpt.get().getRight(), currentUser.getExtId())) {

                s3MockDAO.delete(s3MockDAO.findByExtId(existingBucketIdOpt.get().getLeft()));
                s3MockDAO.flush();

            } else {

                renameDuplicatingBucket(s3BucketDTO);

            }

        }

    }

    void renameDuplicatingBucket(final S3MockBucketResponseDTO s3BucketDTO) {

        final String duplicateName;
        if (s3BucketDTO.getBucket().length() + GeneralUtils.UNIQUE_TIMESTAMP_FORMAT.length() > 80) {
            duplicateName = StringUtils.substring(s3BucketDTO.getBucket(), 0, s3BucketDTO.getBucket().length() - GeneralUtils.UNIQUE_TIMESTAMP_FORMAT.length());
        } else {
            duplicateName = s3BucketDTO.getBucket();
        }

        s3BucketDTO.setBucket(duplicateName + "-" + GeneralUtils.createFileNameUniqueTimeStamp());
    }

    void populateS3BucketContent(final S3MockDirResponseDTO s3DirDTO,
                                 final Optional<S3Mock> parentBucket,
                                 final Optional<S3MockDir> parentDir) {

        // Create this dir
        final S3MockDir newDir = createS3BucketDir(s3DirDTO, parentBucket, parentDir);

        // Add files to this dir
        s3DirDTO.getFiles()
                .forEach(file ->
                    createS3BucketFile(file, Optional.empty(), Optional.of(newDir)));

        // Move onto the sub-dirs of this dir
        s3DirDTO.getChildren()
                .forEach(subDir ->
                    populateS3BucketContent(subDir, Optional.empty(), Optional.of(newDir)));

    }

    S3Mock createS3Bucket(final S3MockBucketDTO bucketDTO,
                          final SmockinUser currentUser)
            throws RecordNotFoundException, ValidationException {

        // Validation
        if (StringUtils.isBlank(bucketDTO.getBucket())) {
            throw new ValidationException("Bucket name is required");
        }

        return s3MockDAO
                .saveAndFlush(new S3Mock(bucketDTO.getBucket(),
                                 bucketDTO.getStatus(),
                                 bucketDTO.getSyncMode(),
                                 currentUser));
    }

    S3MockDir createS3BucketDir(final S3MockDirResponseDTO dirDTO,
                                final Optional<S3Mock> parentBucket,
                                final Optional<S3MockDir> parentDir) throws IllegalArgumentException {

        // Validation
        if (StringUtils.isBlank(dirDTO.getName())) {
            throw new IllegalArgumentException("Directory name is required");
        }

        if (parentBucket.isPresent()) {

            final S3Mock s3MockParent = parentBucket.get();
            final S3MockDir newS3MockDir = new S3MockDir(dirDTO.getName(), s3MockParent);
            s3MockParent.getChildrenDirs().add(newS3MockDir);

            return newS3MockDir;
        }

        if (parentDir.isPresent()) {

            final S3MockDir s3MockParentDir = parentDir.get();

            final S3MockDir newS3MockDir = new S3MockDir(dirDTO.getName(), s3MockParentDir);
            s3MockParentDir.getChildren().add(newS3MockDir);

            return newS3MockDir;
        }

        throw new IllegalArgumentException("A parent bucket or parent directory is required");
    }

     void createS3BucketFile(final S3MockFileResponseDTO fileDTO,
                             final Optional<S3Mock> bucket,
                             final Optional<S3MockDir> dir) {

        final String fileName = fileDTO.getName();
        final String mimeType = fileDTO.getMimeType();
        final String fileContent = fileDTO.getContent();

        if (fileName == null) {
            throw new IllegalArgumentException("File name cannot be blank");
        }
        if (mimeType == null) {
            throw new IllegalArgumentException("File mimeType cannot be blank");
        }
        if (fileContent == null) {
            throw new IllegalArgumentException("File content is missing");
        }

        if (bucket.isPresent()) {

            final S3Mock s3MockParent = bucket.get();

            final S3MockFile s3MockFile = new S3MockFile(fileName, mimeType, s3MockParent);
            final S3MockFileContent s3MockFileContent = new S3MockFileContent(s3MockFile, GeneralUtils.base64Encode(fileContent));
            s3MockFile.setFileContent(s3MockFileContent);

            s3MockParent.getFiles().add(s3MockFile);

            return;
        }

        if (dir.isPresent()) {

            final S3MockDir s3MockDirParent = dir.get();

            final S3MockFile s3MockFile = new S3MockFile(fileName, mimeType, s3MockDirParent);
            final S3MockFileContent s3MockFileContent = new S3MockFileContent(s3MockFile, GeneralUtils.base64Encode(fileContent));
            s3MockFile.setFileContent(s3MockFileContent);

            s3MockDirParent.getFiles().add(s3MockFile);

            return;
        }

        throw new IllegalArgumentException("A parent bucket or parent directory is required");
    }

    private String handleImportFail(final String info, final Throwable cause) {

        final String msg = "Error importing " + info;

        logger.error(msg, cause);

        return msg + GeneralUtils.CARRIAGE;
    }

}
