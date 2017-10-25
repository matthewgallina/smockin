package com.smockin.mockserver.service;

import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.ServerConfigDAO;
import com.smockin.mockserver.engine.MockedJmsServerEngine;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.jms.*;


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
    public void pushToQueue(final String queueName, final String body, final long timeToLive) throws ValidationException {
        logger.debug("pushToQueue called");

        if (StringUtils.isBlank(queueName)) {
            throw new ValidationException("queueName is required");
        }
        if (StringUtils.isBlank(body)) {
            throw new ValidationException("body is required");
        }

        mockedJmsServerEngine.sendTextMessage(queueName, body, timeToLive);
    }

    @Override
    public void clearQueue(final String queueName) throws ValidationException {
        logger.debug("clearQueue called");

        mockedJmsServerEngine.clearQueue(queueName);
    }

}
