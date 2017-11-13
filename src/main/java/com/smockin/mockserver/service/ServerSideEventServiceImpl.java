package com.smockin.mockserver.service;

import com.smockin.mockserver.service.bean.SseClientKey;
import com.smockin.mockserver.service.dto.SseMessageDTO;
import com.smockin.utils.GeneralUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.eclipse.jetty.io.RuntimeIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import spark.Response;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by mgallina
 */
@Service
public class ServerSideEventServiceImpl implements ServerSideEventService {

    private final Logger logger = LoggerFactory.getLogger(ServerSideEventServiceImpl.class);

    private final ConcurrentHashMap<SseClientKey, List<String>> clients = new ConcurrentHashMap<SseClientKey, List<String>>();

    private final String messagePrefix = "data: ";
    private final String messageSuffix = "\n\n";


    @Override
    public void registerClient(final String path, final long heartBeatMillis, final Response response) throws IOException {
        logger.debug("registerClient called");

        final SseClientKey clientKey = new SseClientKey(GeneralUtils.generateUUID(), path);

        if (clients.containsKey(clientKey)) {
            logger.debug("client is already registered");
            return;
        }

        applyHeaders(response);

        // Register client and build messages collection
        clients.put(clientKey, new ArrayList<String>(0));

        initHeartBeat(clientKey, heartBeatMillis, response);
    }

    @Override
    public void clear(final String path) {
        logger.debug("clear called");

        clients.forEach( (key, msgs) -> {
            if (key.getPath().equals(path)) {
                msgs.clear();
            }
        });

        throw new NotImplementedException("");
    }

    @Override
    public void addMessage(final SseMessageDTO dto) {
        logger.debug("addMessage called");

        // Add message to all clients associated to this path.
        clients.forEach( (key, msgs) -> {
            if (key.getPath().equals(dto.getPath())) {
                msgs.add(dto.getBody());
            }
        });

    }


    void applyHeaders(final Response res) {

       // Set SSE related headers
       res.header("Content-Type","text/event-stream;charset=UTF-8");
       res.header("Cache-Control", "no-cache");

    }

    void initHeartBeat(final SseClientKey key, final long heartBeatMillis, final Response response) throws IOException {
        logger.debug("initHeartBeat called");

        // Get raw response Start stream
        final PrintWriter writer = response.raw().getWriter();

        while (true) {

            final List<String> messages = clients.get(key);

            try {

                if (!messages.isEmpty()) {

                    final Iterator<String> messagesIterator = messages.iterator();

                    while (messagesIterator.hasNext()) {
                        writer.println(messagePrefix + messagesIterator.next() + messageSuffix);
                        messagesIterator.remove();
                    }

                } else {
                    writer.println(messagePrefix + messageSuffix); // Empty heartbeat must follow this structure exactly!
                }

                writer.flush();

            } catch (RuntimeIOException ex) {

                if (ex.getMessage().equals("org.eclipse.jetty.io.EofException")) {
                    logger.debug("closing SSE connection");

                    writer.close();
                    clients.remove(key);

                    break;
                }

                logger.error("Error pushing SSE message", ex);
            }

            try {
                Thread.sleep(heartBeatMillis);
            } catch (InterruptedException ex) {
                logger.error("Error pausing SSE heartbeat thread", ex);
            }

        }

    }

}
