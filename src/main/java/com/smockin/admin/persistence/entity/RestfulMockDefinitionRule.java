package com.smockin.admin.persistence.entity;

import lombok.Data;
import org.hibernate.annotations.ColumnDefault;

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
@Data
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

    @Column(name = "RESPONSE_BODY", length = Integer.MAX_VALUE)
    private String responseBody;

    @Column(name = "SLEEP_IN_MILLIS", nullable = false)
    private long sleepInMillis;

    @ColumnDefault("false")
    @Column(name = "SUSPEND", nullable = false)
    private boolean suspend;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name="REST_MOCK_RULE_RES_HDR")
    private Map<String, String> responseHeaders = new HashMap<String, String>();

    // Each 'rule group' is associated by 'OR'
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "rule", orphanRemoval = true)
    @OrderBy("orderNo ASC")
    private List<RestfulMockDefinitionRuleGroup> conditionGroups = new ArrayList<>();

    public RestfulMockDefinitionRule() {
    }

    public RestfulMockDefinitionRule(final RestfulMock mock, final int orderNo, final int httpStatusCode,
                                     final String responseContentType, final String responseBody,
                                     long sleepInMillis, boolean suspend) {
        this.restfulMock = mock;
        this.orderNo = orderNo;
        this.httpStatusCode = httpStatusCode;
        this.responseContentType = responseContentType;
        this.responseBody = responseBody;
        this.sleepInMillis = sleepInMillis;
        this.suspend = suspend;
    }

}
