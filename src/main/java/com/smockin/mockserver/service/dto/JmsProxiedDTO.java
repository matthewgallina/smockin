package com.smockin.mockserver.service.dto;

/**
 * Created by gallina.
 */
public class JmsProxiedDTO {

    private String queueName;
    private String body;

    public JmsProxiedDTO() {

    }

    public JmsProxiedDTO(final String queueName, final String body) {
        this.queueName = queueName;
        this.body = body;
    }

    public String getQueueName() {
        return queueName;
    }
    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public String getBody() {
        return body;
    }
    public void setBody(String body) {
        this.body = body;
    }

}
