package com.smockin.mockserver.service;

import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.mockserver.service.dto.RestfulResponseDTO;

/**
 * Created by gallina.
 */
public interface MockOrderingCounterService {

    RestfulResponseDTO process(final RestfulMock restfulMockDefinition);
    void clearState();

}
