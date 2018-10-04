package com.smockin.admin.persistence.entity;

import com.smockin.admin.persistence.enums.RestMockTypeEnum;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import org.hibernate.annotations.ColumnDefault;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mgallina.
 */
@Entity
@Table(name = "REST_MOCK", uniqueConstraints={
    @UniqueConstraint(columnNames = {"PATH", "HTTP_METHOD", "CREATED_BY"})
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
    private RestMockTypeEnum mockType;

    @Column(name = "PROXY_TIME_OUT_MILLIS", nullable = false)
    private long proxyTimeOutInMillis;

    @ColumnDefault("0")
    @Column(name = "WS_TIME_OUT_MILLIS", nullable = false)
    private long webSocketTimeoutInMillis;

    @ColumnDefault("0")
    @Column(name = "SSE_HEART_BEAT_MILLIS", nullable = false)
    private long sseHeartBeatInMillis;

    @ColumnDefault("false")
    @Column(name = "PROXY_PUSH_ID_ON_CNCT", nullable = false)
    private boolean proxyPushIdOnConnect;

    @Column(name = "INIT_ORDER", nullable = false)
    private int initializationOrder;

    @Column(name = "RANDOM_DEF", nullable = false)
    private boolean randomiseDefinitions;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "restfulMock", orphanRemoval = true)
    @OrderBy("orderNo ASC")
    private List<RestfulMockDefinitionRule> rules = new ArrayList<RestfulMockDefinitionRule>();

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "restfulMock", orphanRemoval = true)
    @OrderBy("orderNo ASC")
    private List<RestfulMockDefinitionOrder> definitions = new ArrayList<RestfulMockDefinitionOrder>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="REST_CATGY_ID", nullable = true)
    private RestfulCategory category;

    @ColumnDefault("false")
    @Column(name = "PROXY_FW_NO_RULE_MATCH", nullable = false)
    private boolean proxyForwardWhenNoRuleMatch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="CREATED_BY", nullable = true)
    private SmockinUser createdBy;

    @ColumnDefault("false")
    @Column(name = "PROXY_PRTY", nullable = false)
    private boolean proxyPriority;

    public RestfulMock() {
    }

    public RestfulMock(String path, RestMethodEnum method, RecordStatusEnum status, RestMockTypeEnum mockType, long proxyTimeOutInMillis, long webSocketTimeoutInMillis, long sseHeartBeatInMillis, boolean proxyPushIdOnConnect, boolean randomiseDefinitions, boolean proxyForwardWhenNoRuleMatch, SmockinUser createdBy) {
        this.path = path;
        this.method = method;
        this.status = status;
        this.mockType = mockType;
        this.proxyTimeOutInMillis = proxyTimeOutInMillis;
        this.webSocketTimeoutInMillis = webSocketTimeoutInMillis;
        this.sseHeartBeatInMillis = sseHeartBeatInMillis;
        this.proxyPushIdOnConnect = proxyPushIdOnConnect;
        this.randomiseDefinitions = randomiseDefinitions;
        this.proxyForwardWhenNoRuleMatch = proxyForwardWhenNoRuleMatch;
        this.createdBy = createdBy;
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

    public long getProxyTimeOutInMillis() {
        return proxyTimeOutInMillis;
    }
    public void setProxyTimeOutInMillis(long proxyTimeOutInMillis) {
        this.proxyTimeOutInMillis = proxyTimeOutInMillis;
    }

    public long getWebSocketTimeoutInMillis() {
        return webSocketTimeoutInMillis;
    }
    public void setWebSocketTimeoutInMillis(long webSocketTimeoutInMillis) {
        this.webSocketTimeoutInMillis = webSocketTimeoutInMillis;
    }

    public long getSseHeartBeatInMillis() {
        return sseHeartBeatInMillis;
    }
    public void setSseHeartBeatInMillis(long sseHeartBeatInMillis) {
        this.sseHeartBeatInMillis = sseHeartBeatInMillis;
    }

    public boolean isProxyPushIdOnConnect() {
        return proxyPushIdOnConnect;
    }
    public void setProxyPushIdOnConnect(boolean proxyPushIdOnConnect) {
        this.proxyPushIdOnConnect = proxyPushIdOnConnect;
    }

    public int getInitializationOrder() {
        return initializationOrder;
    }
    public void setInitializationOrder(int initializationOrder) {
        this.initializationOrder = initializationOrder;
    }

    public boolean isRandomiseDefinitions() {
        return randomiseDefinitions;
    }
    public void setRandomiseDefinitions(boolean randomiseDefinitions) {
        this.randomiseDefinitions = randomiseDefinitions;
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

    public RestfulCategory getCategory() {
        return category;
    }
    public void setCategory(RestfulCategory category) {
        this.category = category;
    }

    public boolean isProxyForwardWhenNoRuleMatch() {
        return proxyForwardWhenNoRuleMatch;
    }
    public void setProxyForwardWhenNoRuleMatch(boolean proxyForwardWhenNoRuleMatch) {
        this.proxyForwardWhenNoRuleMatch = proxyForwardWhenNoRuleMatch;
    }

    public SmockinUser getCreatedBy() {
        return createdBy;
    }
    public void setCreatedBy(SmockinUser createdBy) {
        this.createdBy = createdBy;
    }

    public boolean isProxyPriority() {
        return proxyPriority;
    }
    public void setProxyPriority(boolean proxyPriority) {
        this.proxyPriority = proxyPriority;
    }

}
