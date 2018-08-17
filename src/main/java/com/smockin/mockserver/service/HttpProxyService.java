package com.smockin.mockserver.service;

import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.mockserver.service.dto.HttpProxiedDTO;
import com.smockin.mockserver.service.dto.RestfulResponseDTO;

/**
 * Created by mgallina.
 */
public interface HttpProxyService {

    int MAX_TIMEOUT_MILLIS = 1800000; // 30 mins

    RestfulResponseDTO waitForResponse(final String requestPath, final RestfulMock mock);
    void addResponse(final String externalId, final HttpProxiedDTO dto, final String token) throws RecordNotFoundException, ValidationException;
    void clearSession(final String externalId, final String token) throws RecordNotFoundException, ValidationException;
    void clearAllSessions();

}
