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
    private final Map<Long, List<DefinitionCounter>> synchronizedCounter = new HashMap<Long, List<DefinitionCounter>>();

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

        final long mockId = restfulMock.getId();

        Long mockDefinitionId = null;

        synchronized (monitor) {

            // Load definition counters for mock endpoint
            final List<DefinitionCounter> definitionCounterList = synchronizedCounter.getOrDefault(mockId, new ArrayList<DefinitionCounter>() {
                {
                    // Create definition counters for mock endpoint if not present
                    restfulMock.getDefinitions().forEach(d ->
                        add(new DefinitionCounter(d.getId(), (d.getFrequencyCount() > 0)?d.getFrequencyCount():1))
                    );
                }
            });

            // Look up the next definition
            for (DefinitionCounter d : definitionCounterList) {
                if (d.getFrequencyCount() > d.getCurrentTally()) {
                    mockDefinitionId = d.getDefinitionId();
                    d.setCurrentTally(d.getCurrentTally()+1);
                    break;
                }
            }

            // If tally counters are all maxed out then reset and restart at the 1st definition
            if (mockDefinitionId == null) {

                for (DefinitionCounter d : definitionCounterList) {
                    if (mockDefinitionId == null) {
                        mockDefinitionId = d.getDefinitionId();
                        d.setCurrentTally(1);
                    } else {
                        d.setCurrentTally(0);
                    }
                }

            }

            synchronizedCounter.put(mockId, definitionCounterList);
        }

        // Finally load the definition for the given mockDefinitionId
        for (RestfulMockDefinitionOrder d : restfulMock.getDefinitions()) {
            if (d.getId() == mockDefinitionId) {
                return d;
            }
        }

        throw new NullPointerException("mockDefinitionId not found!");
    }

    RestfulMockDefinitionOrder getRandomResponse(final RestfulMock restfulMock) {

        final int randomIndex = RandomUtils.nextInt(0, restfulMock.getDefinitions().size());

        return restfulMock.getDefinitions().get(randomIndex);
    }


    private class DefinitionCounter {

        private final long definitionId;
        private final int frequencyCount;
        private int currentTally;

        DefinitionCounter(long definitionId, int frequencyCount) {
            this.definitionId = definitionId;
            this.frequencyCount = frequencyCount;
        }

        public long getDefinitionId() {
            return definitionId;
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
