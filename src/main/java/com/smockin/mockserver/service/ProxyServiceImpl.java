package com.smockin.mockserver.service;

import com.smockin.admin.dto.ProxiedDTO;
import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.mockserver.service.dto.RestfulResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import spark.Request;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;


/**
 * Created by mgallina on 09/08/17.
 */
@Service
public class ProxyServiceImpl implements ProxyService {

    private final Logger logger = LoggerFactory.getLogger(ProxyServiceImpl.class);

    private final Semaphore lock = new Semaphore(1);
    private Map<String, List<ProxiedDTO>> responsesQueueMap = new HashMap<String, List<ProxiedDTO>>();

    @Override
    public RestfulResponse waitForResponse(final String path) {

        try {

            lock.acquire();

            final List<ProxiedDTO> responses = responsesQueueMap.get(path);

            if (responses == null || responses.isEmpty()) {
                // TODO wait
            } else {
                final ProxiedDTO proxiedResponse = responses.get(0);
                lock.release();
                return new RestfulResponse(proxiedResponse.getHttpStatusCode(), proxiedResponse.getResponseContentType(), proxiedResponse.getBody(), new HashSet<Map.Entry<String, String>>());
            }

        } catch (InterruptedException ex) {
            logger.error("Error whilst waiting for mock proxied response", ex);
        }

        return null;
    }

    @Override
    public void addResponse(final ProxiedDTO dto) {

        try {

            lock.acquire();

            final List<ProxiedDTO> responses = responsesQueueMap.getOrDefault(dto.getPath(), new ArrayList<ProxiedDTO>());
            responses.add(dto);
            responsesQueueMap.put(dto.getPath(), responses); // not sure we need this line...

            lock.release();

        } catch (InterruptedException ex) {
            logger.error("Error whilst adding response to map for proxied responses", ex);
        }

    }

}
