package com.smockin.admin.websocket;

import com.fasterxml.jackson.core.type.TypeReference;
import com.smockin.admin.dto.LiveLoggingAction;
import com.smockin.admin.dto.LiveLoggingBlockedResponseAmendmentDTO;
import com.smockin.admin.dto.response.LiveLoggingDTO;
import com.smockin.admin.enums.UserModeEnum;
import com.smockin.admin.persistence.dao.SmockinUserDAO;
import com.smockin.admin.persistence.enums.SmockinUserRoleEnum;
import com.smockin.admin.service.SmockinUserService;
import com.smockin.mockserver.dto.LiveloggingUserOverrideResponse;
import com.smockin.mockserver.engine.MockedRestServerEngine;
import com.smockin.mockserver.engine.MockedRestServerEngineUtils;
import com.smockin.utils.GeneralUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class LiveLoggingHandlerImpl extends TextWebSocketHandler implements LiveLoggingHandler {

    private final Logger logger = LoggerFactory.getLogger(LiveLoggingHandlerImpl.class);

    private static final String ENABLE_LIVE_LOG_BLOCKING = "ENABLE_LIVE_LOG_BLOCKING";
    private static final String DISABLE_LIVE_LOG_BLOCKING = "DISABLE_LIVE_LOG_BLOCKING";
    private static final String LIVE_LOGGING_AMENDMENT = "LIVE_LOGGING_AMENDMENT";

    private final AtomicReference<List<WebSocketSession>> liveSessionsRef = new AtomicReference<>(new ArrayList<>());


    @Autowired
    private MockedRestServerEngine mockedRestServerEngine;

    @Autowired
    private MockedRestServerEngineUtils mockedRestServerEngineUtils;

    @Autowired
    private SmockinUserService smockinUserService;

    @Autowired
    private SmockinUserDAO smockinUserDAO;


    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {

        liveSessionsRef.get().add(session);
    }

    @Override
    public void afterConnectionClosed(final WebSocketSession session,
                                      final CloseStatus status) throws Exception {

        super.afterConnectionClosed(session, status);

        logger.debug("Live logging WS connection closed");

        liveSessionsRef.get().remove(session);

        stopLiveBlockingMode(session, false);

    }

    @Override
    protected void handleTextMessage(final WebSocketSession session,
                                     final TextMessage message) throws Exception {

        if (message == null || StringUtils.isBlank(message.getPayload())) {
            return;
        }

        final LiveLoggingAction clientAction
                = GeneralUtils.deserialiseJson(message.getPayload(), new TypeReference<LiveLoggingAction<?>>() {});

        if (clientAction == null) {
            return;
        }

        final String type = clientAction.getType();

        if (StringUtils.equals(ENABLE_LIVE_LOG_BLOCKING, type)) {
            mockedRestServerEngine.updateLiveBlockingMode(true);
        } else if (StringUtils.equals(DISABLE_LIVE_LOG_BLOCKING, type)) {
            stopLiveBlockingMode(session, true);
        } else if (StringUtils.equals(LIVE_LOGGING_AMENDMENT, type)) {
            handleLiveLoggingAmendment(message);
        }

    }

    @Override
    public synchronized void broadcast(final LiveLoggingDTO dto) {

        final List<WebSocketSession> sessions = liveSessionsRef.get();

        if (sessions.isEmpty()) {
            return;
        }

        sessions.stream()
                .forEach(s ->
                    handleBroadcast(dto, s));

    }

    private void stopLiveBlockingMode(final WebSocketSession session, final boolean stillConnected) {

        if (!UserModeEnum.ACTIVE.equals(smockinUserService.getUserMode())) {
            mockedRestServerEngine.clearAllPathsFromLiveBlocking();
            mockedRestServerEngine.updateLiveBlockingMode(false);
            return;
        }

        mockedRestServerEngine.clearAllPathsFromLiveBlockingForUser((String)session.getAttributes().get(WS_CONNECTED_USER_ID));

        if ((stillConnected && liveSessionsRef.get().size() == 1)
                || (!stillConnected && liveSessionsRef.get().isEmpty())) {
            mockedRestServerEngine.updateLiveBlockingMode(false);
        }

    }

    private void handleLiveLoggingAmendment(final TextMessage message) {

        final LiveLoggingAction liveLoggingAction
                = GeneralUtils.deserialiseJson(message.getPayload(),
                    new TypeReference<LiveLoggingAction<LiveLoggingBlockedResponseAmendmentDTO>>() {});

        final LiveLoggingBlockedResponseAmendmentDTO amendmentDTO
                = (LiveLoggingBlockedResponseAmendmentDTO)liveLoggingAction.getPayload();

        mockedRestServerEngine.releaseBlockedLiveLoggingResponse(
                amendmentDTO.getTraceId(),
                Optional.of(new LiveloggingUserOverrideResponse(
                                amendmentDTO.getStatus(),
                                amendmentDTO.getHeaders(),
                                amendmentDTO.getBody())));

    }

    private TextMessage serialiseMessage(final LiveLoggingDTO dto) {
        return new TextMessage(GeneralUtils.serialiseJson(dto));
    }

    void handleBroadcast(final LiveLoggingDTO dto, final WebSocketSession session) {

        try {

            // Not in multi user mode, so just send to single user
            if (!UserModeEnum.ACTIVE.equals(smockinUserService.getUserMode())) {
                session.sendMessage(serialiseMessage(dto));
                return;
            }

            //
            // Multi user mode logic...
            final Boolean adminViewAll = (Boolean)session.getAttributes().get(WS_CONNECTED_USER_ADMIN_VIEW_ALL);

            final String inboundPath = dto.getPayload().getContent().getUrl();
            final String userCtxPath = findUserCtxPath(session);

            if (isSysAdmin(session)) {

                if (adminViewAll) {
                    session.sendMessage(serialiseMessage(dto));
                    return;
                }

                final String userCtxPathSegment = mockedRestServerEngineUtils.extractMultiUserCtxPathSegment(inboundPath);

                // TODO
                // This function will eventually move to using a cache, as at the moment we are making a DB call for EVERY SINGLE
                // live logging broadcast where the admin user DOES NOT want to see calls from other users!
                if (!mockedRestServerEngineUtils.isInboundPathMultiUserPath(userCtxPathSegment)) {
                    session.sendMessage(serialiseMessage(dto));
                }

                return;
            }

            if (StringUtils.startsWith(inboundPath, userCtxPath)) {
                session.sendMessage(serialiseMessage(dto));
            }

        } catch (IOException e) {
            logger.error("Error pushing message to connected web socket: " + session.getId(), e);
        }

    }

    private boolean isSysAdmin(final WebSocketSession session) {

        return SmockinUserRoleEnum.SYS_ADMIN.equals(session.getAttributes().get(WS_CONNECTED_USER_ROLE));
    }

    private String findUserCtxPath(final WebSocketSession session) {

        return isSysAdmin(session)
                ? ""
                : GeneralUtils.URL_PATH_SEPARATOR + session.getAttributes().get(WS_CONNECTED_USER_CTX_PATH);
    }

}