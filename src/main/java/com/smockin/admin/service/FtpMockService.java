package com.smockin.admin.service;

import com.smockin.admin.dto.FtpMockDTO;
import com.smockin.admin.dto.response.FtpMockResponseDTO;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Created by mgallina.
 */
public interface FtpMockService {

    String createEndpoint(final FtpMockDTO dto, final String token) throws RecordNotFoundException;
    void updateEndpoint(final String mockExtId, final FtpMockDTO dto, final String token) throws RecordNotFoundException, ValidationException;
    void deleteEndpoint(final String mockExtId, final String token) throws RecordNotFoundException, IOException, ValidationException;
    List<FtpMockResponseDTO> loadAll(final String token) throws RecordNotFoundException;
    void uploadFile(final String mockExtId, final MultipartFile file) throws RecordNotFoundException, IOException;
    List<String> loadUploadFiles(final String mockExtId) throws RecordNotFoundException, IOException;
    void deleteUploadedFile(final String mockExtId, final String uri) throws RecordNotFoundException, ValidationException, IOException;

}
