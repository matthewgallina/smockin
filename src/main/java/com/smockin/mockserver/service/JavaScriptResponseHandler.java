package com.smockin.mockserver.service;

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
                + " status : 200,"
                + " contentType : 'text/plain',"
                + " headers : {}"
                + "};";

    String userResponseFunctionInvoker =
            " if (typeof handleResponse === 'function') { "
            + " handleResponse(request, response); "
            + "} else {"
            + " response.status = 404; "
            + " response.body = 'mock js logic is undefined!'; "
            + " response; "
            + "}";

    RestfulResponseDTO executeUserResponse(final Request req, final String userDefinedResponseFunc);

}
