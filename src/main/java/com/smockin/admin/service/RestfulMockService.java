package com.smockin.admin.service;

import com.smockin.admin.dto.RestfulMockDTO;
import com.smockin.admin.dto.response.RestfulMockResponseDTO;
import com.smockin.admin.exception.AuthException;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;

import java.util.List;
import java.util.Map;

/**
 * Created by mgallina.
 */
public interface RestfulMockService {

    String createEndpoint(final RestfulMockDTO dto, final String token) throws RecordNotFoundException;
    void updateEndpoint(final String mockExtId, final RestfulMockDTO dto, final String token) throws RecordNotFoundException, ValidationException;
    void deleteEndpoint(final String mockExtId, final String token) throws RecordNotFoundException, ValidationException;
    List<RestfulMockResponseDTO> loadAll(final String searchFilter, final String token) throws RecordNotFoundException;
    Map<String, List<RestfulMockResponseDTO>> loadAllUserPathDuplicates(final String token) throws RecordNotFoundException, AuthException;

}
