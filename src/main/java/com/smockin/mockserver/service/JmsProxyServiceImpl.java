package com.smockin.mockserver.service;

import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.ServerConfigDAO;
import com.smockin.admin.persistence.entity.ServerConfig;
import com.smockin.admin.persistence.enums.ServerTypeEnum;
import com.smockin.mockserver.engine.MockedJmsServerEngine;
import com.smockin.mockserver.exception.MockServerException;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.jms.*;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by mgallina.
 */
@Service
public class JmsProxyServiceImpl implements JmsProxyService {

    private final Logger logger = LoggerFactory.getLogger(JmsProxyServiceImpl.class);

    @Autowired
    private ServerConfigDAO serverConfigDAO;

    @Autowired
    private MockedJmsServerEngine mockedJmsServerEngine;

    @Override
    public void pushToQueue(final String queueName, final String body) throws ValidationException, MockServerException {
        logger.debug("pushToQueue called");

        if (StringUtils.isBlank(queueName)) {
            throw new ValidationException("queueName is required");
        }
        if (StringUtils.isBlank(body)) {
            throw new ValidationException("body is required");
        }

        mockedJmsServerEngine.sendTextMessage(queueName, body);
    }

    @Override
    public void clearQueue(final String queueName) throws JMSException, ValidationException {
        logger.debug("clearQueue called");

    }

}
