package com.smockin.admin.websocket;

import com.smockin.admin.dto.response.LiveLoggingDTO;

public interface LiveLoggingHandler {

    String WS_CONNECTED_USER_ROLE = "WS_CONNECTED_USER_ROLE";
    String WS_CONNECTED_USER_ADMIN_VIEW_ALL = "WS_CONNECTED_USER_ADMIN_VIEW_ALL";
    String WS_CONNECTED_USER_CTX_PATH = "WS_CONNECTED_USER_CTX_PATH";
    String WS_CONNECTED_USER_ID = "WS_CONNECTED_USER_ID";

    void broadcast(final LiveLoggingDTO dto);

}
