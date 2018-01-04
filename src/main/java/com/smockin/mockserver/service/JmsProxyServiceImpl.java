package com.smockin.mockserver.service;

import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.ServerConfigDAO;
import com.smockin.mockserver.engine.MockedJmsServerEngine;
import com.smockin.utils.GeneralUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import spark.staticfiles.MimeType;

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
    public void pushToQueue(final String name, final String body, final String mimeType, final long timeToLive) throws ValidationException {
        logger.debug("pushToQueue called");

        if (StringUtils.isBlank(name)) {
            throw new ValidationException("name is required");
        }
        if (StringUtils.isBlank(body)) {
            throw new ValidationException("body is required");
        }
        if (StringUtils.isBlank(mimeType)) {
            throw new ValidationException("mimeType is required");
        }

        if (MediaType.TEXT_PLAIN_VALUE.equals(mimeType)) {
            mockedJmsServerEngine.sendTextMessageToQueue(name, body, timeToLive);
        } else {
            throw new ValidationException("Unsupported mimeType: " + mimeType);
        }

    }

    @Override
    public void pushToTopic(final String name, final String body, final String mimeType) throws ValidationException {
        logger.debug("pushToTopic called");

        if (StringUtils.isBlank(name)) {
            throw new ValidationException("name is required");
        }
        if (StringUtils.isBlank(body)) {
            throw new ValidationException("body is required");
        }
        if (StringUtils.isBlank(mimeType)) {
            throw new ValidationException("mimeType is required");
        }

        if (MediaType.TEXT_PLAIN_VALUE.equals(mimeType)) {
            mockedJmsServerEngine.broadcastTextMessageToTopic(name, body);
        } else {
            throw new ValidationException("Unsupported mimeType: " + mimeType);
        }

    }

    @Override
    public void clearQueue(final String queueName) throws ValidationException {
        logger.debug("clearQueue called");

        mockedJmsServerEngine.clearQueue(queueName);
    }

}
