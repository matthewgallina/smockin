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

import java.io.IOException;

/**
 * Created by mgallina.
 */
@WebSocket
public class SparkWebSocketEchoService {

    private final String mockExtId;
    private final String path;
    private final long idleTimeoutMillis;
    private final boolean proxyPushIdOnConnect;
    private final WebSocketService webSocketService;
    private final boolean logMockCalls;

    public SparkWebSocketEchoService(final String mockExtId,
                                     final String path,
                                     final long idleTimeoutMillis,
                                     final boolean proxyPushIdOnConnect,
                                     final WebSocketService webSocketService,
                                     final boolean logMockCalls) {
        this.mockExtId = mockExtId;
        this.path = path;
        this.idleTimeoutMillis = idleTimeoutMillis;
        this.proxyPushIdOnConnect = proxyPushIdOnConnect;
        this.webSocketService = webSocketService;
        this.logMockCalls = logMockCalls;
    }

    @OnWebSocketConnect
    public void connected(final Session session) {
        webSocketService.registerSession(mockExtId, path, idleTimeoutMillis, proxyPushIdOnConnect, session, logMockCalls);
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
