package com.smockin.admin.dto.response;

import java.util.Map;

public abstract class LiveLoggingContentDTO {

    private final Map<String, String> headers;
    private final String body;

    public LiveLoggingContentDTO(final Map<String, String> headers, final String body) {
        this.headers = headers;
        this.body = body;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }
    public String getBody() {
        return body;
    }

}

