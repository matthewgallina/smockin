package com.smockin.admin.persistence.entity;

import lombok.Data;
import org.hibernate.annotations.ColumnDefault;
import javax.persistence.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mgallina.
 */
@Entity
@Table(name = "REST_MOCK_DEF")
@Data
public class RestfulMockDefinitionOrder extends Identifier {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REST_MOCK_ID", nullable = false)
    private RestfulMock restfulMock;

    @Column(name = "HTTP_STATUS_CODE", nullable = false)
    private int httpStatusCode;

    @Column(name = "RESPONSE_CONTENT_TYPE", nullable = false, length = 100)
    private String responseContentType;

    @Column(name = "RESPONSE_BODY", length = VARCHAR_MAX_VALUE)
    private String responseBody;

    @Column(name = "SLEEP_IN_MILLIS", nullable = false)
    private long sleepInMillis;

    @ColumnDefault("false")
    @Column(name = "SUSPEND", nullable = false)
    private boolean suspend;

    @ColumnDefault("0")
    @Column(name = "FREQ_COUNT", nullable = false)
    private int frequencyCount;

    @ColumnDefault("0")
    @Column(name = "FREQ_PERCENT", nullable = false)
    private int frequencyPercentage;

    @Column(name = "ORDER_NO", nullable = false)
    private int orderNo;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name="REST_MOCK_DEF_RES_HDR")
    private Map<String, String> responseHeaders = new HashMap<>();

    public RestfulMockDefinitionOrder() {
    }

    public RestfulMockDefinitionOrder(RestfulMock restfulMock, int httpStatusCode, String responseContentType,
                                      String responseBody, int orderNo, long sleepInMillis,
                                      boolean suspend, int frequencyCount, int frequencyPercentage) {
        this.restfulMock = restfulMock;
        this.httpStatusCode = httpStatusCode;
        this.responseContentType = responseContentType;
        this.responseBody = responseBody;
        this.orderNo = orderNo;
        this.sleepInMillis = sleepInMillis;
        this.suspend = suspend;
        this.frequencyCount = frequencyCount;
        this.frequencyPercentage = frequencyPercentage;
    }

}
