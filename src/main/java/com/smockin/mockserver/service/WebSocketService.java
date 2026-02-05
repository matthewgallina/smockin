package com.smockin.mockserver.service;

import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.mockserver.exception.MockServerException;
import com.smockin.mockserver.service.dto.PushClientDTO;
import com.smockin.mockserver.service.dto.WebSocketDTO;
import org.eclipse.jetty.websocket.api.Session;

import java.io.IOException;
import java.util.List;

/**
 * Created by mgallina.
 */
public interface WebSocketService {

    int MAX_IDLE_TIMEOUT_MILLIS = 3600000; // 1 hour max
    String WS_SEC_WEBSOCKET_KEY = "Sec-WebSocket-Key";

    void registerSession(final Session session, final boolean isMultiUserMode);
    void removeSession(final Session session);
    void sendMessage(final String id, final WebSocketDTO dto) throws MockServerException;
    List<PushClientDTO> getClientConnections(final String mockExtId, final String token) throws RecordNotFoundException, ValidationException;
    String getExternalId(final Session session);
    void clearSession();
    void respondToMessage(final Session session, final String message) throws IOException;

}
