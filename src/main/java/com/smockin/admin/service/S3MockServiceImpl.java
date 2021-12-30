package com.smockin.admin.service;

import com.smockin.admin.dto.S3MockBucketDTO;
import com.smockin.admin.dto.S3MockDirDTO;
import com.smockin.admin.dto.response.S3MockBucketResponseDTO;
import com.smockin.admin.dto.response.S3MockBucketResponseLiteDTO;
import com.smockin.admin.dto.response.S3MockDirResponseDTO;
import com.smockin.admin.dto.response.S3MockFileResponseDTO;
import com.smockin.admin.enums.S3MockTypeEnum;
import com.smockin.admin.exception.FileUploadException;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.S3MockDAO;
import com.smockin.admin.persistence.dao.S3MockDirDAO;
import com.smockin.admin.persistence.dao.S3MockFileDAO;
import com.smockin.admin.persistence.entity.*;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.persistence.enums.S3SyncModeEnum;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import com.smockin.mockserver.dto.MockServerState;
import com.smockin.mockserver.engine.MockedS3ServerEngineUtils;
import com.smockin.mockserver.service.S3Client;
import com.smockin.utils.GeneralUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class S3MockServiceImpl implements S3MockService {

    private final Logger logger = LoggerFactory.getLogger(S3MockServiceImpl.class);

    @Autowired
    private S3MockDAO s3MockDAO;

    @Autowired
    private S3MockDirDAO s3MockDirDAO;

    @Autowired
    private S3MockFileDAO s3MockFileDAO;

    @Autowired
    private UserTokenServiceUtils userTokenServiceUtils;

    @Autowired
    private MockedServerEngineService mockedServerEngineService;

    @Autowired
    private MockedS3ServerEngineUtils mockedS3ServerEngineUtils;


    @Override
    public String createS3Bucket(final S3MockBucketDTO dto,
                                 final String token) throws RecordNotFoundException, ValidationException {

        // Validation
        if (StringUtils.isBlank(dto.getBucket())) {
            throw new ValidationException("Bucket name is required");
        }

        final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);

        final String extId = s3MockDAO
                .save(new S3Mock(dto.getBucket(), dto.getStatus(), dto.getSyncMode(), smockinUser))
                .getExtId();

        if (RecordStatusEnum.ACTIVE.equals(dto.getStatus())
                && !S3SyncModeEnum.NO_SYNC.equals(dto.getSyncMode())) {

            applyUpdateToRunningServer(cli -> {
                cli.createBucket(dto.getBucket());
                mockedS3ServerEngineUtils.handleS3Logging(String.format("User '%s' created bucket %s", smockinUser.getUsername(), dto.getBucket()),
                        smockinUser.getExtId());
            });

        }

        return extId;
    }

    @Override
    public String createS3BucketDir(final S3MockDirDTO dto,
                                    final String token) throws RecordNotFoundException, ValidationException {

        // Validation
        if (StringUtils.isBlank(dto.getName())) {
            throw new ValidationException("Directory name is required");
        }

        if (dto.getBucketExtId().isPresent()
                && dto.getBucketExtId().get() != null) {

            final S3Mock parentBucket = findS3Mock(dto.getBucketExtId().get(), token);

            final String extId = s3MockDirDAO
                    .save(new S3MockDir(dto.getName(), parentBucket))
                    .getExtId();

            if (RecordStatusEnum.ACTIVE.equals(parentBucket.getStatus())
                    && !S3SyncModeEnum.NO_SYNC.equals(parentBucket.getSyncMode())) {

                applyUpdateToRunningServer(cli -> {
                    cli.createSubDirectory(parentBucket.getBucketName(), dto.getName());
                    final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);
                    mockedS3ServerEngineUtils.handleS3Logging(String.format("User '%s' created directory '%s'", smockinUser.getUsername(), dto.getName()),
                            parentBucket.getCreatedBy().getExtId());
                });

            }

            return extId;
        }

        if (dto.getParentDirExtId().isPresent()
                && dto.getParentDirExtId().get() != null) {

            final S3MockDir parentDir = findS3MockDir(dto.getParentDirExtId().get(), token);

            final S3MockDir newDir = s3MockDirDAO
                    .save(new S3MockDir(dto.getName(), parentDir));

            final StringBuilder filePathTracer = new StringBuilder();
            final S3Mock bucket = mockedS3ServerEngineUtils.locateParentBucket(filePathTracer, newDir);

            // TODO test removing this duplicated line!!!
//            final S3Mock bucket = mockedS3ServerEngineUtils.locateParentBucket(newDir);

            if (RecordStatusEnum.ACTIVE.equals(bucket.getStatus())
                    && !S3SyncModeEnum.NO_SYNC.equals(bucket.getSyncMode())) {

                applyUpdateToRunningServer(cli -> {
                    cli.createSubDirectory(bucket.getBucketName(), filePathTracer.toString());
                    final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);
                    mockedS3ServerEngineUtils.handleS3Logging(String.format("User '%s' created directory '%s'", smockinUser.getUsername(), dto.getName()),
                            bucket.getCreatedBy().getExtId());
                });

            }

            return newDir.getExtId();
        }

        throw new ValidationException("A parent bucket or parent directory is required");
    }

    @Override
    public String uploadS3BucketFile(final String extId, final S3MockTypeEnum type, final MultipartFile file, final String token)
            throws RecordNotFoundException, ValidationException {


        final S3Mock bucket;
        final S3MockDir mockDir;

        if (S3MockTypeEnum.BUCKET.equals(type)) {
            bucket = findS3Mock(extId, token);
            mockDir = null;
        } else if (S3MockTypeEnum.DIR.equals(type)) {
            mockDir = findS3MockDir(extId, token);
            bucket = null;
        } else {
            throw new ValidationException("Invalid type " + type);
        }

        try {

            final InputStream is = file.getInputStream();
            final Optional<String> fileContent = GeneralUtils.convertInputStreamToString(is, true);

            if (!fileContent.isPresent()) {
                throw new FileUploadException("Error reading input stream for S3 file upload.");
            }

            final String originalFileName = file.getOriginalFilename();
            final String contentType = file.getContentType();

            if (bucket != null) {

                final String bucketName = bucket.getBucketName();
                final S3MockFile s3MockFile = new S3MockFile(originalFileName, contentType, bucket);
                final S3MockFileContent s3MockFileContent = new S3MockFileContent(s3MockFile, GeneralUtils.base64Encode(fileContent.get()));
                s3MockFile.setFileContent(s3MockFileContent);
                final String newFileExtId = s3MockFileDAO.save(s3MockFile).getExtId();

                if (RecordStatusEnum.ACTIVE.equals(bucket.getStatus())
                        && !S3SyncModeEnum.NO_SYNC.equals(bucket.getSyncMode())) {

                    applyUpdateToRunningServer(cli -> {
                        cli.uploadObject(bucketName, originalFileName, IOUtils.toInputStream(fileContent.get(), Charset.defaultCharset()), contentType);
                        final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);
                        mockedS3ServerEngineUtils.handleS3Logging(String.format("User '%s' uploaded file '%s' to bucket '%s'", smockinUser.getUsername(), originalFileName, bucket),
                                bucket.getCreatedBy().getExtId());
                    });

                }

                return newFileExtId;
            }

            if (mockDir != null) {

                final S3MockFile s3MockFile = new S3MockFile(originalFileName, contentType, mockDir);
                final S3MockFileContent s3MockFileContent = new S3MockFileContent(s3MockFile, GeneralUtils.base64Encode(fileContent.get()));
                s3MockFile.setFileContent(s3MockFileContent);
                final String newFileExtId = s3MockFileDAO.save(s3MockFile).getExtId();

                final S3Mock parentBucket = mockedS3ServerEngineUtils.locateParentBucket(mockDir);
                final Pair<String, String> filePath = mockedS3ServerEngineUtils.extractBucketAndFilePath(s3MockFile);

                if (RecordStatusEnum.ACTIVE.equals(parentBucket.getStatus())
                        && !S3SyncModeEnum.NO_SYNC.equals(parentBucket.getSyncMode())) {

                    applyUpdateToRunningServer(cli -> {
                        cli.uploadObject(parentBucket.getBucketName(), filePath.getRight(), IOUtils.toInputStream(fileContent.get(), Charset.defaultCharset()), contentType);
                        final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);
                        mockedS3ServerEngineUtils.handleS3Logging(String.format("User '%s' uploaded file '%s' to bucket '%s'", smockinUser.getUsername(), filePath.getRight(), parentBucket.getBucketName()),
                                parentBucket.getCreatedBy().getExtId());
                    });

                }

                return newFileExtId;
            }

            throw new RecordNotFoundException();

        } catch (IOException ex) {
            logger.error("Error uploading file for S3 Mock", ex);
            throw new FileUploadException("Error uploading file for S3 Mock", ex);
        }

    }

    @Override
    public void updateS3Bucket(final String extId, final S3MockBucketDTO dto, final String token)
            throws RecordNotFoundException, ValidationException {

        final S3Mock s3Mock = findS3Mock(extId, token);

        final String originalBucket = s3Mock.getBucketName();

        s3Mock.setBucketName(dto.getBucket());
        s3Mock.setStatus(dto.getStatus());
        s3Mock.setSyncMode(dto.getSyncMode());

        s3MockDAO.save(s3Mock);

        // Remove current bucket and re-create with latest content...
        if (RecordStatusEnum.ACTIVE.equals(s3Mock.getStatus())
                && !S3SyncModeEnum.NO_SYNC.equals(s3Mock.getSyncMode())) {

            applyUpdateToRunningServer(
                    cli -> cli.deleteBucket(originalBucket, true),
                    cli -> {
                        mockedS3ServerEngineUtils.initBucketContent(cli, s3Mock);

                        if (!StringUtils.equals(originalBucket, dto.getBucket())) {
                            final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);
                            mockedS3ServerEngineUtils.handleS3Logging(String.format("User '%s' renamed bucket '%s' to '%s'", smockinUser.getUsername(), originalBucket, dto.getBucket()),
                                    s3Mock.getCreatedBy().getExtId());
                        }
                    }
            );

        }

    }

    @Override
    public void updateS3Dir(final String extId, final S3MockDirDTO dto, final String token)
            throws RecordNotFoundException, ValidationException {

        final S3MockDir s3MockDir = findS3MockDir(extId, token);
        final String originalName = s3MockDir.getName();

        s3MockDir.setName(dto.getName());

        s3MockDirDAO.save(s3MockDir);

        final S3Mock s3Mock = mockedS3ServerEngineUtils.locateParentBucket(s3MockDir);

        if (RecordStatusEnum.INACTIVE.equals(s3Mock.getStatus())
                || S3SyncModeEnum.NO_SYNC.equals(s3Mock.getSyncMode())) {
            return;
        }

        // Remove current bucket and re-create with latest content...
        applyUpdateToRunningServer(
                cli -> cli.deleteBucket(s3Mock.getBucketName(), true),
                cli -> {
                    mockedS3ServerEngineUtils.initBucketContent(cli, s3Mock);
                    final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);
                    mockedS3ServerEngineUtils.handleS3Logging(String.format("User '%s' renamed directory '%s' to '%s'", smockinUser.getUsername(), originalName, dto.getName()),
                            s3Mock.getCreatedBy().getExtId());
                }
        );

    }

    @Override
    public void deleteS3BucketOrFile(final String extId, final S3MockTypeEnum type, final String token) throws RecordNotFoundException, ValidationException {

        if (logger.isDebugEnabled())
            logger.debug(String.format("deleteS3BucketOrFile called (type: %s)", type));

        if (S3MockTypeEnum.BUCKET.equals(type)) {

            final S3Mock s3Mock = findS3Mock(extId, token);
            final String bucketName = s3Mock.getBucketName();
            final RecordStatusEnum status = s3Mock.getStatus();
            s3MockDAO.delete(s3Mock);

            if (RecordStatusEnum.INACTIVE.equals(status)
                    || S3SyncModeEnum.NO_SYNC.equals(s3Mock.getSyncMode())) {
                return;
            }

            applyUpdateToRunningServer(cli -> {
                cli.deleteBucket(bucketName, true);
                final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);
                mockedS3ServerEngineUtils.handleS3Logging(String.format("User '%s' deleted bucket '%s'", smockinUser.getUsername(), bucketName),
                        s3Mock.getCreatedBy().getExtId());
            });

            return;
        }

        if (S3MockTypeEnum.DIR.equals(type)) {

            final S3MockDir dir = findS3MockDir(extId, token);
            final long bucketId = mockedS3ServerEngineUtils.locateParentBucket(dir).getId();
            final String directoryName = dir.getName();

            s3MockDirDAO.delete(dir);
            s3MockDirDAO.flush();

            final S3Mock bucket = s3MockDAO.getById(bucketId);

            if (RecordStatusEnum.INACTIVE.equals(bucket.getStatus())
                    || S3SyncModeEnum.NO_SYNC.equals(bucket.getSyncMode())) {
                return;
            }

            // Remove current bucket and re-create with latest content...
            applyUpdateToRunningServer(
                    cli -> cli.deleteBucket(bucket.getBucketName(), true),
                    cli -> {
                        mockedS3ServerEngineUtils.initBucketContent(cli, bucket);
                        final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);
                        mockedS3ServerEngineUtils.handleS3Logging(String.format("User '%s' deleted directory '%s' in bucket '%s'", smockinUser.getUsername(), directoryName, bucket.getBucketName()),
                                bucket.getCreatedBy().getExtId());
                    }
            );

            return;
        }

        if (S3MockTypeEnum.FILE.equals(type)) {

            final S3MockFile s3MockFile = findS3MockFile(extId, token);

            final String filePath;
            final S3Mock bucket;

            if (s3MockFile.getS3Mock() != null) {

                filePath = s3MockFile.getName();
                bucket = s3MockFile.getS3Mock();

            } else {

                filePath = mockedS3ServerEngineUtils.extractBucketAndFilePath(s3MockFile).getRight();
                bucket = (s3MockFile.getS3Mock() != null)
                        ? s3MockFile.getS3Mock()
                        : mockedS3ServerEngineUtils.locateParentBucket(s3MockFile.getS3MockDir());

            }

            s3MockFileDAO.delete(s3MockFile);

            if (bucket != null) {

                if (RecordStatusEnum.INACTIVE.equals(bucket.getStatus())
                        || S3SyncModeEnum.NO_SYNC.equals(bucket.getSyncMode())) {
                    return;
                }

                applyUpdateToRunningServer(cli -> {
                    cli.deleteObject(bucket.getBucketName(), filePath);
                    final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);
                    mockedS3ServerEngineUtils.handleS3Logging(String.format("User '%s' deleted file '%s' in bucket '%s'", smockinUser.getUsername(), filePath, bucket.getBucketName()),
                            bucket.getCreatedBy().getExtId());
                });
            }

            return;
        }

        throw new ValidationException("Invalid S3 type: " + type);
    }

    @Override
    public List<S3MockBucketResponseLiteDTO> loadAll(final String token) throws RecordNotFoundException {

        final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);

        return s3MockDAO
                .findAllBucketsByUser(smockinUser.getId())
                .stream()
                .map(m ->
                        new S3MockBucketResponseLiteDTO(m.getExtId(),
                                m.getBucketName(),
                                m.getStatus(),
                                m.getSyncMode(),
                                m.getDateCreated(),
                                m.getCreatedBy().getUsername()))
                .collect(Collectors.toList());
    }

    @Override
    public S3MockBucketResponseDTO loadById(final String extId, final String token) throws ValidationException, RecordNotFoundException {

        userTokenServiceUtils.loadCurrentActiveUser(token);

        final S3Mock s3Mock = s3MockDAO.findByExtId(extId);

        if (s3Mock == null) {
            throw new RecordNotFoundException();
        }

        userTokenServiceUtils.validateRecordOwner(s3Mock.getCreatedBy(), token);

        return buildBucketDtoTree(s3Mock, false);
    }

    @Override
    public void resetS3BucketOnMockServer(final String extId, final String token) throws RecordNotFoundException, ValidationException {

        final S3Mock s3Mock = findS3Mock(extId, token);

        if (RecordStatusEnum.INACTIVE.equals(s3Mock.getStatus())) {
            return;
        }

        applyUpdateToRunningServer(
                cli -> cli.deleteBucket(s3Mock.getBucketName(), true),
                cli -> {
                    mockedS3ServerEngineUtils.initBucketContent(cli, s3Mock);
                    final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);
                    mockedS3ServerEngineUtils.handleS3Logging(String.format("User '%s' has reset bucket '%s'", smockinUser.getUsername(), s3Mock.getBucketName()),
                            s3Mock.getCreatedBy().getExtId());
                }
        );
    }

    S3Mock findS3Mock(final String extId, final String token) throws RecordNotFoundException, ValidationException {

        final S3Mock s3Mock = s3MockDAO.findByExtId(extId);

        if (s3Mock == null)
            throw new RecordNotFoundException();

        userTokenServiceUtils.validateRecordOwner(s3Mock.getCreatedBy(), token);

        return s3Mock;
    }

    S3MockDir findS3MockDir(final String extId, final String token) throws RecordNotFoundException, ValidationException {

        final S3MockDir s3MockDir = s3MockDirDAO.findByExtId(extId);

        if (s3MockDir == null)
            throw new RecordNotFoundException();

        userTokenServiceUtils.validateRecordOwner(mockedS3ServerEngineUtils.locateParentBucket(s3MockDir).getCreatedBy(), token);

        return s3MockDir;
    }

    S3MockFile findS3MockFile(final String extId, final String token) throws RecordNotFoundException, ValidationException {

        final S3MockFile s3MockFile = s3MockFileDAO.findByExtId(extId);

        if (s3MockFile == null)
            throw new RecordNotFoundException();

        final S3Mock bucket = (s3MockFile.getS3Mock() != null)
                ? s3MockFile.getS3Mock()
                : mockedS3ServerEngineUtils.locateParentBucket(s3MockFile.getS3MockDir());

        userTokenServiceUtils.validateRecordOwner(bucket.getCreatedBy(), token);

        return s3MockFile;
    }

    public Optional<Pair<String, String>> doesBucketAlreadyExist(final String name) {

        final S3Mock s3Mock = s3MockDAO.findByBucketName(name);

        if (s3Mock == null) {
            return Optional.empty();
        }

        return Optional.of(Pair.of(s3Mock.getExtId(), s3Mock.getCreatedBy().getExtId()));
    }

    public S3MockBucketResponseDTO buildBucketDtoTree(final S3Mock s3Mock,
                                                      final boolean includeBase64EncodedFileContent) {

        final S3MockBucketResponseDTO dto = new S3MockBucketResponseDTO(
                s3Mock.getExtId(),
                s3Mock.getBucketName(),
                s3Mock.getStatus(),
                s3Mock.getSyncMode(),
                s3Mock.getDateCreated(),
                s3Mock.getCreatedBy().getUsername());

        dto.getFiles()
            .addAll(s3Mock
                .getFiles()
                .stream()
                .map(mf ->
                        new S3MockFileResponseDTO(
                            mf.getExtId(),
                            mf.getName(),
                            mf.getMimeType(),
                                (includeBase64EncodedFileContent)
                                        ? mf.getFileContent().getContent() // straight from DB so already base64 encoded
                                        : null))
                .collect(Collectors.toList()));

        s3Mock
            .getChildrenDirs()
            .stream()
            .forEach(m ->
                dto.getChildren()
                        .add(buildDirDtoTree(m, includeBase64EncodedFileContent)));

        return dto;
    }

    S3MockDirResponseDTO buildDirDtoTree(final S3MockDir s3MockDir,
                                         final boolean includeBase64EncodedFileContent) {

        final S3MockDirResponseDTO dto = new S3MockDirResponseDTO(
                s3MockDir.getExtId(),
                s3MockDir.getName(),
                (s3MockDir.getS3Mock() != null)
                        ? Optional.of(s3MockDir.getS3Mock().getExtId())
                        : Optional.empty(),
                (s3MockDir.getParent() != null)
                        ? Optional.of(s3MockDir.getParent().getExtId())
                        : Optional.empty());

        dto.getFiles()
                .addAll(s3MockDir
                        .getFiles()
                        .stream()
                        .map(mf ->
                                new S3MockFileResponseDTO(
                                        mf.getExtId(),
                                        mf.getName(),
                                        mf.getMimeType(),
                                        (includeBase64EncodedFileContent)
                                                ? mf.getFileContent().getContent() // straight from DB so already base64 encoded
                                                : null))
                        .collect(Collectors.toList()));

        s3MockDir
                .getChildren()
                .stream()
                .forEach(m ->
                        dto.getChildren()
                                .add(buildDirDtoTree(m, includeBase64EncodedFileContent)));

        return dto;
    }

    // TODO should we make this async...?
    void applyUpdateToRunningServer(final ApplyToMockServerAction ... actions) {

        if (actions == null || actions.length == 0) {
            return;
        }

        final MockServerState s3MockServerState = mockedServerEngineService.getS3ServerState();

        if (!s3MockServerState.isRunning()) {
            return;
        }

        final S3Client s3Client = mockedS3ServerEngineUtils.buildS3Client(s3MockServerState.getPort());

        for (ApplyToMockServerAction action : actions) {
            action.execute(s3Client);
        }

    }

    @FunctionalInterface
    interface ApplyToMockServerAction {
        void execute(final S3Client S3Client);
    }

}
