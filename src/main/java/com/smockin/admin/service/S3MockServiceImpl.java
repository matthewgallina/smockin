package com.smockin.admin.service;

import com.smockin.admin.dto.S3MockDTO;
import com.smockin.admin.dto.response.S3MockFileResponseDTO;
import com.smockin.admin.dto.response.S3MockResponseDTO;
import com.smockin.admin.dto.response.S3MockResponseLiteDTO;
import com.smockin.admin.enums.S3MockTypeEnum;
import com.smockin.admin.exception.FileUploadException;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.S3MockDAO;
import com.smockin.admin.persistence.dao.S3MockFileDAO;
import com.smockin.admin.persistence.entity.S3Mock;
import com.smockin.admin.persistence.entity.S3MockFile;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import com.smockin.utils.GeneralUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
    private S3MockFileDAO s3MockFileDAO;

    @Autowired
    private UserTokenServiceUtils userTokenServiceUtils;

    @Override
    public String createS3Bucket(final S3MockDTO dto,
                                 final String token) throws RecordNotFoundException, ValidationException {

        // Validation
        if (StringUtils.isBlank(dto.getBucket())) {
            throw new ValidationException("bucket is required");
        }

        final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);

        // TODO will need equivalent here...
        // restfulMockServiceUtils.validateMockPathDoesNotStartWithUsername(dto.getPath());

        final S3Mock parent = (dto.getParentExtId() != null
                                && dto.getParentExtId().isPresent())
                ? findS3Mock(dto.getParentExtId().get(), token)
                : null;

        return s3MockDAO
                .save(new S3Mock(dto.getBucket(), dto.getStatus(), smockinUser, parent))
                .getExtId();
    }

    @Override
    public String uploadS3BucketFile(final String extId, final MultipartFile file, final String token)
            throws RecordNotFoundException, ValidationException {

        final S3Mock mock = findS3Mock(extId, token);

        InputStream fis = null;

        try {

            fis = file.getInputStream();

            final String fileContent = IOUtils.toString(fis, StandardCharsets.UTF_8.name());

            return s3MockFileDAO.save(new S3MockFile(file.getOriginalFilename(), file.getContentType(), fileContent, mock)).getExtId();

        } catch (IOException ex) {
            logger.error("Error uploading file for S3 Mock", ex);
            throw new FileUploadException("Error uploading file for S3 Mock", ex);
        } finally {
            GeneralUtils.closeSilently(fis);
        }

    }

    @Override
    public void updateS3Bucket(final String extId, final S3MockDTO dto, final String token)
            throws RecordNotFoundException, ValidationException {

        final S3Mock s3Mock = findS3Mock(extId, token);

        // TODO will need equivalent here...
        // restfulMockServiceUtils.validateMockPathDoesNotStartWithUsername(dto.getPath());

        s3Mock.setBucket(dto.getBucket());
        s3Mock.setStatus(dto.getStatus());

        s3MockDAO.save(s3Mock);
    }

    @Override
    public void deleteS3BucketOrFile(final String extId, final S3MockTypeEnum type, final String token) throws RecordNotFoundException, ValidationException {

        if (S3MockTypeEnum.DIR.equals(type)) {
            s3MockDAO.delete(findS3Mock(extId, token));
            return;
        }

        if (S3MockTypeEnum.FILE.equals(type)) {
            s3MockFileDAO.delete(findS3MockFile(extId, token));
            return;
        }

        throw new ValidationException("Invalid S3 type: " + type);
    }

    @Override
    public List<S3MockResponseLiteDTO> loadAll(final String token) throws RecordNotFoundException {

        final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);

        return s3MockDAO
                .findAllParentsByUser(smockinUser.getId())
                .stream()
                .map(m ->
                        new S3MockResponseLiteDTO(m.getExtId(),
                                m.getBucket(),
                                m.getCreatedBy().getCtxPath(),
                                m.getStatus(),
                                m.getDateCreated(),
                                m.getCreatedBy().getUsername(),
                                (m.getParent() != null)
                                        ? Optional.of(m.getParent().getExtId())
                                        : Optional.empty()))
                .collect(Collectors.toList());
    }

    @Override
    public S3MockResponseDTO loadById(final String extId, final String token) throws ValidationException, RecordNotFoundException {

        userTokenServiceUtils.loadCurrentActiveUser(token);

        final S3Mock s3Mock = s3MockDAO.findByExtId(extId);

        if (s3Mock == null) {
            throw new RecordNotFoundException();
        }

        userTokenServiceUtils.validateRecordOwner(s3Mock.getCreatedBy(), token);

        return buildDtoTree(s3Mock, Optional.empty());
    }

    S3Mock findS3Mock(final String extId, final String token) throws RecordNotFoundException, ValidationException {

        final S3Mock s3Mock = s3MockDAO.findByExtId(extId);

        if (s3Mock == null)
            throw new RecordNotFoundException();

        userTokenServiceUtils.validateRecordOwner(s3Mock.getCreatedBy(), token);

        return s3Mock;
    }

    S3MockFile findS3MockFile(final String extId, final String token) throws RecordNotFoundException, ValidationException {

        final S3MockFile s3MockFile = s3MockFileDAO.findByExtId(extId);

        if (s3MockFile == null)
            throw new RecordNotFoundException();

        userTokenServiceUtils.validateRecordOwner(s3MockFile.getS3Mock().getCreatedBy(), token);

        return s3MockFile;
    }

    S3MockResponseDTO buildDtoTree(final S3Mock s3Mock, final Optional<String> parentExtId) {

        final S3MockResponseDTO dto = new S3MockResponseDTO(
                s3Mock.getExtId(),
                s3Mock.getBucket(),
                s3Mock.getCreatedBy().getCtxPath(),
                s3Mock.getStatus(),
                s3Mock.getDateCreated(),
                s3Mock.getCreatedBy().getUsername(),
                parentExtId);

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
            .getChildren()
            .stream()
            .forEach(m ->
                dto.getChildren().add(buildDtoTree(m, Optional.of(s3Mock.getExtId()))));

        return dto;
    }

}
