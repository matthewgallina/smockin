package com.smockin.admin.dto.response;

import java.util.Map;

public class LiveLoggingOutboundContentDTO extends LiveLoggingContentDTO {

    private final Integer status;
    private final boolean proxyResponseMocked;

    public LiveLoggingOutboundContentDTO(final Map<String, String> headers, final String body, final Integer status, final boolean proxyResponseMocked) {
        super(headers, body);
        this.status = status;
        this.proxyResponseMocked = proxyResponseMocked;
    }

    public Integer getStatus() {
        return status;
    }
    public boolean isProxyResponseMocked() {
        return proxyResponseMocked;
    }

}

