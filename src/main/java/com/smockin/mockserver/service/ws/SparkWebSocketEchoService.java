package com.smockin.mockserver.service.ws;

/**
 * Created by mgallina on 31/08/17.
 */
import com.smockin.mockserver.service.WebSocketService;
import com.smockin.mockserver.service.enums.WebSocketCommandEnum;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by mgallina.
 */
@WebSocket
public class SparkWebSocketEchoService {

    private final String path;
    private final long idleTimeoutMillis;
    private final WebSocketService webSocketService;

    public SparkWebSocketEchoService(final String path, final long idleTimeoutMillis, final WebSocketService webSocketService) {
        this.path = path;
        this.idleTimeoutMillis = idleTimeoutMillis;
        this.webSocketService = webSocketService;
    }

    @OnWebSocketConnect
    public void connected(final Session session) {

        webSocketService.registerSession(path, idleTimeoutMillis, session);
    }

    @OnWebSocketClose
    public void closed(final Session session, final int statusCode, final String reason) {

        webSocketService.removeSession(session);
    }

    @OnWebSocketMessage
    public void message(final Session session, final String message) throws IOException {

        if (WebSocketCommandEnum.SMOCKIN_ID.name().equals(message)) {
            session.getRemote().sendString(webSocketService.getExternalId(path, session));
            return;
        }

        // Ignore all other inbound messages from client
    }

}
