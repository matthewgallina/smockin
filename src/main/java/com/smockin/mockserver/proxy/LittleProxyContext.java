package com.smockin.mockserver.proxy;

import io.netty.handler.codec.http.HttpResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LittleProxyContext {

    private boolean useMock;
    private String requestBody;
    private HttpResponse clientResponse;
    private List<Map.Entry<String, String>> requestHeaders = new ArrayList<>();

    public boolean isUseMock() {
        return useMock;
    }
    public void setUseMock(boolean useMock) {
        this.useMock = useMock;
    }

    public String getRequestBody() {
        return requestBody;
    }
    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public HttpResponse getClientResponse() {
        return clientResponse;
    }
    public void setClientResponse(HttpResponse clientResponse) {
        this.clientResponse = clientResponse;
    }

    public List<Map.Entry<String, String>> getRequestHeaders() {
        return requestHeaders;
    }

}
