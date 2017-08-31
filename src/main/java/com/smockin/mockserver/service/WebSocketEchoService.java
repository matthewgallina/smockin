package com.smockin.mockserver.service;

/**
 * Created by mgallina on 31/08/17.
 */
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
public class WebSocketEchoService {

    private final Logger logger = LoggerFactory.getLogger(WebSocketEchoService.class);

    // Store sessions if you want to, for example, broadcast a message to all users
//    private static final Queue<Session> sessions = new ConcurrentLinkedQueue<>();

    public WebSocketEchoService() {
    }

    @OnWebSocketConnect
    public void connected(Session session) {
//        sessions.add(session);
    }

    @OnWebSocketClose
    public void closed(Session session, int statusCode, String reason) {
//        sessions.remove(session);
    }

    @OnWebSocketMessage
    public void message(Session session, String message) throws IOException {
        session.getRemote().sendString(message);
    }

}
