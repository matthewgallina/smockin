package com.smockin.mockserver.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
public class MQMockDTO {

    private String extId;
    private String name;
    private String mqType;
    private String destinationName;
    private boolean topic;
    private String brokerUrl;
    private Map<String, String> properties = new HashMap<>();
    private boolean saveMessages;
    private String status;
    private String createdBy;
    private int messageCount;

    public MQMockDTO(final String extId, final String name, final String mqType,
                     final String destinationName, final boolean topic, final String brokerUrl,
                     final boolean saveMessages, final String status, final String createdBy) {
        this.extId = extId;
        this.name = name;
        this.mqType = mqType;
        this.destinationName = destinationName;
        this.topic = topic;
        this.brokerUrl = brokerUrl;
        this.saveMessages = saveMessages;
        this.status = status;
        this.createdBy = createdBy;
    }
}
