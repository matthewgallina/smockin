package com.smockin.admin.service;

import com.smockin.admin.dto.S3MockDTO;
import com.smockin.admin.dto.response.S3MockResponseDTO;
import com.smockin.admin.dto.response.S3MockResponseLiteDTO;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface S3MockService {

    String createS3Bucket(final S3MockDTO dto, final String token) throws RecordNotFoundException, ValidationException;
    String uploadS3BucketFile(final String extId, final MultipartFile file, final String token) throws RecordNotFoundException, ValidationException;
    void updateS3Bucket(final String extId, final S3MockDTO dto, final String token) throws RecordNotFoundException, ValidationException;
    void deleteS3Bucket(final String extId, final String token) throws RecordNotFoundException, ValidationException;
    List<S3MockResponseLiteDTO> loadAll(final String token) throws RecordNotFoundException;
    S3MockResponseDTO loadById(final String extId, final String token) throws ValidationException, RecordNotFoundException;

}
