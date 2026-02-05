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
import io.javalin.config.Key;
import io.javalin.config.MultipartConfig;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.json.JsonMapper;
import io.javalin.plugin.ContextPlugin;
import io.javalin.router.Endpoint;
import io.javalin.router.Endpoints;
import io.javalin.security.RouteRole;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

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
                session.sendText("No suitable mock found for " + wsPath, Callback.NOOP);
                session.disconnect();
            }
            return;
        }

        final String path = mockedRestServerEngineUtils.buildUserPath(wsMock);

        session.setIdleTimeout(Duration.ofMillis((wsMock.getWebSocketTimeoutInMillis() > 0) ? wsMock.getWebSocketTimeoutInMillis() : MAX_IDLE_TIMEOUT_MILLIS));

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
    public void respondToMessage(final Session session, final String message) throws IOException {
        logger.debug("respondToMessage called, with message {}", message);

        final String wsPath = session.getUpgradeRequest().getRequestURI().getPath();

        if (session.isOpen()) {

            // retrieve the session details
            final String sessionHandshake = session.getUpgradeResponse().getHeader(WS_HAND_SHAKE_KEY);
            Set<SessionIdWrapper> sessions = sessionMap.get(wsPath);

            final RestfulMock wsMock = restfulMockDAO.findActiveByMethodAndPathPatternAndTypesForSingleUser(
                    RestMethodEnum.GET, wsPath, List.of(RestMockTypeEnum.RULE_WS));

            if (wsMock != null && wsMock.getDefinitions().get(0) != null) {

                // If none of the rules match, send the default response body
                RestfulMockDefinitionOrder order = wsMock.getDefinitions().get(0);

                // check if a rules is matched
                boolean ruleMatched = false;
                // Use sMockinRequest to wrap the message as the request body
                Context req = new sMockinRequest(message);
                RestfulResponseDTO response = ruleEngine.process(req, wsMock.getRules());
                if (response != null && response.getResponseBody() != null) {
                    ruleMatched = true;
                    // Only one session should match this key - needs verification
                    for (SessionIdWrapper wrapper : sessions) {
                        if (wrapper.session().getUpgradeResponse().getHeader(WS_HAND_SHAKE_KEY)
                                .equals(sessionHandshake)) {
                            final String id = wrapper.id();
                            sendMessage(id, new WebSocketDTO(wsPath, response.getResponseBody()));
                            break;
                        }
                    }
                }

                if (!ruleMatched && order.getResponseBody() != null) {
                    // Only one session should match this key - needs verification
                    for (SessionIdWrapper wrapper : sessions) {
                        if (wrapper.session().getUpgradeResponse().getHeader(WS_HAND_SHAKE_KEY)
                                .equals(sessionHandshake)) {
                            final String id = wrapper.id();
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

        sessionMap.forEach((key, sessionSet) -> sessionSet.forEach(s -> {
            if (s.session().getUpgradeResponse().getHeader(WS_HAND_SHAKE_KEY).equals(sessionHandshake)) {
                sessionSet.remove(s);
                
                // TODO Need to account for multi users
//                    liveLoggingHandler.broadcast(LiveLoggingUtils.buildLiveLogOutboundDTO(s.getTraceId(), null,null, null, "Websocket closed", false));
                
                return;
            }
        }));

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
            .filter(s -> (s.id().equals(id)))
            .findFirst()
            .ifPresent(s -> {
                s.session().sendText(dto.getBody(),Callback.NOOP);
                // TODO Need to account for multi users
//                    liveLoggingHandler.broadcast(LiveLoggingUtils.buildLiveLogOutboundDTO(s.getTraceId(), null,null, null, dto.getBody(), false));
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
                .forEach(s -> sessionHandshakeIds.add(new PushClientDTO(s.id(), s.dateJoined())));

        return sessionHandshakeIds;
    }

    public String getExternalId(final Session session) {

        final String wsPath = session.getUpgradeRequest().getRequestURI().getPath();
        final String sessionHandshake = session.getUpgradeResponse().getHeader(WS_HAND_SHAKE_KEY);

        for (SessionIdWrapper sw : sessionMap.get(wsPath)) {
            if (sw.session().getUpgradeResponse().getHeader(WS_HAND_SHAKE_KEY).equals(sessionHandshake)) {
                return sw.id();
            }
        }

        return null;
    }
    
    /**
     * Private class used to associate each WebSocket session against an allocated UUID.
     * <p>
     * The UUID is used as an external identifier.
     *
     */
        private record SessionIdWrapper(String id, String traceId, Session session, Date dateJoined) {
        
    }

    public void clearSession() {
        sessionMap.clear();
    }

    // Minimal mock Context for WebSocket messages
    private static class sMockinRequest implements Context {
       private final String body;
        
        private sMockinRequest(String body) {
            this.body = body;
        }
        
        @Override
        public String body() {
            return body;
        }
        
        @Override
        public @NonNull HttpServletRequest req() {
            return null;
        }
        
        @Override
        public @NonNull HttpServletResponse res() {
            return null;
        }
        
        @Override
        public @NonNull Endpoints endpoints() {
            return null;
        }
        
        @Override
        public @NonNull Endpoint endpoint() {
            return null;
        }
        
        @Override
        public <T> T appData(@NonNull Key<T> key) {
            return null;
        }
        
        @Override
        public @NonNull JsonMapper jsonMapper() {
            return null;
        }
        
        @Override
        public <T> T with(@NonNull Class<? extends ContextPlugin<?, T>> aClass) {
            return null;
        }
        
        @Override
        public @NonNull MultipartConfig multipartConfig() {
            return null;
        }
        
        @Override
        public boolean strictContentTypes() {
            return false;
        }
        
        @Override
        public @NonNull String pathParam(@NonNull String s) {
            return "";
        }
        
        @Override
        public @NonNull Map<String, String> pathParamMap() {
            return Map.of();
        }
        
        @Override
        public @NonNull ServletOutputStream outputStream() {
            return null;
        }
        
        @Override
        public @NonNull Context minSizeForCompression(int i) {
            return null;
        }
        
        @Override
        public @NonNull Context result(@NonNull InputStream inputStream) {
            return null;
        }
        
        @Override
        public @Nullable InputStream resultInputStream() {
            return null;
        }
        
        @Override
        public void future(@NonNull Supplier<? extends CompletableFuture<?>> supplier) {
        
        }
        
        @Override
        public void redirect(@NonNull String s, @NonNull HttpStatus httpStatus) {
        
        }
        
        @Override
        public void writeJsonStream(@NonNull Stream<?> stream) {
        
        }
        
        @Override
        public @NonNull Context skipRemainingHandlers() {
            return null;
        }
        
        @Override
        public @NonNull Set<RouteRole> routeRoles() {
            return Set.of();
        }
        // Optionally override other methods if needed for rules
    }
}
