package com.smockin.mockserver.service.dto;

/**
 * Created by gallina.
 */
public class JmsProxiedDTO {

    private String name;
    private String body;
    private String mimeType;

    public JmsProxiedDTO() {

    }

    public JmsProxiedDTO(final String name, final String body, final String mimeType) {
        this.name = name;
        this.body = body;
        this.mimeType = mimeType;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
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
