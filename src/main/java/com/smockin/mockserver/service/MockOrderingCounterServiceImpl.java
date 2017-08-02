package com.smockin.mockserver.service;

import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.persistence.entity.RestfulMockDefinitionOrder;
import com.smockin.mockserver.service.dto.RestfulResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by gallina.
 */
@Service
public class MockOrderingCounterServiceImpl implements MockOrderingCounterService {

    private final Object monitor = new Object();
    private final Map<String, Integer> synchronizedCounter = new HashMap<String, Integer>();

    @Transactional
    public RestfulResponse getNextInSequence(final RestfulMock restfulMock) {

        final String extId = restfulMock.getExtId();

        Integer currentCount;

        synchronized (monitor) {
            currentCount = synchronizedCounter.get(extId);

            if (currentCount == null
                    || (currentCount + 1) > restfulMock.getDefinitions().size()) {
                currentCount = 0;
            }

            synchronizedCounter.put(extId, (currentCount + 1)); //  new int object, so that currentCount retains original value for use below.
        }

        final RestfulMockDefinitionOrder mockDefOrder = restfulMock.getDefinitions().get(currentCount);
        return new RestfulResponse(mockDefOrder.getHttpStatusCode(), mockDefOrder.getResponseContentType(), mockDefOrder.getResponseBody(), mockDefOrder.getResponseHeaders().entrySet());
    }
    
}
