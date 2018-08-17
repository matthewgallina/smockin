package com.smockin.mockserver.service.dto;

/**
 * Created by gallina.
 */
public class JmsProxiedDTO {

    private String body;
    private String mimeType;

    public JmsProxiedDTO() {

    }

    public JmsProxiedDTO(final String body, final String mimeType) {
        this.body = body;
        this.mimeType = mimeType;
    }

    public String getBody() {
        return body;
    }
    public void setBody(String body) {
        this.body = body;
    }

    public String getMimeType() {
        return mimeType;
    }
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

}
