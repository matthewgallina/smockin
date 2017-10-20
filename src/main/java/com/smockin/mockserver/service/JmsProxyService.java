package com.smockin.mockserver.service;

import com.smockin.admin.exception.ValidationException;
import com.smockin.mockserver.exception.MockServerException;

import javax.jms.JMSException;

/**
 * Created by mgallina.
 */
public interface JmsProxyService {

    void pushToQueue(final String queueName, final String body) throws ValidationException, MockServerException;
    void clearQueue(final String queueName) throws JMSException, ValidationException;

}
