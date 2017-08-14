package com.smockin.mockserver.service;

import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.mockserver.service.bean.ProxiedKey;
import com.smockin.mockserver.service.dto.ProxiedDTO;
import com.smockin.mockserver.service.dto.RestfulResponse;
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
public class ProxyServiceImpl implements ProxyService {

    private final Logger logger = LoggerFactory.getLogger(ProxyServiceImpl.class);

    private Map<ProxiedKey, List<ProxiedDTO>> synchronizedProxyResponsesMap = new HashMap<ProxiedKey, List<ProxiedDTO>>();

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();


    @Override
    public RestfulResponse waitForResponse(final RestfulMock mock) {

        try {

            lock.lock();

            final List<ProxiedDTO> responses = synchronizedProxyResponsesMap.get(new ProxiedKey(mock.getPath(), mock.getMethod()));

            if (responses == null || responses.isEmpty()) {

                // No matching response was found, so wait until one is added to the synchronizedProxyResponsesMap.

                final long timeOut = (mock.getProxyTimeOutInMillis() > 0)?mock.getProxyTimeOutInMillis():MAX_TIMEOUT_MILLIS;

                if (!condition.await(timeOut, TimeUnit.MILLISECONDS)) {

                    // The wait has timed out

                    if (logger.isDebugEnabled()) {
                        logger.debug("The wait for '" + mock.getMethod() + " " + mock.getPath() + "' has timed out");
                    }

                    return null;
                }

                // Signal received i.e something has been added to the synchronizedProxyResponsesMap, so let's check if it's what this request wants.
                return waitForResponse(mock);

            } else {

                // A matching path was found, so consume/remove the element from the synchronizedProxyResponsesMap, release the lock and return response.

                final ProxiedDTO proxiedResponse = responses.remove(0);
                return new RestfulResponse(proxiedResponse.getHttpStatusCode(), proxiedResponse.getResponseContentType(), proxiedResponse.getBody(), new HashSet<Map.Entry<String, String>>());
            }

        } catch (InterruptedException ex) {
            logger.error("Error whilst waiting for proxied mock response from queue", ex);
        } finally {
            lock.unlock();
        }

        return null;
    }

    @Override
    public void addResponse(final ProxiedDTO dto) {

        try {

            lock.lock();

            final ProxiedKey key = new ProxiedKey(dto.getPath(), dto.getMethod());
            final List<ProxiedDTO> responses = synchronizedProxyResponsesMap.getOrDefault(key, new ArrayList<ProxiedDTO>());
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

}
