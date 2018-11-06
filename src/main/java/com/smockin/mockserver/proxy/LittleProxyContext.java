package com.smockin.mockserver.proxy;

import io.netty.handler.codec.http.HttpResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LittleProxyContext {

    private final String requestId;
    private boolean useMock;
    private String userCtx;
    private String requestBody;
    private HttpResponse mockedClientResponse;
    private List<Map.Entry<String, String>> requestHeaders = new ArrayList<>();
    private LittleProxyLoggingDTO littleProxyLoggingDTO = new LittleProxyLoggingDTO();

    public LittleProxyContext(final String requestId) {
        this.requestId = requestId;
    }

    public String getRequestId() {
        return requestId;
    }

    public boolean isUseMock() {
        return useMock;
    }
    public void setUseMock(boolean useMock) {
        this.useMock = useMock;
    }

    public String getUserCtx() {
        return userCtx;
    }
    public void setUserCtx(String userCtx) {
        this.userCtx = userCtx;
    }

    public String getRequestBody() {
        return requestBody;
    }
    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public HttpResponse getMockedClientResponse() {
        return mockedClientResponse;
    }
    public void setMockedClientResponse(HttpResponse mockedClientResponse) {
        this.mockedClientResponse = mockedClientResponse;
    }

    public List<Map.Entry<String, String>> getRequestHeaders() {
        return requestHeaders;
    }
    public void setRequestHeaders(List<Map.Entry<String, String>> requestHeaders) {
        this.requestHeaders = requestHeaders;
    }

    public LittleProxyLoggingDTO getLittleProxyLoggingDTO() {
        return littleProxyLoggingDTO;
    }
    public void setLittleProxyLoggingDTO(LittleProxyLoggingDTO littleProxyLoggingDTO) {
        this.littleProxyLoggingDTO = littleProxyLoggingDTO;
    }

}
