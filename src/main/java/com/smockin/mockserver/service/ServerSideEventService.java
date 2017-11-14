package com.smockin.mockserver.service;

import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.mockserver.service.dto.SseMessageDTO;
import com.smockin.mockserver.service.dto.PushClientDTO;
import spark.Response;

import java.io.IOException;
import java.util.List;

/**
 * Created by mgallina.
 */
public interface ServerSideEventService {

    void register(final String path, final long heartBeatMillis, final boolean proxyPushIdOnConnect, final Response response) throws IOException;
//    void clear(final String path);
    List<PushClientDTO> getClientConnections(final String mockExtId) throws RecordNotFoundException;
    void broadcastMessage(final SseMessageDTO dto);
    void addMessage(final String id, final SseMessageDTO dto);
    void interruptAndClearAllHeartBeatThreads();
    void clearState();

}
