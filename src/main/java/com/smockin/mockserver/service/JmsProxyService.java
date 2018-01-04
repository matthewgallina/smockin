package com.smockin.mockserver.service;

import com.smockin.admin.exception.ValidationException;
import com.smockin.mockserver.exception.MockServerException;

import javax.jms.JMSException;

/**
 * Created by mgallina.
 */
public interface JmsProxyService {

    void pushToQueue(final String queueName, final String body, final String mimeType, final long timeToLive) throws ValidationException;
    void pushToTopic(final String topicName, final String body, final String mimeType) throws ValidationException;
    void clearQueue(final String queueName) throws ValidationException;

}
