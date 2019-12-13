package com.smockin.mockserver.service.ws;

import com.smockin.mockserver.service.WebSocketService;
import com.smockin.mockserver.service.enums.WebSocketCommandEnum;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import java.io.IOException;

/**
 * Created by mgallina.
 */
@WebSocket
public class SparkWebSocketEchoService {

    private final WebSocketService webSocketService;
    private boolean isMultiUserMode;

    public SparkWebSocketEchoService(final WebSocketService webSocketService, final boolean isMultiUserMode) {
        this.webSocketService = webSocketService;
        this.isMultiUserMode = isMultiUserMode;
    }

    @OnWebSocketConnect
    public void connected(final Session session) {
        webSocketService.registerSession(session, isMultiUserMode);
    }

    @OnWebSocketClose
    public void closed(final Session session, final int statusCode, final String reason) {
        webSocketService.removeSession(session);
    }

    @OnWebSocketMessage
    public void message(final Session session, final String message) throws IOException {

        if (WebSocketCommandEnum.SMOCKIN_ID.name().equals(message)) {
            session.getRemote().sendString(webSocketService.getExternalId(session));
            return;
        }

        // Ignore all other inbound messages from client
    }

}
