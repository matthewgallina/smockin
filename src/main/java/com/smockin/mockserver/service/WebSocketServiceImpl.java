package com.smockin.mockserver.service;

import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.persistence.dao.RestfulMockDAO;
import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.mockserver.service.dto.PushClientDTO;
import com.smockin.mockserver.service.dto.WebSocketDTO;
import com.smockin.utils.GeneralUtils;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by mgallina.
 */
@Service
public class WebSocketServiceImpl implements WebSocketService {

    private final Logger logger = LoggerFactory.getLogger(WebSocketServiceImpl.class);

    private final String WS_HAND_SHAKE_KEY = "Sec-WebSocket-Accept";

    @Autowired
    private RestfulMockDAO restfulMockDAO;

    // TODO Should add TTL and scheduled sweeper to stop the sessionMap from building up.
    // A map of web socket client sessions per simulated web socket path
    private final ConcurrentHashMap<String, Set<SessionIdWrapper>> sessionMap = new ConcurrentHashMap<String, Set<SessionIdWrapper>>();

    /**
     *
     * Stores all websocket client sessions in the internal map 'sessionMap'.
     *
     * Note sessions are 'internally' identified using the encrypted handshake 'Sec-WebSocket-Accept' value and
     * 'externally' identified using an allocated UUID.
     *
     *
     * @param mockExtId
     * @param path
     * @param idleTimeoutMillis
     * @param session
     *
     */
    public void registerSession(final String mockExtId, final String path, final long idleTimeoutMillis, final boolean proxyPushIdOnConnect, final Session session) {
        logger.debug("registerSession called");

        session.setIdleTimeout((idleTimeoutMillis > 0) ? idleTimeoutMillis : MAX_IDLE_TIMEOUT_MILLIS );

        final Set<SessionIdWrapper> sessions = sessionMap.getOrDefault(path, new HashSet<SessionIdWrapper>());

        final String assignedId = GeneralUtils.generateUUID();

        sessions.add(new SessionIdWrapper(assignedId, session, GeneralUtils.getCurrentDate()));

        sessionMap.put(path, sessions);

        if (proxyPushIdOnConnect) {
            try {
                sendMessage(assignedId, new WebSocketDTO(path, "clientId: " + assignedId));
            } catch (IOException ex) {
                logger.error("Error pushing client id to client on 1st connect", ex);
            }
        }

    }

    /**
     *
     * Removes the closing client's session from 'sessionMap' (using the handshake identifier).
     *
     * @param session
     */
    public void removeSession(final Session session) {
        logger.debug("removeSession called");

        final String sessionHandshake = session.getUpgradeResponse().getHeader(WS_HAND_SHAKE_KEY);

        sessionMap.values().forEach( sessionSet -> {

            sessionSet.forEach( s -> {

                if (s.getSession().getUpgradeResponse().getHeader(WS_HAND_SHAKE_KEY).equals(sessionHandshake)) {
                    sessionSet.remove(s);
                    return;
                }

            });

        });

    }

    public void broadcastMessage(final WebSocketDTO dto) throws IOException {
        logger.debug("broadcastMessage called");

        sendMessage(null, dto);
    }

    public void sendMessage(final String id, final WebSocketDTO dto) throws IOException {
        logger.debug("sendMessage called");

        final Set<SessionIdWrapper> sessions = sessionMap.get(dto.getPath());

        if (sessions == null) {
            return;
        }

        if (id != null) {

            // Push to specific client session for the given handshake id
            for (SessionIdWrapper s : sessions) {

                if (s.getId().equals(id)) {
                    s.getSession().getRemote().sendString(dto.getBody());
                    return;
                }

            }

        } else {

            // Broadcast to all session on this path if not id is specified
            for (SessionIdWrapper s : sessions) {
                s.getSession().getRemote().sendString(dto.getBody());
            }

        }

    }

    public List<PushClientDTO> getClientConnections(final String mockExtId) throws RecordNotFoundException {

        final RestfulMock mock = restfulMockDAO.findByExtId(mockExtId);

        if (mock == null)
            throw new RecordNotFoundException();

        final String prefixedPath = GeneralUtils.prefixPath(mock.getPath());
        final List<PushClientDTO> sessionHandshakeIds = new ArrayList<PushClientDTO>();

        if (!sessionMap.containsKey(prefixedPath)) {
            return sessionHandshakeIds;
        }

        sessionMap.get(prefixedPath).forEach( s -> {
            sessionHandshakeIds.add(new PushClientDTO(s.getId(), s.getDateJoined()));
        });

        return sessionHandshakeIds;
    }

    public String getExternalId(final String path, final Session session) {

        final String sessionHandshake = session.getUpgradeResponse().getHeader(WS_HAND_SHAKE_KEY);

        for (SessionIdWrapper sw : sessionMap.get(path)) {
            if (sw.getSession().getUpgradeResponse().getHeader(WS_HAND_SHAKE_KEY).equals(sessionHandshake)) {
                return sw.getId();
            }
        }

        return null;
    }

    /**
     * Private class used to associate each WebSocket session against an allocated UUID.
     *
     * The UUID is used as an external identifier.
     *
     */
    private final class SessionIdWrapper {

        private final String id;
        private final Session session;
        private final Date dateJoined;

        public SessionIdWrapper(final String id, final Session session, final Date dateJoined) {
            this.id = id;
            this.session = session;
            this.dateJoined = dateJoined;
        }

        public String getId() {
            return id;
        }
        public Session getSession() {
            return session;
        }
        public Date getDateJoined() {
            return dateJoined;
        }
    }

    public void clearSession() {
        sessionMap.clear();
    }

}
