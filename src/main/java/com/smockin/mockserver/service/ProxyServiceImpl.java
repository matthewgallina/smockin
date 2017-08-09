package com.smockin.mockserver.service;

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
    private Map<String, List<String>> responsesMap = new HashMap<String, List<String>>();

    @Override
    public RestfulResponse waitForResponse(final String path) {

        try {

            lock.acquire();

            final List<String> responses = responsesMap.get(path);

            if (responses == null || responses.isEmpty()) {

            }

            final String proxiedResponseBody = responses.get(0);

            lock.release();

            return new RestfulResponse(200, "application/json", proxiedResponseBody, new HashSet<Map.Entry<String, String>>());
        } catch (InterruptedException ex) {
            logger.error("Error whilst waiting for mock proxied response", ex);
        }

        return null;
    }

    @Override
    public void addResponse(final String path, final String responseBody) {

        try {

            lock.acquire();

            final List<String> responses = responsesMap.getOrDefault(path, new ArrayList<String>());
            responses.add(responseBody);
            responsesMap.put(path, responses); // not sure we need this line...

            lock.release();

        } catch (InterruptedException ex) {
            logger.error("Error whilst adding response to map for proxied responses", ex);
        }

    }

}
