package com.smockin.mockserver.service;

import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.RestfulMockDAO;
import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.persistence.entity.RestfulMockDefinitionOrder;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.admin.persistence.enums.RestMockTypeEnum;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import com.smockin.mockserver.engine.MockedRestServerEngineUtils;
import com.smockin.mockserver.exception.MockServerException;
import com.smockin.mockserver.service.dto.PushClientDTO;
import com.smockin.mockserver.service.dto.RestfulResponseDTO;
import com.smockin.mockserver.service.dto.WebSocketDTO;
import com.smockin.utils.GeneralUtils;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spark.Request;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by mgallina.
 */
@Service
@Transactional
public class WebSocketServiceImpl implements WebSocketService {

    private final Logger logger = LoggerFactory.getLogger(WebSocketServiceImpl.class);

    private final String WS_HAND_SHAKE_KEY = "Sec-WebSocket-Accept";

    @Autowired
    private RestfulMockDAO restfulMockDAO;

    @Autowired
    private MockedRestServerEngineUtils mockedRestServerEngineUtils;

    @Autowired
    private UserTokenServiceUtils userTokenServiceUtils;

//    @Autowired
//    private LiveLoggingHandler liveLoggingHandler;

    @Autowired
    private RuleEngine ruleEngine;


    // TODO Should add TTL and scheduled sweeper to stop the sessionMap from building up.
    // A map of web socket client sessions per simulated web socket path
    private final ConcurrentHashMap<String, Set<SessionIdWrapper>> sessionMap = new ConcurrentHashMap<>();

    /**
     *
     * Stores all websocket client sessions in the internal map 'sessionMap'.
     *
     * Note sessions are 'internally' identified using the encrypted handshake 'Sec-WebSocket-Accept' value and
     * 'externally' identified using an allocated UUID.
     *
     */
    public void registerSession(final Session session, final boolean isMultiUserMode) {
        logger.debug("registerSession called");

        final String wsPath = session.getUpgradeRequest().getRequestURI().getPath();
        final RestfulMock wsMock = (isMultiUserMode)
                ? restfulMockDAO.findActiveByMethodAndPathPatternAndTypesForMultiUser(
                        RestMethodEnum.GET, wsPath, Arrays.asList(RestMockTypeEnum.PROXY_WS, RestMockTypeEnum.RULE_WS))
                : restfulMockDAO.findActiveByMethodAndPathPatternAndTypesForSingleUser(
                        RestMethodEnum.GET, wsPath, Arrays.asList(RestMockTypeEnum.PROXY_WS, RestMockTypeEnum.RULE_WS));

        if (wsMock == null) {
            if (session.isOpen()) {
                try {
                    session.getRemote().sendString("No suitable mock found for " + wsPath);
                    session.disconnect();
                } catch(IOException e) {
                    logger.error("Error closing non mock matching web socket client connection", e);
                }
            }
            return;
        }

        final String path = mockedRestServerEngineUtils.buildUserPath(wsMock);

        session.setIdleTimeout((wsMock.getWebSocketTimeoutInMillis() > 0) ? wsMock.getWebSocketTimeoutInMillis() : MAX_IDLE_TIMEOUT_MILLIS);

        final Set<SessionIdWrapper> sessions = sessionMap.computeIfAbsent(path, k -> new HashSet<>());
        final String assignedId = GeneralUtils.generateUUID();
        final String traceId = session.getUpgradeResponse().getHeader(GeneralUtils.LOG_REQ_ID);

        sessionMap.merge(
                path, 
                sessions, 
                (k, v) -> {
                    v.add(new SessionIdWrapper(assignedId, traceId, session, GeneralUtils.getCurrentDate()));
                    return v;
                });

        if (wsMock.isProxyPushIdOnConnect()) {
            sendMessage(assignedId, new WebSocketDTO(path, "clientId: " + assignedId));
        }

        if (wsMock.getMockType() == RestMockTypeEnum.RULE_WS && wsMock.getDefinitions().get(0) != null) {
            RestfulMockDefinitionOrder order = wsMock.getDefinitions().get(0);
            if (order.getResponseBody() != null) {
                sendMessage(assignedId, new WebSocketDTO(path, order.getResponseBody()));
            }

        }

        // TODO Need to account for multi users
//        liveLoggingHandler.broadcast(LiveLoggingUtils.buildLiveLogOutboundDTO(traceId, null, 101, null,
//                "Websocket established (clientId: " + assignedId + ")", false));

    }
    
