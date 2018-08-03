package com.smockin.mockserver.service;

import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.persistence.dao.RestfulMockDAO;
import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.mockserver.service.dto.SseMessageDTO;
import com.smockin.mockserver.service.dto.PushClientDTO;
import com.smockin.utils.GeneralUtils;
import org.eclipse.jetty.io.RuntimeIOException;
import org.h2.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import spark.Response;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by mgallina
 */
@Service
public class ServerSideEventServiceImpl implements ServerSideEventService {

    private final Logger logger = LoggerFactory.getLogger(ServerSideEventServiceImpl.class);

    private final ConcurrentHashMap<String, ClientSseData> clients = new ConcurrentHashMap<String, ClientSseData>(0);

    private final String messagePrefix = "data: ";
    private final String messageSuffix = "\n\n";

    @Autowired
    private RestfulMockDAO restfulMockDAO;


    @Override
    public void register(final String path, final long heartBeatMillis, final boolean proxyPushIdOnConnect, final Response response) throws IOException {
        logger.debug("register called");

        final String clientId = GeneralUtils.generateUUID();

        applyHeaders(response);

        // Register client and build messages collection
        clients.put(clientId, new ClientSseData(path, Thread.currentThread(), GeneralUtils.getCurrentDate()));

        initHeartBeat(clientId, heartBeatMillis, proxyPushIdOnConnect, response);
    }

    /*
    @Override
    public void clear(final String path) {
        logger.debug("clear called");

        clients.forEach( (key, msgs) -> {
            if (key.getPath().equals(path)) {
                msgs.getMessages().clear();
            }
        });

        throw new NotImplementedException("");
    }
    */

    @Override
    public List<PushClientDTO> getClientConnections(final String mockExtId) throws RecordNotFoundException {

        final RestfulMock mock = restfulMockDAO.findByExtId(mockExtId);

        if (mock == null)
            throw new RecordNotFoundException();

        final String prefixedPath = GeneralUtils.prefixPath(mock.getPath());
        final List<PushClientDTO> sessionIds = new ArrayList<>();

        clients.forEach( (id, data) -> {
            if (data.getPath().equals(prefixedPath)) {
                sessionIds.add(new PushClientDTO(id, data.getDateJoined()));
            }
        });

        return sessionIds;
    }

    @Override
    public void broadcastMessage(final SseMessageDTO dto) {
        logger.debug("broadcastMessage called");

        addMessage(null, dto);
    }

    @Override
    public void addMessage(final String id, final SseMessageDTO dto) {
        logger.debug("addMessage called");

        dto.setBody(GeneralUtils.removeAllLineBreaks(dto.getBody()));

        // Add message to specific client.
        if (id != null) {
            clients.get(id).getMessages().add(dto.getBody());
            return;
        }

        // Add message to all clients associated to this path.
        clients.values().forEach( (data) -> {
            if (data.getPath().equals(dto.getPath())) {
                data.getMessages().add(dto.getBody());
            }
        });

    }

    @Override
    public void interruptAndClearAllHeartBeatThreads() {

        clients.forEach( (key, msgs) -> {
            msgs.getThread().interrupt();
        });
/*
        heartbeatThreadVector.forEach(t -> {
            ((Thread)t).interrupt();
        });

        heartbeatThreadVector.clear();
*/
        clients.clear();
    }

    @Override
    public void clearState() {

        clients.clear();
    }

    void applyHeaders(final Response res) {

       // Set SSE related headers
       res.header("Content-Type","text/event-stream;charset=UTF-8");
       res.header("Cache-Control", "no-cache");

    }

    void initHeartBeat(final String clientId, final long heartBeatMillis, final boolean proxyPushIdOnConnect, final Response response) throws IOException {
        logger.debug("initHeartBeat called");

        // Get raw response Start stream
        final PrintWriter writer = response.raw().getWriter();

        if (proxyPushIdOnConnect) {
            writer.write(messagePrefix + "clientId: " + clientId + messageSuffix);
        }

        while (true) {

            final List<String> messages = clients.get(clientId).getMessages();

            try {

                if (!messages.isEmpty()) {

                    final Iterator<String> messagesIterator = messages.iterator();

                    while (messagesIterator.hasNext()) {
                        writer.write(messagePrefix + messagesIterator.next() + messageSuffix);
                        messagesIterator.remove();
                    }

                } else {
                    writer.write(messagePrefix + messageSuffix); // Empty heartbeat must follow this structure exactly!
                }

                writer.flush();

            } catch (RuntimeIOException ex) {

                if (ex.getMessage().equals("org.eclipse.jetty.io.EofException")) {
                    logger.info("closing SSE connection");

                    writer.close();
                    clients.remove(clientId);

                    break;
                }

                logger.error("Error pushing SSE message", ex);
            }

            try {
                Thread.sleep(heartBeatMillis);
            } catch (InterruptedException ex) {

                if (logger.isDebugEnabled()) {
                    logger.debug("SSE heartbeat thread sleep interrupted", ex);
                }

                break;
            }

        }

    }

    private final class ClientSseData {

        private final String path;
        private final Thread thread;
        private final Date dateJoined;
        private final List<String> messages = new ArrayList<String>(0);

        public ClientSseData(final String path, final Thread thread, final Date dateJoined) {
            this.path = path;
            this.thread = thread;
            this.dateJoined = dateJoined;
        }

        public String getPath() {
            return path;
        }
        public Thread getThread() {
            return thread;
        }
        public Date getDateJoined() {
            return dateJoined;
        }
        public List<String> getMessages() {
            return messages;
        }
    }

}
