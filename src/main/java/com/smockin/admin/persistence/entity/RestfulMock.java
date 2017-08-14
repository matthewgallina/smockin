package com.smockin.admin.persistence.entity;

import com.smockin.admin.persistence.enums.MockTypeEnum;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.persistence.enums.RestMethodEnum;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mgallina.
 */
@Entity
@Table(name = "REST_MOCK", uniqueConstraints={
    @UniqueConstraint(columnNames = {"PATH", "HTTP_METHOD"})
})
public class RestfulMock extends Identifier {

    @Column(name = "PATH", nullable = false, length = 1000)
    private String path;

    @Enumerated(EnumType.STRING)
    @Column(name = "HTTP_METHOD", nullable = false, length = 10)
    private RestMethodEnum method;

    @Enumerated(EnumType.STRING)
    @Column(name = "REC_STATUS", nullable = false, length = 15)
    private RecordStatusEnum status;

    @Enumerated(EnumType.STRING)
    @Column(name = "MOCK_TYPE", nullable = false, length = 10)
    private MockTypeEnum mockType;

    @Column(name = "PROXY_TIME_OUT_MILLIS", nullable = false)
    private long proxyTimeOutInMillis;

    @Column(name = "INIT_ORDER", nullable = false)
    private int initializationOrder;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "restfulMock", orphanRemoval = true)
    @OrderBy("orderNo ASC")
    private List<RestfulMockDefinitionRule> rules = new ArrayList<RestfulMockDefinitionRule>();

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "restfulMock", orphanRemoval = true)
    @OrderBy("orderNo ASC")
    private List<RestfulMockDefinitionOrder> definitions = new ArrayList<RestfulMockDefinitionOrder>();

    public RestfulMock() {
    }

    public RestfulMock(String path, RestMethodEnum method, RecordStatusEnum status, MockTypeEnum mockType, long proxyTimeOutInMillis) {
        this.path = path;
        this.method = method;
        this.status = status;
        this.mockType = mockType;
        this.proxyTimeOutInMillis = proxyTimeOutInMillis;
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

    public long getProxyTimeOutInMillis() {
        return proxyTimeOutInMillis;
    }
    public void setProxyTimeOutInMillis(long proxyTimeOutInMillis) {
        this.proxyTimeOutInMillis = proxyTimeOutInMillis;
    }

    public int getInitializationOrder() {
        return initializationOrder;
    }
    public void setInitializationOrder(int initializationOrder) {
        this.initializationOrder = initializationOrder;
    }

    public List<RestfulMockDefinitionRule> getRules() {
        return rules;
    }
    public void setRules(List<RestfulMockDefinitionRule> rules) {
        this.rules = rules;
    }

    public List<RestfulMockDefinitionOrder> getDefinitions() {
        return definitions;
    }
    public void setDefinitions(List<RestfulMockDefinitionOrder> definitions) {
        this.definitions = definitions;
    }

}
