package com.smockin.mockserver.service.dto;

/**
 * Created by mgallina.
 */
public class SseMessageDTO {

    private String path;
    private String body;

    public SseMessageDTO() {

    }

    public SseMessageDTO(String path, String body) {
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
