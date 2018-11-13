package com.smockin.mockserver.proxy;

import java.util.HashMap;
import java.util.Map;

public class LittleProxyLoggingDTO {

    private boolean loggingRequestReceived;
    private int loggingResponseStatus;
    private String loggingResponseContentType;
    private Map<String, String> responseHeaders = new HashMap<>();

    public boolean isLoggingRequestReceived() {
        return loggingRequestReceived;
    }
    public void setLoggingRequestReceived(boolean loggingRequestReceived) {
        this.loggingRequestReceived = loggingRequestReceived;
    }

    public int getLoggingResponseStatus() {
        return loggingResponseStatus;
    }
    public void setLoggingResponseStatus(int loggingResponseStatus) {
        this.loggingResponseStatus = loggingResponseStatus;
    }

    public String getLoggingResponseContentType() {
        return loggingResponseContentType;
    }
    public void setLoggingResponseContentType(String loggingResponseContentType) {
        this.loggingResponseContentType = loggingResponseContentType;
    }

    public Map<String, String> getResponseHeaders() {
        return responseHeaders;
    }
    public void setResponseHeaders(Map<String, String> responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

}
