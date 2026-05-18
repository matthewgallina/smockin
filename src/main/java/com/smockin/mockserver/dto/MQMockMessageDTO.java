package com.smockin.mockserver.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.Map;

@Data
@NoArgsConstructor
public class MQMockMessageDTO {

    private String extId;
    private String messageKey;
    private String messageBody;
    private String contentType;
    private Map<String, String> headers;
    private Date dateReceived;
    private String producerId;
    private String mqMockExtId;

    public MQMockMessageDTO(final String extId, final String messageKey, final String messageBody,
                            final String contentType, final Map<String, String> headers,
                            final Date dateReceived, final String producerId, final String mqMockExtId) {
        this.extId = extId;
        this.messageKey = messageKey;
        this.messageBody = messageBody;
        this.contentType = contentType;
        this.headers = headers;
        this.dateReceived = dateReceived;
        this.producerId = producerId;
        this.mqMockExtId = mqMockExtId;
    }
}
