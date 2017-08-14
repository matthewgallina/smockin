package com.smockin.admin.persistence.entity;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by mgallina.
 */
@Entity
@Table(name = "REST_MOCK_RULE")
public class RestfulMockDefinitionRule extends Identifier {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REST_MOCK_ID", nullable = false)
    private RestfulMock restfulMock;

    @Column(name = "ORDER_NO", nullable = false)
    private int orderNo;

    @Column(name = "HTTP_STATUS_CODE", nullable = false)
    private int httpStatusCode;

    @Column(name = "RESPONSE_CONTENT_TYPE", nullable = false, length = 100)
    private String responseContentType;

    @Column(name = "RESPONSE_BODY", nullable = false, length = 5000)
    private String responseBody;

    @Column(name = "SLEEP_IN_MILLIS", nullable = false)
    private long sleepInMillis;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name="REST_MOCK_RULE_RES_HDR")
    private Map<String, String> responseHeaders = new HashMap<String, String>();

    // Each 'rule group' is associated by 'OR'
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "rule", orphanRemoval = true)
    @OrderBy("orderNo ASC")
    private List<RestfulMockDefinitionRuleGroup> conditionGroups = new ArrayList<>();

    public RestfulMockDefinitionRule() {
    }

    public RestfulMockDefinitionRule(final RestfulMock mock, final int orderNo, final int httpStatusCode, final String responseContentType, final String responseBody, long sleepInMillis) {
        this.restfulMock = mock;
        this.orderNo = orderNo;
        this.httpStatusCode = httpStatusCode;
        this.responseContentType = responseContentType;
        this.responseBody = responseBody;
        this.sleepInMillis = sleepInMillis;
    }

    public RestfulMock getRestfulMock() {
        return restfulMock;
    }
    public void setRestfulMock(RestfulMock restfulMock) {
        this.restfulMock = restfulMock;
    }

    public int getOrderNo() {
        return orderNo;
    }
    public void setOrderNo(int orderNo) {
        this.orderNo = orderNo;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }
    public void setHttpStatusCode(int httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }

    public String getResponseContentType() {
        return responseContentType;
    }
    public void setResponseContentType(String responseContentType) {
        this.responseContentType = responseContentType;
    }

    public String getResponseBody() {
        return responseBody;
    }
    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public long getSleepInMillis() {
        return sleepInMillis;
    }
    public void setSleepInMillis(long sleepInMillis) {
        this.sleepInMillis = sleepInMillis;
    }

    public List<RestfulMockDefinitionRuleGroup> getConditionGroups() {
        return conditionGroups;
    }
    public void setConditionGroups(List<RestfulMockDefinitionRuleGroup> conditionGroups) {
        this.conditionGroups = conditionGroups;
    }

    public Map<String, String> getResponseHeaders() {
        return responseHeaders;
    }
    public void setResponseHeaders(Map<String, String> responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

}
