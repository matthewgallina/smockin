package com.smockin.mockserver.service;

import com.smockin.mockserver.service.dto.WebSocketClientDTO;
import com.smockin.mockserver.service.dto.WebSocketDTO;
import com.smockin.utils.GeneralUtils;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by mgallina.
 */
@Service
public class WebSocketServiceImpl implements WebSocketService {

    private final Logger logger = LoggerFactory.getLogger(WebSocketServiceImpl.class);

    private final String WS_HAND_SHAKE_KEY = "Sec-WebSocket-Accept";

    // A map of web socket client sessions per simulated web socket path
    private final ConcurrentHashMap<String, Set<SessionIdWrapper>> sessionMap = new ConcurrentHashMap<String, Set<SessionIdWrapper>>();

    /**
     *
     * Stores all websocket client sessions in the internal map 'sessionMap'.
     *
     * Note sessions are 'internally' identified using the encrypted handshake 'Sec-WebSocket-Accept' value and
     * 'externally' identified using an allocated UUID.
     *
     * @param path
     * @param session
     */
    public void registerSession(final String path, final Session session) {
        logger.debug("registerSession called");

        final Set<SessionIdWrapper> sessions = sessionMap.getOrDefault(path, new HashSet<SessionIdWrapper>());

        sessions.add(new SessionIdWrapper(GeneralUtils.generateUUID(), session));

        sessionMap.put(path, sessions);
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

    public List<WebSocketClientDTO> getClientConnections(final String path) {

        final String prefixedPath = GeneralUtils.prefixPath(path);
        final List<WebSocketClientDTO> sessionHandshakeIds = new ArrayList<WebSocketClientDTO>();

        if (!sessionMap.containsKey(prefixedPath)) {
            return sessionHandshakeIds;
        }

        sessionMap.get(prefixedPath).forEach( s -> {
            sessionHandshakeIds.add(new WebSocketClientDTO(s.getId()));
        });

        return sessionHandshakeIds;
    }

    /**
     * Private class used to associate each WebSocket session against an allocated UUID.
     *
     * The UUID is used as an external identifier.
     *
     */
    class SessionIdWrapper {

        private final String id;
        private final Session session;

        public SessionIdWrapper(final String id, final Session session) {
            this.id = id;
            this.session = session;
        }

        public String getId() {
            return id;
        }
        public Session getSession() {
            return session;
        }

    }

}
