package com.smockin.mockserver.service.dto;

/**
 * Created by gallina.
 */
public class WebSocketDTO {

    private String path;
    private String body;

    public WebSocketDTO() {
    }

    public WebSocketDTO(String path, String body) {
        this.path = path;
        this.body = body;
    }

    public WebSocketDTO(String id, String path, String body) {
        this.path = path;
        this.body = body;
    }

    public String getPath() {
        return path;
    }
    public void setPath(String path) {
        this.path = path;
    }

    public String getBody() {
        return body;
    }
    public void setBody(String body) {
        this.body = body;
    }

}
