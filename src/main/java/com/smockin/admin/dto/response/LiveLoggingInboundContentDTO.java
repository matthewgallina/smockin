package com.smockin.admin.dto.response;

import java.util.Map;

public class LiveLoggingInboundContentDTO extends LiveLoggingContentDTO {

    private final String method;
    private final Map<String, String> requestParams;

    public LiveLoggingInboundContentDTO(final Map<String, String> headers,
                                        final String method,
                                        final String url,
                                        final String body,
                                        final Map<String, String> requestParams) {
        super(url, headers, body);
        this.method = method;
        this.requestParams = (requestParams != null && !requestParams.isEmpty()) ? requestParams : null;
    }

    public String getMethod() {
        return method;
    }
    public Map<String, String> getRequestParams() {
        return requestParams;
    }

}

