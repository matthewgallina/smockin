package com.smockin.mockserver.service;

import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.mockserver.service.dto.RestfulResponse;
import spark.Request;

/**
 * Created by mgallina on 09/08/17.
 */
public interface ProxyService {

    RestfulResponse waitForResponse(final String path);
    void addResponse(final String path, final String responseBody);

}
