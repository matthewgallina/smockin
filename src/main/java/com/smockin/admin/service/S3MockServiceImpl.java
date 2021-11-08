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
import com.smockin.admin.persistence.entity.S3Mock;
import com.smockin.admin.persistence.entity.S3MockDir;
import com.smockin.admin.persistence.entity.S3MockFile;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import com.smockin.mockserver.dto.MockServerState;
import com.smockin.mockserver.engine.MockedS3ServerEngineUtils;
import com.smockin.mockserver.service.S3Client;
import com.smockin.utils.GeneralUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
                .save(new S3Mock(dto.getBucket(), dto.getStatus(), smockinUser))
                .getExtId();

        applyUpdateToRunningServer(cli ->
                cli.createBucket(dto.getBucket()));

        return extId;
    }

    @Override
    public String createS3BucketDir(final S3MockDirDTO dto,
                                    final String token) throws RecordNotFoundException, ValidationException {

        // Validation
        if (StringUtils.isBlank(dto.getName())) {
            throw new ValidationException("Directory name is required");
        }

        if (dto.getBucketExtId().isPresent() && dto.getBucketExtId().get() != null) {

            final S3Mock parentBucket = findS3Mock(dto.getBucketExtId().get(), token);

            return s3MockDirDAO
                    .save(new S3MockDir(dto.getName(), parentBucket))
                    .getExtId();
        }

        if (dto.getParentDirExtId().isPresent() && dto.getParentDirExtId().get() != null) {

            final S3MockDir parentDir = findS3MockDir(dto.getParentDirExtId().get(), token);

            return s3MockDirDAO
                    .save(new S3MockDir(dto.getName(), parentDir))
                    .getExtId();
        }

        throw new ValidationException("A parent bucket or parent directory is required");
    }

    @Override
    public String uploadS3BucketFile(final String extId, final S3MockTypeEnum type, final MultipartFile file, final String token)
            throws RecordNotFoundException, ValidationException {


        S3Mock mock = null;
        S3MockDir mockDir = null;

        if (S3MockTypeEnum.BUCKET.equals(type)) {
            mock = findS3Mock(extId, token);
        } else if (S3MockTypeEnum.DIR.equals(type)) {
            mockDir = findS3MockDir(extId, token);
        }

        try {

            final Optional<String> fileContent = GeneralUtils.convertInputStreamToString(file.getInputStream());

            if (!fileContent.isPresent()) {
                throw new FileUploadException("Error reading input stream for S3 file upload.");
            }

            if (mock != null) {

                final String bucketName = mock.getBucketName();
                final String newFileExtId = s3MockFileDAO.save(new S3MockFile(file.getOriginalFilename(), file.getContentType(), fileContent.get(), mock)).getExtId();

                // TODO apply to S3 Mock server if running

//                applyUpdateToRunningServer(cli ->
//                        cli.uploadObject(bucketName, null, null, file.getContentType()));

                return newFileExtId;
            }

            if (mockDir != null) {

                final String newFileExtId = s3MockFileDAO.save(new S3MockFile(file.getOriginalFilename(), file.getContentType(), fileContent.get(), mockDir)).getExtId();

                // TODO apply to S3 Mock server if running

//                applyUpdateToRunningServer(cli ->
//                        cli.uploadObject(null, null, null, file.getContentType()));

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

        s3Mock.setBucketName(dto.getBucket());
        s3Mock.setStatus(dto.getStatus());

        s3MockDAO.save(s3Mock);

        // TODO apply to S3 Mock server if running
    }

    @Override
    public void updateS3Dir(final String extId, final S3MockDirDTO dto, final String token)
            throws RecordNotFoundException, ValidationException {

        final S3MockDir s3MockDir = findS3MockDir(extId, token);

        s3MockDir.setName(dto.getName());

        s3MockDirDAO.save(s3MockDir);

        // TODO apply to S3 Mock server if running
    }

    @Override
    public void deleteS3BucketOrFile(final String extId, final S3MockTypeEnum type, final String token) throws RecordNotFoundException, ValidationException {

        if (S3MockTypeEnum.BUCKET.equals(type)) {
            final S3Mock s3Mock = findS3Mock(extId, token);
            final String bucketName = s3Mock.getBucketName();
            s3MockDAO.delete(s3Mock);

            applyUpdateToRunningServer(cli ->
                cli.deleteBucket(bucketName));

            return;
        }
        if (S3MockTypeEnum.DIR.equals(type)) {
            s3MockDirDAO.delete(findS3MockDir(extId, token));

            // TODO apply to S3 Mock server if running

            return;
        }
        if (S3MockTypeEnum.FILE.equals(type)) {
            s3MockFileDAO.delete(findS3MockFile(extId, token));

            // TODO apply to S3 Mock server if running

            return;
        }

        throw new ValidationException("Invalid S3 type: " + type);
    }

    @Override
    public List<S3MockBucketResponseLiteDTO> loadAll(final String token) throws RecordNotFoundException {

        final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);

        return s3MockDAO
                .findAllParentsByUser(smockinUser.getId())
                .stream()
                .map(m ->
                        new S3MockBucketResponseLiteDTO(m.getExtId(),
                                m.getBucketName(),
                                m.getStatus(),
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

        return buildBucketDtoTree(s3Mock, Optional.empty());
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

    S3MockBucketResponseDTO buildBucketDtoTree(final S3Mock s3Mock, final Optional<String> parentExtId) {

        final S3MockBucketResponseDTO dto = new S3MockBucketResponseDTO(
                s3Mock.getExtId(),
                s3Mock.getBucketName(),
                s3Mock.getStatus(),
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
                            mf.getContent()))
                .collect(Collectors.toList()));

        s3Mock
            .getChildrenDirs()
            .stream()
            .forEach(m ->
                dto.getChildren()
                        .add(buildDirDtoTree(m, Optional.of(s3Mock.getExtId()))));

        return dto;
    }

    S3MockDirResponseDTO buildDirDtoTree(final S3MockDir s3MockDir, final Optional<String> parentExtId) {

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
                                        mf.getContent()))
                        .collect(Collectors.toList()));

        s3MockDir
                .getChildren()
                .stream()
                .forEach(m ->
                        dto.getChildren()
                                .add(buildDirDtoTree(m, Optional.of(s3MockDir.getExtId()))));

        return dto;
    }

    void applyUpdateToRunningServer(final ApplyToServerAction action) {

        final MockServerState s3MockServerState = mockedServerEngineService.getS3ServerState();

        if (!s3MockServerState.isRunning()) {
            return;
        }

        action.execute(new S3Client(GeneralUtils.S3_HOST, s3MockServerState.getPort()));
    }

    @FunctionalInterface
    interface ApplyToServerAction {
        void execute(final S3Client S3Client);
    }

}
