package com.smockin.admin.websocket;

import com.fasterxml.jackson.core.type.TypeReference;
import com.smockin.admin.dto.LiveLoggingAction;
import com.smockin.admin.dto.LiveLoggingBlockedResponseAmendmentDTO;
import com.smockin.admin.dto.response.LiveLoggingDTO;
import com.smockin.mockserver.dto.LiveloggingUserOverrideResponse;
import com.smockin.mockserver.engine.MockedRestServerEngine;
import com.smockin.utils.GeneralUtils;
import org.h2.util.StringUtils;
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
    private static final String LIVE_LOGGING_AMENDMENT_CANCEL = "LIVE_LOGGING_AMENDMENT_CANCEL";

    private final AtomicReference<List<WebSocketSession>> liveSessionsRef = new AtomicReference<>(new ArrayList<>());


    @Autowired
    private MockedRestServerEngine mockedRestServerEngine;


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

        stopLiveBlockingMode();

    }

    @Override
    protected void handleTextMessage(final WebSocketSession session,
                                     final TextMessage message) throws Exception {

        final LiveLoggingAction clientAction
                = GeneralUtils.deserialiseJson(message.getPayload(), new TypeReference<LiveLoggingAction<?>>() {});

        if (clientAction == null) {
            return;
        }

        final String type = clientAction.getType();

        if (StringUtils.equals(ENABLE_LIVE_LOG_BLOCKING, type)) {
            mockedRestServerEngine.updateLiveBlockingMode(true);
        } else if (StringUtils.equals(DISABLE_LIVE_LOG_BLOCKING, type)) {
            stopLiveBlockingMode();
        } else if (StringUtils.equals(LIVE_LOGGING_AMENDMENT, type)) {
            handleLiveLoggingAmendment(message);
        } else if (StringUtils.equals(LIVE_LOGGING_AMENDMENT_CANCEL, type)) {
            clearLiveBlockingMode();
        }

    }

    @Override
    public synchronized void broadcast(final LiveLoggingDTO dto) {

        final List<WebSocketSession> sessions = liveSessionsRef.get();

        if (sessions.isEmpty()) {
            return;
        }

        sessions.stream()
                .forEach(s -> {
                    try {
                        s.sendMessage(serialiseMessage(dto));
                    } catch (IOException e) {
                        logger.error("Error pushing message to connected web socket: " + s.getId(), e);
                    }
                });

    }

    private void stopLiveBlockingMode() {

        clearLiveBlockingMode();
        mockedRestServerEngine.updateLiveBlockingMode(false);
    }

    private void clearLiveBlockingMode() {

        mockedRestServerEngine.releaseBlockedLiveLoggingResponse(Optional.empty());
        mockedRestServerEngine.clearAllPathsFromLiveBlocking();
    }

    private void handleLiveLoggingAmendment(final TextMessage message) {

        final LiveLoggingAction liveLoggingAction
                = GeneralUtils.deserialiseJson(message.getPayload(),
                    new TypeReference<LiveLoggingAction<LiveLoggingBlockedResponseAmendmentDTO>>() {});

        final LiveLoggingBlockedResponseAmendmentDTO amendmentDTO
                = (LiveLoggingBlockedResponseAmendmentDTO)liveLoggingAction.getPayload();

        mockedRestServerEngine.releaseBlockedLiveLoggingResponse(
                Optional.of(new LiveloggingUserOverrideResponse(
                                amendmentDTO.getStatus(),
                                amendmentDTO.getContentType(),
                                amendmentDTO.getHeaders(),
                                amendmentDTO.getBody())));

    }

    private TextMessage serialiseMessage(final LiveLoggingDTO dto) {
        return new TextMessage(GeneralUtils.serialiseJson(dto));
    }

}