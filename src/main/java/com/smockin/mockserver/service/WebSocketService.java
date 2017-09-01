package com.smockin.mockserver.service;

import com.smockin.mockserver.service.dto.WebSocketDTO;
import org.eclipse.jetty.websocket.api.Session;

import java.io.IOException;

/**
 * Created by mgallina.
 */
public interface WebSocketService {

    int MAX_IDLE_TIMEOUT_MILLIS = 3600000; // 1 hour max

    void registerSession(final String path, final Session session);
    void removeSession(final Session session);
    void pushMessage(final WebSocketDTO dto) throws IOException;

}
