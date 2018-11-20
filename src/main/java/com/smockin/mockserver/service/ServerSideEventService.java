package com.smockin.mockserver.service;

import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.mockserver.service.dto.SseMessageDTO;
import com.smockin.mockserver.service.dto.PushClientDTO;
import spark.Request;
import spark.Response;
import java.io.IOException;
import java.util.List;

/**
 * Created by mgallina.
 */
public interface ServerSideEventService {

    String SSE_EVENT_STREAM_HEADER = "text/event-stream;charset=UTF-8";

    void register(final String path, final long heartBeatMillis, final boolean proxyPushIdOnConnect, final Request request, final Response response, final boolean logMockCalls) throws IOException;
    List<PushClientDTO> getClientConnections(final String mockExtId, final String token) throws RecordNotFoundException, ValidationException;
    void addMessage(final String id, final SseMessageDTO dto);
    void interruptAndClearAllHeartBeatThreads();
    void clearState();

}
