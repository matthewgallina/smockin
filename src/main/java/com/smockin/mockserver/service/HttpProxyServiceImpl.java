package com.smockin.mockserver.service;

import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.mockserver.service.bean.ProxiedKey;
import com.smockin.mockserver.service.dto.HttpProxiedDTO;
import com.smockin.mockserver.service.dto.RestfulResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;


/**
 * Created by mgallina on 09/08/17.
 */
@Service
public class HttpProxyServiceImpl implements HttpProxyService {

    private final Logger logger = LoggerFactory.getLogger(HttpProxyServiceImpl.class);

    // TODO Should add TTL and scheduled sweeper to stop the synchronizedProxyResponsesMap from building up.
    private Map<ProxiedKey, List<HttpProxiedDTO>> synchronizedProxyResponsesMap = new HashMap<ProxiedKey, List<HttpProxiedDTO>>();

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();


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
    public void addResponse(final HttpProxiedDTO dto) {

        // TODO
        // Need to add a guard to ensure only legitimate paths are added to the 'synchronizedProxyResponsesMap', to prevent this building up with duff/inaccessible data.

        try {

            lock.lock();

            final ProxiedKey key = new ProxiedKey(dto.getPath(), dto.getMethod());
            final List<HttpProxiedDTO> responses = synchronizedProxyResponsesMap.getOrDefault(key, new ArrayList<HttpProxiedDTO>());
            responses.add(dto);

            synchronizedProxyResponsesMap.put(key, responses);

            if (logger.isDebugEnabled())
                logger.debug("Added dto " + dto.getPath() + ". Responses size is " + responses.size());

            // Signal ALL threads waiting on a proxied response to check synchronizedProxyResponsesMap.
            condition.signalAll();

        } finally {
            lock.unlock();
        }

    }

    public void clearSession() {

        try {
            lock.lock();
            synchronizedProxyResponsesMap.clear();
        } finally {
            lock.unlock();
        }

    }

}
