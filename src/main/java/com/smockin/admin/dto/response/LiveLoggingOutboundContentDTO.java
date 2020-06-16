package com.smockin.admin.dto.response;

import java.util.Map;

public class LiveLoggingOutboundContentDTO extends LiveLoggingContentDTO {

    private final Integer status;
    private final boolean proxiedResponse;

    public LiveLoggingOutboundContentDTO(final Map<String, String> headers, final String body, final Integer status, final boolean proxiedResponse) {
        super(headers, body);
        this.status = status;
        this.proxiedResponse = proxiedResponse;
    }

    public Integer getStatus() {
        return status;
    }
    public boolean isProxiedResponse() {
        return proxiedResponse;
    }

}

