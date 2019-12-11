package com.smockin.admin.service;

import com.smockin.admin.dto.JmsMockDTO;
import com.smockin.admin.dto.response.JmsMockResponseDTO;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;

import java.util.List;

/**
 * Created by mgallina.
 */
public interface JmsMockService {

    String createEndpoint(final JmsMockDTO dto, final String token) throws RecordNotFoundException;
    void updateEndpoint(final String mockExtId, final JmsMockDTO dto, final String token) throws RecordNotFoundException, ValidationException;
    void deleteEndpoint(final String mockExtId, final String token) throws RecordNotFoundException, ValidationException;
    List<JmsMockResponseDTO> loadAll(final String token) throws RecordNotFoundException;

}
