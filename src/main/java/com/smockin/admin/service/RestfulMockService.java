package com.smockin.admin.service;

import com.smockin.admin.dto.ProxyRestDuplicatePriorityDTO;
import com.smockin.admin.dto.RestfulMockDTO;
import com.smockin.admin.dto.response.ProxyRestDuplicateDTO;
import com.smockin.admin.dto.response.RestfulMockResponseDTO;
import com.smockin.admin.exception.AuthException;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;

import java.util.List;

/**
 * Created by mgallina.
 */
public interface RestfulMockService {

    String createEndpoint(final RestfulMockDTO dto, final String token) throws RecordNotFoundException;
    void updateEndpoint(final String mockExtId, final RestfulMockDTO dto, final String token) throws RecordNotFoundException, ValidationException;
    void deleteEndpoint(final String mockExtId, final String token) throws RecordNotFoundException, ValidationException;
    List<RestfulMockResponseDTO> loadAll(final String searchFilter, final String token) throws RecordNotFoundException;
    List<ProxyRestDuplicateDTO> loadAllUserPathDuplicates(final String token) throws RecordNotFoundException, AuthException;
    void saveUserPathDuplicatePriorities(final ProxyRestDuplicatePriorityDTO priorityMocks, final String token) throws RecordNotFoundException, AuthException;

}
