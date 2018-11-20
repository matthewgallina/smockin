package com.smockin.mockserver.service;

import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.mockserver.exception.MockServerException;
import com.smockin.mockserver.service.dto.PushClientDTO;
import com.smockin.mockserver.service.dto.WebSocketDTO;
import org.eclipse.jetty.websocket.api.Session;
import java.util.List;

/**
 * Created by mgallina.
 */
public interface WebSocketService {

    int MAX_IDLE_TIMEOUT_MILLIS = 3600000; // 1 hour max

    void registerSession(final String mockExtId, final String path, final long idleTimeoutMillis, final boolean proxyPushIdOnConnect, final Session session, final boolean logMockCalls);
    void removeSession(final Session session);
    void sendMessage(final String id, final WebSocketDTO dto) throws MockServerException;
    List<PushClientDTO> getClientConnections(final String mockExtId, final String token) throws RecordNotFoundException, ValidationException;
    String getExternalId(final String path, final Session session);
    void clearSession();

}
