package com.smockin.admin.dto.response;

import java.util.Map;

public class LiveLoggingInboundContentDTO extends LiveLoggingContentDTO {

    private final String method;
    private final String url;

    public LiveLoggingInboundContentDTO(final String contentType, final Map<String, String> headers, final String method, final String url, final String body) {
        super(contentType, headers, body);
        this.method = method;
        this.url = url;
    }

    public String getMethod() {
        return method;
    }
    public String getUrl() {
        return url;
    }

}

