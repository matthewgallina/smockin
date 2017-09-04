package com.smockin.mockserver.service.dto;

/**
 * Created by mgallina.
 */
public class WebSocketClientDTO {

    private String id;

    public WebSocketClientDTO() {
    }

    public WebSocketClientDTO(final String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
    public void setId(String handshakeId) {
        this.id = id;
    }

}
