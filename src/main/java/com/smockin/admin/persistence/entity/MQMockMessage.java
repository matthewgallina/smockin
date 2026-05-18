package com.smockin.admin.persistence.entity;

import com.smockin.utils.GeneralUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "MQ_MOCK_MSG")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MQMockMessage extends Identifier {

    @Column(name = "MESSAGE_KEY", nullable = true, length = 500)
    private String messageKey;

    @Column(name = "MESSAGE_BODY", nullable = false, length = VARCHAR_MAX_VALUE)
    private String messageBody;

    @Column(name = "CONTENT_TYPE", nullable = true, length = 100)
    private String contentType;

    @Column(name = "HEADERS", nullable = true, length = VARCHAR_MAX_VALUE)
    private String headers;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "DATE_RECEIVED", nullable = false)
    private Date dateReceived;

    @Column(name = "PRODUCER_ID", nullable = true, length = 200)
    private String producerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MQ_MOCK_ID", nullable = false)
    private MQMock mqMock;

    public MQMockMessage(final String messageKey, final String messageBody, final String contentType,
                         final String headers, final MQMock mqMock) {
        this.messageKey = messageKey;
        this.messageBody = messageBody;
        this.contentType = contentType;
        this.headers = headers;
        this.dateReceived = GeneralUtils.getCurrentDate();
        this.mqMock = mqMock;
    }
}
