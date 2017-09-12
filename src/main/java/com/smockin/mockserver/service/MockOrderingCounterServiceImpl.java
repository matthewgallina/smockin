package com.smockin.mockserver.service;

import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.persistence.entity.RestfulMockDefinitionOrder;
import com.smockin.utils.GeneralUtils;
import com.smockin.mockserver.service.dto.RestfulResponseDTO;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by gallina.
 */
@Service
@Transactional
public class MockOrderingCounterServiceImpl implements MockOrderingCounterService {

    private final Object monitor = new Object();
    private final Map<String, List<DefinitionCounter>> synchronizedCounter = new HashMap<String, List<DefinitionCounter>>();

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

        final String mockExtId = restfulMock.getExtId();

        String mockDefinitionId = null;

        synchronized (monitor) {

            // Load definition counters for mock endpoint
            final List<DefinitionCounter> definitionCounterList = synchronizedCounter.getOrDefault(mockExtId, new ArrayList<DefinitionCounter>() {
                {
                    // Create definition counters for mock endpoint if not present
                    restfulMock.getDefinitions().forEach(d ->
                        add(new DefinitionCounter(d.getExtId(), (d.getFrequencyCount() > 0)?d.getFrequencyCount():1))
                    );
                }
            });

            // Look up the next definition
            for (DefinitionCounter d : definitionCounterList) {
                if (d.getFrequencyCount() > d.getCurrentTally()) {
                    mockDefinitionId = d.getDefinitionExtId();
                    d.setCurrentTally(d.getCurrentTally()+1);
                    break;
                }
            }

            // If tally counters are all maxed out then reset and restart at the 1st definition
            if (mockDefinitionId == null) {

                for (DefinitionCounter d : definitionCounterList) {
                    if (mockDefinitionId == null) {
                        mockDefinitionId = d.getDefinitionExtId();
                        d.setCurrentTally(1);
                    } else {
                        d.setCurrentTally(0);
                    }
                }

            }

            synchronizedCounter.put(mockExtId, definitionCounterList);
        }

        // Finally load the definition for the given mockDefinitionId
        for (RestfulMockDefinitionOrder d : restfulMock.getDefinitions()) {
            if (d.getExtId().equals(mockDefinitionId)) {
                return d;
            }
        }

        throw new NullPointerException("mockDefinitionId not found!");
    }

    RestfulMockDefinitionOrder getRandomResponse(final RestfulMock restfulMock) {

        final int randomIndex = RandomUtils.nextInt(0, restfulMock.getDefinitions().size());

        return restfulMock.getDefinitions().get(randomIndex);
    }

    public void clearState() {

        synchronized (monitor) {
            synchronizedCounter.clear();
        }
    }

    private class DefinitionCounter {

        private final String definitionExtId;
        private final int frequencyCount;
        private int currentTally;

        DefinitionCounter(String definitionExtId, int frequencyCount) {
            this.definitionExtId = definitionExtId;
            this.frequencyCount = frequencyCount;
        }

        public String getDefinitionExtId() {
            return definitionExtId;
        }
        public int getFrequencyCount() {
            return frequencyCount;
        }
        public int getCurrentTally() {
            return currentTally;
        }
        public void setCurrentTally(int currentTally) {
            this.currentTally = currentTally;
        }
    }

}
