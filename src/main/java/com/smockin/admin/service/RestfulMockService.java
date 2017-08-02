package com.smockin.admin.service;

import com.smockin.admin.dto.RestfulMockDTO;
import com.smockin.admin.dto.response.RestfulMockResponseDTO;
import com.smockin.admin.exception.RecordNotFoundException;

import java.util.List;

/**
 * Created by mgallina.
 */
public interface RestfulMockService {

    String createEndpoint(final RestfulMockDTO dto);
    void updateEndpoint(final String mockDefExtId, final RestfulMockDTO dto) throws RecordNotFoundException;
    void deleteEndpoint(final String mockDefExtId) throws RecordNotFoundException;
    List<RestfulMockResponseDTO> loadAll();
//    List<RestfulMockResponseDTO> loadAllByStatus(final RecordStatusEnum status);
//    String createRule(final String mockDefExtId, final RuleDTO dto) throws RecordNotFoundException;
//    void updateRule(final String ruleExtId, final RuleDTO dto) throws RecordNotFoundException;

}
