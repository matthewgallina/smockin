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

    @ColumnDefault("false")
    @Column(name = "RANDOM_LAT", nullable = false)
    private boolean randomiseLatency;

    @ColumnDefault("0")
    @Column(name = "RDM_LAT_RANGE_MIN", nullable = false)
    private long randomiseLatencyRangeMinMillis;

    @ColumnDefault("0")
    @Column(name = "RDM_LAT_RANGE_MAX", nullable = false)
    private long randomiseLatencyRangeMaxMillis;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "restfulMock", orphanRemoval = true)
    @OrderBy("orderNo ASC")
    private List<RestfulMockDefinitionRule> rules = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "restfulMock", orphanRemoval = true)
    @OrderBy("orderNo ASC")
    private List<RestfulMockDefinitionOrder> definitions = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "restfulMock", orphanRemoval = true)
    private RestfulMockJavaScriptHandler javaScriptHandler;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="PROJ_ID", nullable = true)
    private RestfulProject project;

    @Deprecated
    @ColumnDefault("false")
    @Column(name = "PROXY_FW_NO_RULE_MATCH", nullable = false)
    private boolean proxyForwardWhenNoRuleMatch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="CREATED_BY", nullable = true)
    private SmockinUser createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="STATEFUL_PARENT", nullable = true)
    private RestfulMock statefulParent;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "statefulParent", orphanRemoval = true)
    private List<RestfulMock> statefulChildren = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "restfulMock", orphanRemoval = true)
    private RestfulMockStatefulMeta restfulMockStatefulMeta;

    public RestfulMock() {
    }

    public RestfulMock(final String path, final RestMethodEnum method, final RecordStatusEnum status, final RestMockTypeEnum mockType, final long proxyTimeOutInMillis, final long webSocketTimeoutInMillis, final long sseHeartBeatInMillis,
                       final boolean proxyPushIdOnConnect, final boolean randomiseDefinitions, final boolean proxyForwardWhenNoRuleMatch, final SmockinUser createdBy, boolean randomiseLatency, final long randomiseLatencyRangeMinMillis,
                       final long randomiseLatencyRangeMaxMillis, final RestfulProject project) {
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
        this.randomiseLatency = randomiseLatency;
        this.randomiseLatencyRangeMinMillis = randomiseLatencyRangeMinMillis;
        this.randomiseLatencyRangeMaxMillis = randomiseLatencyRangeMaxMillis;
        this.project = project;
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

    public boolean isRandomiseLatency() {
        return randomiseLatency;
    }
    public void setRandomiseLatency(boolean randomiseLatency) {
        this.randomiseLatency = randomiseLatency;
    }

    public long getRandomiseLatencyRangeMinMillis() {
        return randomiseLatencyRangeMinMillis;
    }
    public void setRandomiseLatencyRangeMinMillis(long randomiseLatencyRangeMinMillis) {
        this.randomiseLatencyRangeMinMillis = randomiseLatencyRangeMinMillis;
    }

    public long getRandomiseLatencyRangeMaxMillis() {
        return randomiseLatencyRangeMaxMillis;
    }
    public void setRandomiseLatencyRangeMaxMillis(long randomiseLatencyRangeMaxMillis) {
        this.randomiseLatencyRangeMaxMillis = randomiseLatencyRangeMaxMillis;
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

    public RestfulMockJavaScriptHandler getJavaScriptHandler() {
        return javaScriptHandler;
    }
    public void setJavaScriptHandler(RestfulMockJavaScriptHandler javaScriptHandler) {
        this.javaScriptHandler = javaScriptHandler;
    }

    public RestfulProject getProject() {
        return project;
    }
    public void setProject(RestfulProject project) {
        this.project = project;
    }

    @Deprecated
    public boolean isProxyForwardWhenNoRuleMatch() {
        return proxyForwardWhenNoRuleMatch;
    }
    @Deprecated
    public void setProxyForwardWhenNoRuleMatch(boolean proxyForwardWhenNoRuleMatch) {
        this.proxyForwardWhenNoRuleMatch = proxyForwardWhenNoRuleMatch;
    }

    public SmockinUser getCreatedBy() {
        return createdBy;
    }
    public void setCreatedBy(SmockinUser createdBy) {
        this.createdBy = createdBy;
    }

    public RestfulMock getStatefulParent() {
        return statefulParent;
    }
    public void setStatefulParent(RestfulMock statefulParent) {
        this.statefulParent = statefulParent;
    }

    public List<RestfulMock> getStatefulChildren() {
        return statefulChildren;
    }
    public void setStatefulChildren(List<RestfulMock> statefulChildren) {
        this.statefulChildren = statefulChildren;
    }

    public RestfulMockStatefulMeta getRestfulMockStatefulMeta() {
        return restfulMockStatefulMeta;
    }
    public void setRestfulMockStatefulMeta(RestfulMockStatefulMeta restfulMockStatefulMeta) {
        this.restfulMockStatefulMeta = restfulMockStatefulMeta;
    }

}
