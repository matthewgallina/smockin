package com.smockin.admin.dto;

import com.smockin.admin.persistence.enums.MockTypeEnum;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.persistence.enums.RestMethodEnum;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mgallina.
 */
public class RestfulMockDTO {

    private String path;
    private RestMethodEnum method;
    private RecordStatusEnum status;
    private MockTypeEnum mockType;
    private long proxyTimeoutInMillis;
    private List<RestfulMockDefinitionDTO> definitions = new ArrayList<RestfulMockDefinitionDTO>();
    private List<RuleDTO> rules = new ArrayList<RuleDTO>();

    public RestfulMockDTO() {

    }

    public RestfulMockDTO(String path, RestMethodEnum method, RecordStatusEnum status, MockTypeEnum mockType, long proxyTimeoutInMillis) {
        this.path = path;
        this.method = method;
        this.status = status;
        this.mockType = mockType;
        this.proxyTimeoutInMillis = proxyTimeoutInMillis;
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

    public RecordStatusEnum getStatus() {
        return status;
    }
    public void setStatus(RecordStatusEnum status) {
        this.status = status;
    }

    public MockTypeEnum getMockType() {
        return mockType;
    }
    public void setMockType(MockTypeEnum mockType) {
        this.mockType = mockType;
    }

    public long getProxyTimeoutInMillis() {
        return proxyTimeoutInMillis;
    }
    public void setProxyTimeoutInMillis(long proxyTimeoutInMillis) {
        this.proxyTimeoutInMillis = proxyTimeoutInMillis;
    }

    public List<RestfulMockDefinitionDTO> getDefinitions() {
        return definitions;
    }
    public void setDefinitions(List<RestfulMockDefinitionDTO> definitions) {
        this.definitions = definitions;
    }

    public List<RuleDTO> getRules() {
        return rules;
    }
    public void setRules(List<RuleDTO> rules) {
        this.rules = rules;
    }

}
