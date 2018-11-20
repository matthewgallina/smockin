package com.smockin.admin.websocket;

import com.smockin.admin.dto.response.LiveLoggingDTO;

public interface LiveLoggingHandler {

    void broadcast(final LiveLoggingDTO dto);

}
