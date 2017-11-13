package com.smockin.mockserver.service;

import com.smockin.admin.dto.PathDTO;
import com.smockin.mockserver.service.dto.SseMessageDTO;
import spark.Response;

import java.io.IOException;

/**
 * Created by mgallina.
 */
public interface ServerSideEventService {

    void registerClient(final String path, final long heartBeatMillis, final Response response) throws IOException;
    void clear(final String path);
    void addMessage(final SseMessageDTO dto);

}
