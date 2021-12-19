package com.smockin.admin.service;

import com.smockin.admin.dto.S3MockBucketDTO;
import com.smockin.admin.dto.S3MockDirDTO;
import com.smockin.admin.dto.response.S3MockBucketResponseDTO;
import com.smockin.admin.dto.response.S3MockBucketResponseLiteDTO;
import com.smockin.admin.enums.S3MockTypeEnum;
import com.smockin.admin.exception.FileUploadException;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.entity.S3Mock;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface S3MockService {

    String createS3Bucket(final S3MockBucketDTO dto, final String token) throws RecordNotFoundException, ValidationException;
    String createS3BucketDir(final S3MockDirDTO dto, final String token) throws RecordNotFoundException, ValidationException;
    String uploadS3BucketFile(final String extId, final S3MockTypeEnum type, final MultipartFile file, final String token) throws RecordNotFoundException, ValidationException, FileUploadException;
    void updateS3Bucket(final String extId, final S3MockBucketDTO dto, final String token) throws RecordNotFoundException, ValidationException;
    void updateS3Dir(final String extId, final S3MockDirDTO dto, final String token) throws RecordNotFoundException, ValidationException;
    void deleteS3BucketOrFile(final String extId, final S3MockTypeEnum type, final String token) throws RecordNotFoundException, ValidationException;
    List<S3MockBucketResponseLiteDTO> loadAll(final String token) throws RecordNotFoundException;
    S3MockBucketResponseDTO loadById(final String extId, final String token) throws ValidationException, RecordNotFoundException;
    void syncS3Bucket(final String extId, final String token) throws RecordNotFoundException, ValidationException;
    boolean doesBucketAlreadyExist(final String name);
    S3MockBucketResponseDTO buildBucketDtoTree(final S3Mock s3Mock, final boolean includeFileContent);

}
