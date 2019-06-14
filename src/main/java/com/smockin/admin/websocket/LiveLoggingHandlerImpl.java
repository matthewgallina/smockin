package com.smockin.admin.websocket;

import com.smockin.admin.dto.response.LiveLoggingDTO;
import com.smockin.utils.GeneralUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class LiveLoggingHandlerImpl extends TextWebSocketHandler implements LiveLoggingHandler {

    private final Logger logger = LoggerFactory.getLogger(LiveLoggingHandlerImpl.class);
    private final AtomicReference<List<WebSocketSession>> liveSessionsRef = new AtomicReference<>(new ArrayList<>());

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        liveSessionsRef.get().add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);

        liveSessionsRef.get().remove(session);
    }

    @Override
    public synchronized void broadcast(final LiveLoggingDTO dto) {
        final List<WebSocketSession> sessions = liveSessionsRef.get();

        if (sessions.isEmpty()) {
            return;
        }

        sessions.stream().forEach(s -> {
            try {
                s.sendMessage(serialiseMessage(dto));
            } catch (IOException e) {
                logger.error("Error pushing message to connected web socket: " + s.getId(), e);
            }
        });

    }

    private TextMessage serialiseMessage(final LiveLoggingDTO dto) {
        return new TextMessage(GeneralUtils.serialiseJson(dto));
    }

}