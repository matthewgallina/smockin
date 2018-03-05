package com.smockin.admin.service;

import com.smockin.admin.dto.FtpMockDTO;
import com.smockin.admin.dto.response.FtpMockResponseDTO;
import com.smockin.admin.exception.RecordNotFoundException;

import java.util.List;

/**
 * Created by mgallina.
 */
public interface FtpMockService {

    String createEndpoint(final FtpMockDTO dto);
    void updateEndpoint(final String mockExtId, final FtpMockDTO dto) throws RecordNotFoundException;
    void deleteEndpoint(final String mockExtId) throws RecordNotFoundException;
    List<FtpMockResponseDTO> loadAll();

}
