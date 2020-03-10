package com.smockin.mockserver.service;

import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.mockserver.service.dto.RestfulResponseDTO;
import spark.Request;

public interface StatefulService {

    RestfulResponseDTO process(final Request req, final RestfulMock mock);
    void resetState(final RestfulMock mock);

}
