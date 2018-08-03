package com.smockin.mockserver.service;

import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.JmsMockDAO;
import com.smockin.admin.persistence.entity.JmsMock;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import com.smockin.mockserver.engine.MockedJmsServerEngine;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * Created by mgallina.
 */
@Service
@Transactional
public class JmsProxyServiceImpl implements JmsProxyService {

    private final Logger logger = LoggerFactory.getLogger(JmsProxyServiceImpl.class);

    @Autowired
    private JmsMockDAO jmsMockDAO;

    @Autowired
    private MockedJmsServerEngine mockedJmsServerEngine;

    @Autowired
    private UserTokenServiceUtils userTokenServiceUtils;

    @Override
    public void pushToQueue(final String externalId, final String body, final String mimeType, final long timeToLive, final String token) throws RecordNotFoundException, ValidationException {
        logger.debug("pushToQueue called");

        if (StringUtils.isBlank(body)) {
            throw new ValidationException("body is required");
        }
        if (StringUtils.isBlank(mimeType)) {
            throw new ValidationException("mimeType is required");
        }

        final JmsMock jmsMock = loadJmsMock(externalId);

        userTokenServiceUtils.validateRecordOwner(jmsMock.getCreatedBy(), token);

        if (MediaType.TEXT_PLAIN_VALUE.equals(mimeType)) {
            mockedJmsServerEngine.sendTextMessageToQueue(mockedJmsServerEngine.buildJmsUserPath(jmsMock), body, timeToLive);
        } else {
            throw new ValidationException("Unsupported mimeType: " + mimeType);
        }

    }

    @Override
    public void pushToTopic(final String externalId, final String body, final String mimeType, final String token) throws RecordNotFoundException, ValidationException {
        logger.debug("pushToTopic called");

        if (StringUtils.isBlank(body)) {
            throw new ValidationException("body is required");
        }
        if (StringUtils.isBlank(mimeType)) {
            throw new ValidationException("mimeType is required");
        }

        final JmsMock jmsMock = loadJmsMock(externalId);

        userTokenServiceUtils.validateRecordOwner(jmsMock.getCreatedBy(), token);

        if (MediaType.TEXT_PLAIN_VALUE.equals(mimeType)) {
            mockedJmsServerEngine.broadcastTextMessageToTopic(mockedJmsServerEngine.buildJmsUserPath(jmsMock), body);
        } else {
            throw new ValidationException("Unsupported mimeType: " + mimeType);
        }

    }

    @Override
    public void clearQueue(final String externalId, final String token) throws RecordNotFoundException, ValidationException {
        logger.debug("clearQueue called");

        final JmsMock jmsMock = loadJmsMock(externalId);

        userTokenServiceUtils.validateRecordOwner(jmsMock.getCreatedBy(), token);

        mockedJmsServerEngine.clearQueue(mockedJmsServerEngine.buildJmsUserPath(jmsMock));
    }

    JmsMock loadJmsMock(final String mockExtId) throws RecordNotFoundException {
        logger.debug("loadJmsMock called");

        final JmsMock mock = jmsMockDAO.findByExtId(mockExtId);

        if (mock == null) {
            throw new RecordNotFoundException();
        }

        return mock;
    }

}
