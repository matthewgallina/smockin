package com.smockin.mockserver.service;

import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.mockserver.service.dto.RestfulResponse;

/**
 * Created by gallina.
 */
public interface MockOrderingCounterService {

    RestfulResponse getNextInSequence(final RestfulMock restfulMockDefinition);

}
