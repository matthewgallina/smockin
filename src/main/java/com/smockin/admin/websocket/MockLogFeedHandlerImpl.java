package com.smockin.admin.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smockin.admin.dto.response.MockClientCallLogDTO;
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
import java.util.concurrent.atomic.AtomicReference;

@Component
public class MockLogFeedHandlerImpl extends TextWebSocketHandler implements MockLogFeedHandler {

    private final Logger logger = LoggerFactory.getLogger(MockLogFeedHandlerImpl.class);
    private final AtomicReference<List<WebSocketSession>> atomRef = new AtomicReference<>(new ArrayList<>());

    @Autowired
    private ObjectMapper jsonMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        atomRef.get().add(session);

        session.sendMessage(buildOutboundMessage("Starting log feed..."));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);

        atomRef.get().remove(session);
    }

    public void broadcast(final String msg) {

        final List<WebSocketSession> sessions = atomRef.get();

        if (sessions.isEmpty()) {
            return;
        }

        sessions.stream().forEach(s -> {
            try {
                s.sendMessage(buildOutboundMessage(msg));
            } catch (IOException e) {
                logger.error("Error pushing message to connected web socket: " + s.getId(), e);
            }
        });

    }

    private TextMessage buildOutboundMessage(final String msg) throws JsonProcessingException {
        return new TextMessage(jsonMapper.writeValueAsString(new MockClientCallLogDTO(msg)));
    }

}