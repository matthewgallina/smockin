package com.smockin.admin.service;

import com.smockin.admin.dto.JmsMockDTO;
import com.smockin.admin.dto.response.JmsMockResponseDTO;
import com.smockin.admin.exception.RecordNotFoundException;

import java.util.List;

/**
 * Created by mgallina.
 */
public interface JmsMockService {

    String createEndpoint(final JmsMockDTO dto);
    void updateEndpoint(final String mockExtId, final JmsMockDTO dto) throws RecordNotFoundException;
    void deleteEndpoint(final String mockExtId) throws RecordNotFoundException;
    List<JmsMockResponseDTO> loadAll();

}
