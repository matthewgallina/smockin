package com.smockin.admin.dto.response;

import java.util.Map;

public class LiveLoggingOutboundContentDTO extends LiveLoggingContentDTO {

    private final int status;
    private final boolean proxyResponseMocked;

    public LiveLoggingOutboundContentDTO(final String contentType, final Map<String, String> headers, final String body, final int status, final boolean proxyResponseMocked) {
        super(contentType, headers, body);
        this.status = status;
        this.proxyResponseMocked = proxyResponseMocked;
    }

    public int getStatus() {
        return status;
    }
    public boolean isProxyResponseMocked() {
        return proxyResponseMocked;
    }

}

