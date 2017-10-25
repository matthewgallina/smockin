package com.smockin.mockserver.service;

import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.mockserver.service.dto.HttpProxiedDTO;
import com.smockin.mockserver.service.dto.RestfulResponseDTO;

/**
 * Created by mgallina.
 */
public interface HttpProxyService {

    int MAX_TIMEOUT_MILLIS = 1800000; // 30 mins

    RestfulResponseDTO waitForResponse(final String requestPath, final RestfulMock mock);
    void addResponse(final HttpProxiedDTO dto);
    void clearSession(final String requestPath);
    void clearSession();

}
