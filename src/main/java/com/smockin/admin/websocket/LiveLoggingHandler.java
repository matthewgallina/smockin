package com.smockin.admin.websocket;

import com.smockin.admin.dto.response.LiveLoggingDTO;

public interface LiveLoggingHandler {

    String WS_CONNECTED_USER_ROLE = "WS_CONNECTED_USER_ROLE";
    String WS_CONNECTED_USER_CTX_PATH = "WS_CONNECTED_USER_CTX_PATH";

    void broadcast(final LiveLoggingDTO dto);

}
