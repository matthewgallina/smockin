package com.smockin.mockserver.dto;

import com.smockin.admin.persistence.enums.RestMethodEnum;

public class ProxyActiveMock {

    private final String path;
    private final String userCtx;
    private final RestMethodEnum method;

    public ProxyActiveMock(String path, String userCtx, RestMethodEnum method) {
        this.path = path;
        this.userCtx = userCtx;
        this.method = method;
    }

    public String getPath() {
        return path;
    }
    public String getUserCtx() {
        return userCtx;
    }
    public RestMethodEnum getMethod() {
        return method;
    }

}
