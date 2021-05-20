package com.smockin.admin.dto.response;

import java.util.Map;

public abstract class LiveLoggingContentDTO {

    private final String url;
    private final Map<String, String> headers;
    private final String body;

    public LiveLoggingContentDTO(final String url, final Map<String, String> headers, final String body) {
        this.url = url;
        this.headers = headers;
        this.body = body;
    }

    public String getUrl() {
        return url;
    }
    public Map<String, String> getHeaders() {
        return headers;
    }
    public String getBody() {
        return body;
    }

}

