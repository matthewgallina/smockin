package com.smockin.mockserver.service;

import com.smockin.admin.dto.ProxiedDTO;
import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.mockserver.service.dto.RestfulResponse;
import spark.Request;

/**
 * Created by mgallina.
 */
public interface ProxyService {

    RestfulResponse waitForResponse(final String path);
    void addResponse(final ProxiedDTO dto);

}
