package com.smockin.mockserver.service;

import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.persistence.entity.RestfulMockDefinitionOrder;
import com.smockin.utils.GeneralUtils;
import com.smockin.mockserver.service.dto.RestfulResponseDTO;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by gallina.
 */
@Service
@Transactional
public class MockOrderingCounterServiceImpl implements MockOrderingCounterService {

    private final Object monitor = new Object();
    private final Map<String, Integer> synchronizedCounter = new HashMap<String, Integer>();

    public RestfulResponseDTO process(final RestfulMock restfulMock) {

        final RestfulMockDefinitionOrder mockDef;

        if (restfulMock.isRandomiseDefinitions()) {
            mockDef = getRandomResponse(restfulMock);
        } else {
            mockDef = getNextInSequence(restfulMock);
        }

        GeneralUtils.checkForAndHandleSleep(mockDef.getSleepInMillis());

        return new RestfulResponseDTO(mockDef.getHttpStatusCode(), mockDef.getResponseContentType(), mockDef.getResponseBody(), mockDef.getResponseHeaders().entrySet());
    }

    RestfulMockDefinitionOrder getNextInSequence(final RestfulMock restfulMock) {

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

        return restfulMock.getDefinitions().get(currentCount);
    }

    RestfulMockDefinitionOrder getRandomResponse(final RestfulMock restfulMock) {

        final int randomIndex = RandomUtils.nextInt(0, restfulMock.getDefinitions().size());

        return restfulMock.getDefinitions().get(randomIndex);
    }

}