    /**
     * Only respond to RULE_WS websocket requests.
     * 
     * @param session
     * @param message
     */
    public void respondToMessage(final Session session, final String message) {
        logger.debug("respondToMessage called, with message {}", message);

        final String wsPath = session.getUpgradeRequest().getRequestURI().getPath();

        if (session.isOpen()) {

            // retrieve the session details
            final String sessionHandshake = session.getUpgradeResponse().getHeader(WS_HAND_SHAKE_KEY);
            Set<SessionIdWrapper> sessions = sessionMap.get(wsPath);

            final RestfulMock wsMock = restfulMockDAO.findActiveByMethodAndPathPatternAndTypesForSingleUser(
                    RestMethodEnum.GET, wsPath, Arrays.asList(RestMockTypeEnum.RULE_WS));

            if (wsMock != null && wsMock.getDefinitions().get(0) != null) {

                // If none of the rules match, send the default response body
                RestfulMockDefinitionOrder order = wsMock.getDefinitions().get(0);

                // check if a rules is matched
                boolean ruleMatched = false;
                Request req = new sMockinRequest(message);
                RestfulResponseDTO response = ruleEngine.process(req, wsMock.getRules());
                if (response != null && response.getResponseBody() != null) {
                    ruleMatched = true;
                    // Only one session should match this key - needs verification
                    for (SessionIdWrapper wrapper : sessions) {
                        if (wrapper.getSession().getUpgradeResponse().getHeader(WS_HAND_SHAKE_KEY)
                                .equals(sessionHandshake)) {
                            final String id = wrapper.getId();
                            sendMessage(id, new WebSocketDTO(wsPath, response.getResponseBody()));
                            break;
                        }
                    }
                }

                if (!ruleMatched && order.getResponseBody() != null) {
                    // Only one session should match this key - needs verification
                    for (SessionIdWrapper wrapper : sessions) {
                        if (wrapper.getSession().getUpgradeResponse().getHeader(WS_HAND_SHAKE_KEY)
                                .equals(sessionHandshake)) {
                            final String id = wrapper.getId();
                            sendMessage(id, new WebSocketDTO(wsPath, order.getResponseBody()));
                            break;
                        }
                    }
                }
            }

        } else {
            logger.info("Session for path {} is not open", wsPath);
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

        sessionMap.entrySet().forEach(entry -> {
            Set<SessionIdWrapper> sessionSet = entry.getValue();
            sessionSet.forEach( s -> {
                if (s.getSession().getUpgradeResponse().getHeader(WS_HAND_SHAKE_KEY).equals(sessionHandshake)) {
                    sessionSet.remove(s);

                    // TODO Need to account for multi users
//                    liveLoggingHandler.broadcast(LiveLoggingUtils.buildLiveLogOutboundDTO(s.getTraceId(), null,null, null, "Websocket closed", false));

                    return;
                }
            });
        });

    }

    public void sendMessage(final String id, final WebSocketDTO dto) throws MockServerException {
        logger.debug("sendMessage called");

        dto.setBody(GeneralUtils.removeAllLineBreaks(dto.getBody()));

        final Set<SessionIdWrapper> sessions = sessionMap.get(dto.getPath());

        if (sessions == null) {
            return;
        }

        // Push to specific client session for the given handshake id
        sessions.stream()
            .filter(s -> (s.getId().equals(id)))
            .findFirst()
            .ifPresent(s -> {
                try {
                    s.getSession().getRemote().sendString(dto.getBody());
                    // TODO Need to account for multi users
//                    liveLoggingHandler.broadcast(LiveLoggingUtils.buildLiveLogOutboundDTO(s.getTraceId(), null,null, null, dto.getBody(), false));
                } catch (IOException e) {
                    throw new MockServerException(e);
                }
            });

    }

    public List<PushClientDTO> getClientConnections(final String mockExtId, final String token) throws RecordNotFoundException, ValidationException {

        final RestfulMock mock = restfulMockDAO.findByExtId(mockExtId);

        if (mock == null)
            throw new RecordNotFoundException();

        userTokenServiceUtils.validateRecordOwner(mock.getCreatedBy(), token);

        final String prefixedPath = mockedRestServerEngineUtils.buildUserPath(mock);
        final List<PushClientDTO> sessionHandshakeIds = new ArrayList<>();

        if (!sessionMap.containsKey(prefixedPath)) {
            return sessionHandshakeIds;
        }

        sessionMap.get(prefixedPath)
                .forEach(s -> sessionHandshakeIds.add(new PushClientDTO(s.getId(), s.getDateJoined())));

        return sessionHandshakeIds;
    }

    public String getExternalId(final Session session) {

        final String wsPath = session.getUpgradeRequest().getRequestURI().getPath();
        final String sessionHandshake = session.getUpgradeResponse().getHeader(WS_HAND_SHAKE_KEY);

        for (SessionIdWrapper sw : sessionMap.get(wsPath)) {
            if (sw.getSession().getUpgradeResponse().getHeader(WS_HAND_SHAKE_KEY).equals(sessionHandshake)) {
                return sw.getId();
            }
        }

        return null;
    }

    private class sMockinRequest extends Request {
    	String body;
    	
    	sMockinRequest(String value) {
    		body = value;
    	}
    	
    	@Override
    	public String body() {
    		return body;
    	}
    	
    }

    /**
     * Private class used to associate each WebSocket session against an allocated UUID.
     *
     * The UUID is used as an external identifier.
     *
     */
    private final class SessionIdWrapper {

        private final String id;
        private final String traceId;
        private final Session session;
        private final Date dateJoined;

        public SessionIdWrapper(final String id, final String traceId, final Session session, final Date dateJoined) {
            this.id = id;
            this.traceId = traceId;
            this.session = session;
            this.dateJoined = dateJoined;
        }

        public String getId() {
            return id;
        }
        public String getTraceId() {
            return traceId;
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
