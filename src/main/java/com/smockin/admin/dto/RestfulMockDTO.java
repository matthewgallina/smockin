package com.smockin.admin.dto;

import com.smockin.admin.persistence.enums.RestMockTypeEnum;
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
    private RestMockTypeEnum mockType;
    private long proxyTimeoutInMillis;
    private long webSocketTimeoutInMillis;
    private boolean randomiseDefinitions;
    private List<RestfulMockDefinitionDTO> definitions = new ArrayList<RestfulMockDefinitionDTO>();
    private List<RuleDTO> rules = new ArrayList<RuleDTO>();

    public RestfulMockDTO() {

    }

    public RestfulMockDTO(String path, RestMethodEnum method, RecordStatusEnum status, RestMockTypeEnum mockType, long proxyTimeoutInMillis, long webSocketTimeoutInMillis, boolean randomiseDefinitions) {
        this.path = path;
        this.method = method;
        this.status = status;
        this.mockType = mockType;
        this.proxyTimeoutInMillis = proxyTimeoutInMillis;
        this.webSocketTimeoutInMillis = webSocketTimeoutInMillis;
        this.randomiseDefinitions = randomiseDefinitions;
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

    public RestMockTypeEnum getMockType() {
        return mockType;
    }
    public void setMockType(RestMockTypeEnum mockType) {
        this.mockType = mockType;
    }

    public long getProxyTimeoutInMillis() {
        return proxyTimeoutInMillis;
    }
    public void setProxyTimeoutInMillis(long proxyTimeoutInMillis) {
        this.proxyTimeoutInMillis = proxyTimeoutInMillis;
    }

    public long getWebSocketTimeoutInMillis() {
        return webSocketTimeoutInMillis;
    }
    public void setWebSocketTimeoutInMillis(long webSocketTimeoutInMillis) {
        this.webSocketTimeoutInMillis = webSocketTimeoutInMillis;
    }

    public boolean isRandomiseDefinitions() {
        return randomiseDefinitions;
    }
    public void setRandomiseDefinitions(boolean randomiseDefinitions) {
        this.randomiseDefinitions = randomiseDefinitions;
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
