package com.smockin.mockserver.service;

import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.mockserver.service.dto.RestfulResponseDTO;
import spark.Request;

public interface JavaScriptResponseHandler {

    String jsEngine = "JavaScript";

    String defaultRequestObject =
            " var request = { "
                + " pathVars : {},"
                + " body : null,"
                + " headers : {},"
                + " parameters : {}"
                + "};";

    String defaultResponseObject =
            " var response = { "
                + " body : null,"
                + " status : 404,"
                + " contentType : 'text/plain',"
                + " headers : {}"
                + "};";

    String userResponseFunctionInvoker =
            " if (typeof handleResponse === 'function') { "
            + " handleResponse(request, response); "
            + "} else {"
            + " response.body = 'Expected handleResponse(request, response) function is undefined!'; "
            + " response; "
            + "}";

    RestfulResponseDTO executeUserResponse(final Request req, final RestfulMock mock);

}
