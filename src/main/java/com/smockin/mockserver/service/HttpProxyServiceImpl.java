package com.smockin.mockserver.service;

import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.RestfulMockDAO;
import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import com.smockin.mockserver.engine.MockedRestServerEngine;
import com.smockin.mockserver.service.bean.ProxiedKey;
import com.smockin.mockserver.service.dto.HttpProxiedDTO;
import com.smockin.mockserver.service.dto.RestfulResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;


/**
 * Created by mgallina on 09/08/17.
 */
@Service
@Transactional
public class HttpProxyServiceImpl implements HttpProxyService {

    private final Logger logger = LoggerFactory.getLogger(HttpProxyServiceImpl.class);

    // TODO Should add TTL and scheduled sweeper to stop the synchronizedProxyResponsesMap from building up.
    private Map<ProxiedKey, List<HttpProxiedDTO>> synchronizedProxyResponsesMap = new HashMap<>();

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();

    @Autowired
    private RestfulMockDAO restfulMockDAO;

    @Autowired
    private UserTokenServiceUtils userTokenServiceUtils;

    @Autowired
    private MockedRestServerEngine mockedRestServerEngine;

    @Override
    public RestfulResponseDTO waitForResponse(final String requestPath, final RestfulMock mock) {

        try {

            lock.lock();

            final List<HttpProxiedDTO> responses = synchronizedProxyResponsesMap.get(new ProxiedKey(requestPath, mock.getMethod()));

            if (responses == null || responses.isEmpty()) {

                // No matching response was found, so wait until one is added to the synchronizedProxyResponsesMap.

                final long timeOut = (mock.getProxyTimeOutInMillis() > 0)?mock.getProxyTimeOutInMillis():MAX_TIMEOUT_MILLIS;

                if (!condition.await(timeOut, TimeUnit.MILLISECONDS)) {

                    // The wait has timed out

                    if (logger.isDebugEnabled()) {
                        logger.debug("The wait for '" + mock.getMethod() + " " + requestPath + "' has timed out");
                    }

                    return null;
                }

                // Signal received i.e something has been added to the synchronizedProxyResponsesMap, so let's check if it's what this request wants.
                return waitForResponse(requestPath, mock);

            } else {

                // A matching path was found, so consume/remove the element from the synchronizedProxyResponsesMap, release the lock and return response.

                final HttpProxiedDTO proxiedResponse = responses.remove(0);
                return new RestfulResponseDTO(proxiedResponse.getHttpStatusCode(), proxiedResponse.getResponseContentType(), proxiedResponse.getBody(), new HashSet<Map.Entry<String, String>>());
            }

        } catch (InterruptedException ex) {
            logger.error("Error whilst waiting for proxied mock response from queue", ex);
        } finally {
            lock.unlock();
        }

        return null;
    }

    @Override
    public void addResponse(final String externalId, final HttpProxiedDTO dto, final String token) throws RecordNotFoundException, ValidationException {

        final RestfulMock mock = loadRestMock(externalId);

        userTokenServiceUtils.validateRecordOwner(mock.getCreatedBy(), token);

        final String path = mockedRestServerEngine.buildUserPath(mock);

        try {

            lock.lock();

            final ProxiedKey key = new ProxiedKey(path, dto.getMethod());
            final List<HttpProxiedDTO> responses = synchronizedProxyResponsesMap.getOrDefault(key, new ArrayList<>());
            responses.add(dto);

            synchronizedProxyResponsesMap.put(key, responses);

            if (logger.isDebugEnabled())
                logger.debug("Added dto " + path + ". Responses size is " + responses.size());

            // Signal ALL threads waiting on a proxied response to check synchronizedProxyResponsesMap.
            condition.signalAll();

        } finally {
            lock.unlock();
        }

    }

    @Override
    public void clearSession(final String externalId, final String token) throws RecordNotFoundException, ValidationException {

        final RestfulMock mock = loadRestMock(externalId);

        userTokenServiceUtils.validateRecordOwner(mock.getCreatedBy(), token);

        try {
            lock.lock();

            Arrays.stream(RestMethodEnum.values())
                    .forEach( rm -> synchronizedProxyResponsesMap.remove(new ProxiedKey(mockedRestServerEngine.buildUserPath(mock), rm)));

        } finally {
            lock.unlock();
        }

    }

    @Override
    public void clearAllSessions() {

        try {
            lock.lock();

            synchronizedProxyResponsesMap.clear();
        } finally {
            lock.unlock();
        }

    }

    RestfulMock loadRestMock(final String externalId) throws RecordNotFoundException {

        final RestfulMock mock = restfulMockDAO.findByExtId(externalId);

        if (mock == null) {
            throw new RecordNotFoundException();
        }

        return mock;
    }

}
