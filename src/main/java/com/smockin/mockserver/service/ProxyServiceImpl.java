package com.smockin.mockserver.service;

import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.mockserver.service.bean.ProxiedKey;
import com.smockin.mockserver.service.dto.ProxiedDTO;
import com.smockin.mockserver.service.dto.RestfulResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;


/**
 * Created by mgallina on 09/08/17.
 */
@Service
public class ProxyServiceImpl implements ProxyService {

    private final Logger logger = LoggerFactory.getLogger(ProxyServiceImpl.class);

    private Map<ProxiedKey, List<ProxiedDTO>> synchronizedProxyResponsesMap = new HashMap<ProxiedKey, List<ProxiedDTO>>();

    private final int SIGNAL_PERMIT_COUNT = 1;
    private final int MAX_TIMEOUT_MILLIS = 30000;

    private final Semaphore signal = new Semaphore(0, true);
    private final Semaphore lock = new Semaphore(1, true);

    @Override
    public RestfulResponse waitForResponse(final RestfulMock mock) {

        final long timeOut = (mock.getProxyTimeOutInMillis() > 0)?mock.getProxyTimeOutInMillis():MAX_TIMEOUT_MILLIS;

        try {

            while (true) {

                // Checks for any existing permits, if none are found then waits (for a set amount of time) until one is available.
                boolean foundOne = signal.tryAcquire(SIGNAL_PERMIT_COUNT, timeOut, TimeUnit.MILLISECONDS);

                if (!foundOne) {

                    // The wait has timed out
                    if (logger.isDebugEnabled()) {
                        logger.debug("The wait for '" + mock.getMethod() + " " + mock.getPath() + "' has timed out");
                        logger.debug("Will attempt 1 more look up, in case ");
                    }

                    // HACK to fix the design issue.
                    // We use the timeout as a way of checking again for a relevant response.
                    return lookUpResponse(mock);
                }

                final RestfulResponse restfulResponse = lookUpResponse(mock);

                if (restfulResponse == null) {
                    // Design flaw!

                    /*
                        You are using a semaphore as part of a consumer / producer model which is like a Queue.

                        What you really need is MQ 'Topic' type behaviour, given the consumer is looking for a
                        'particular' RestfulResponse object.


                        The problem is to do with signalling alignment and described below :

                        Consumer 'Thread A' is waiting for a permit and wants the value '1'.

                        Consumer 'Thread B' is waiting for a permit and wants the value '2'.

                        Producer 'Thread C' posts the value '2' and stores this (in the synchronizedProxyResponsesMap).

                        Consumer 'Thread A' acquires a permit, but cannot find the value '1' and so continues (in the loop) waiting on another permit.

                        Producer 'Thread D' posts the value '1'.

                        Consumer 'Thread B' acquires a permit and finds the value '1', it exits and loop and returns a response.

                        Consumer 'Thread A' is now stuck waiting on a permit, despite value '2', being in the synchronizedProxyResponsesMap.


                        Using a topic approach would fix this as 'all' of the consumers would be notified each time a producer stores a value.
                    */
                    continue;
                }

                return restfulResponse;
            }

        } catch (InterruptedException ex) {
            logger.error("Error whilst waiting for proxied mock response from queue", ex);
        }

        return null;
    }

    @Override
    public void addResponse(final ProxiedDTO dto) {

        try {
            lock.acquire();
            final ProxiedKey key = new ProxiedKey(dto.getPath(), dto.getMethod());
            final List<ProxiedDTO> responses = synchronizedProxyResponsesMap.getOrDefault(key, new ArrayList<ProxiedDTO>());
            responses.add(dto);
            synchronizedProxyResponsesMap.put(key, responses); // not sure we need this line...
            lock.release();

            if (logger.isDebugEnabled())
                logger.debug("Added dto " + dto.getPath() + ". Responses size is " + responses.size());

            signal.release(SIGNAL_PERMIT_COUNT);

        } catch (Throwable ex) {
            logger.error("Error whilst adding proxied response to queue", ex);
        }

    }

    RestfulResponse lookUpResponse(final RestfulMock mock) throws InterruptedException {

        // Acquire lock before checking queue
        lock.acquire();

        final List<ProxiedDTO> responses = synchronizedProxyResponsesMap.get(new ProxiedKey(mock.getPath(), mock.getMethod()));

        if (responses == null || responses.isEmpty()) {

            // no matching path was found, so release lock and continue waiting
            lock.release();

            return null;
        }

        // A matching path was found, so consume/remove the element from the queue, release the lock and return response.
        final ProxiedDTO proxiedResponse = responses.remove(0);

        lock.release();

        return new RestfulResponse(proxiedResponse.getHttpStatusCode(), proxiedResponse.getResponseContentType(), proxiedResponse.getBody(), new HashSet<Map.Entry<String, String>>());
    }

}
