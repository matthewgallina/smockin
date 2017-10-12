package com.smockin.mockserver.service;

import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.mockserver.service.dto.WebSocketClientDTO;
import com.smockin.mockserver.service.dto.WebSocketDTO;
import org.eclipse.jetty.websocket.api.Session;

import java.io.IOException;
import java.util.List;

/**
 * Created by mgallina.
 */
public interface WebSocketService {

    int MAX_IDLE_TIMEOUT_MILLIS = 3600000; // 1 hour max

    void registerSession(final String mockExtId, final String path, final long idleTimeoutMillis, final Session session);
    void removeSession(final Session session);
    void broadcastMessage(final WebSocketDTO dto) throws IOException;
    void sendMessage(final String id, final WebSocketDTO dto) throws IOException;
    List<WebSocketClientDTO> getClientConnections(final String mockExtId) throws RecordNotFoundException;
    String getExternalId(final String path, final Session session);
    void clearSession();

}
