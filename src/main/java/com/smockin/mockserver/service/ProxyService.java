package com.smockin.mockserver.service;

import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.mockserver.service.dto.ProxiedDTO;
import com.smockin.mockserver.service.dto.RestfulResponseDTO;

/**
 * Created by mgallina.
 */
public interface ProxyService {

    int MAX_TIMEOUT_MILLIS = 60000; // 1 min max

    RestfulResponseDTO waitForResponse(final String requestPath, final RestfulMock mock);
    void addResponse(final ProxiedDTO dto);

}
