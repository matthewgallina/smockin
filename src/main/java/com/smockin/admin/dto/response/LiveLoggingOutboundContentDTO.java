package com.smockin.admin.dto.response;

import java.util.Map;

public class LiveLoggingOutboundContentDTO extends LiveLoggingContentDTO {

    private final Integer status;

    public LiveLoggingOutboundContentDTO(final Map<String, String> headers, final String body, final Integer status) {
        super(headers, body);
        this.status = status;
    }

    public Integer getStatus() {
        return status;
    }

}

