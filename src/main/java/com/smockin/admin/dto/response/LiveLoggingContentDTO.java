package com.smockin.admin.dto.response;

import java.util.Map;

public abstract class LiveLoggingContentDTO {

    private final String contentType;
    private final Map<String, String> headers;
    private final String body;

    public LiveLoggingContentDTO(final String contentType, final Map<String, String> headers, final String body) {
        this.contentType = contentType;
        this.headers = headers;
        this.body = body;
    }

    public String getContentType() {
        return contentType;
    }
    public Map<String, String> getHeaders() {
        return headers;
    }
    public String getBody() {
        return body;
    }

}

