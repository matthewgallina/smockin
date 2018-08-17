package com.smockin.mockserver.service;

import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;

/**
 * Created by mgallina.
 */
public interface JmsProxyService {

    void pushToQueue(final String externalId, final String body, final String mimeType, final long timeToLive, final String token) throws RecordNotFoundException, ValidationException;
    void pushToTopic(final String externalId, final String body, final String mimeType, final String token) throws RecordNotFoundException, ValidationException;
    void clearQueue(final String externalId, final String token) throws RecordNotFoundException, ValidationException;

}
