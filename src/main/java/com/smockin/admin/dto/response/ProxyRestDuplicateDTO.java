package com.smockin.admin.dto.response;

import com.smockin.admin.persistence.enums.RestMethodEnum;

import java.util.List;

public class ProxyRestDuplicateDTO {

    private String path;
    private RestMethodEnum method;
    private List<RestfulMockResponseDTO> mocks;

    public ProxyRestDuplicateDTO() {

    }

    public ProxyRestDuplicateDTO(final String path, final RestMethodEnum method, final List<RestfulMockResponseDTO> mocks) {
        this.path = path;
        this.method = method;
        this.mocks = mocks;
    }

    public String getPath() {
        return path;
    }
    public void setPath(String path) {
        this.path = path;
    }

    public RestMethodEnum getMethod() {
        return method;
    }
    public void setMethod(RestMethodEnum method) {
        this.method = method;
    }

    public List<RestfulMockResponseDTO> getMocks() {
        return mocks;
    }
    public void setMocks(List<RestfulMockResponseDTO> mocks) {
        this.mocks = mocks;
    }

}
