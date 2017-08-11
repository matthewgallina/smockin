package com.smockin.mockserver.service;

import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.mockserver.service.dto.ProxiedDTO;
import com.smockin.mockserver.service.dto.RestfulResponse;

/**
 * Created by mgallina.
 */
public interface ProxyService {

    RestfulResponse waitForResponse(final RestfulMock mock);
    void addResponse(final ProxiedDTO dto);

}
