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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Created by mgallina.
 */
@WebSocket
public class SparkWebSocketEchoService {

    private final Logger logger = LoggerFactory.getLogger(SparkWebSocketEchoService.class);

    private String path;
    private WebSocketService webSocketService;

    public SparkWebSocketEchoService(final String path, final WebSocketService webSocketService) {
        this.path = path;
        this.webSocketService = webSocketService;
    }

    @OnWebSocketConnect
    public void connected(Session session) {

        webSocketService.registerSession(path, session);
    }

    @OnWebSocketClose
    public void closed(Session session, int statusCode, String reason) {

        webSocketService.removeSession(session);
    }

    @OnWebSocketMessage
    public void message(Session session, String message) throws IOException {
    }

}
