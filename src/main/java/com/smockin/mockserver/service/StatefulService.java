package com.smockin.mockserver.service;

import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.mockserver.service.dto.RestfulResponseDTO;
import io.javalin.http.Context;

public interface StatefulService {

    RestfulResponseDTO process(final Context ctx, final RestfulMock mock);
    void resetState(final String externalId, final String userToken) throws RecordNotFoundException, ValidationException;

}
