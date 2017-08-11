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

    private Map<ProxiedKey, List<ProxiedDTO>> responsesQueueMap = new HashMap<ProxiedKey, List<ProxiedDTO>>();

    private final int signalPermitCount = 1;
    private final Semaphore signal = new Semaphore(0);
    private final Semaphore lock = new Semaphore(1);

    @Override
    public RestfulResponse waitForResponse(final RestfulMock mock) {

        final long timeOut = (mock.getProxyTimeOutInMillis() > 0)?mock.getProxyTimeOutInMillis():Long.MAX_VALUE;

        try {

            while (true) {

                // Checks for any existing permits, if none are found then waits (for a set amount of time) until one is available.
                boolean foundOne = signal.tryAcquire(signalPermitCount, timeOut, TimeUnit.MILLISECONDS);

                if (!foundOne) {
                    // timed out
                    return null;
                }

                // Acquire lock before checking queue
                lock.acquire();

                final List<ProxiedDTO> responses = responsesQueueMap.get(new ProxiedKey(mock.getPath(), mock.getMethod()));

                if (responses == null || responses.isEmpty()) {

                    // no matching path was found, so release lock and continue waiting
                    lock.release();

                    continue;
                } else {

                    // A matching path was found, so consume/remove the element from the queue, release the lock and return.
                    final ProxiedDTO proxiedResponse = responses.remove(0);

                    lock.release();

                    return new RestfulResponse(proxiedResponse.getHttpStatusCode(), proxiedResponse.getResponseContentType(), proxiedResponse.getBody(), new HashSet<Map.Entry<String, String>>());
                }

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
            final List<ProxiedDTO> responses = responsesQueueMap.getOrDefault(dto.getPath(), new ArrayList<ProxiedDTO>());
            responses.add(dto);
            responsesQueueMap.put(new ProxiedKey(dto.getPath(), dto.getMethod()), responses); // not sure we need this line...
            lock.release();

            signal.release(signalPermitCount);

        } catch (InterruptedException ex) {
            logger.error("Error whilst adding proxied response to queue", ex);
        }

    }

}
