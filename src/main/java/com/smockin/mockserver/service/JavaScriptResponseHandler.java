package com.smockin.mockserver.service;

import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.mockserver.service.dto.RestfulResponseDTO;
import io.javalin.http.Context;

public interface JavaScriptResponseHandler {

    String[] engineSecurityArgs = {
            "-strict",
            "--no-java",
            "--no-syntax-extensions"
    };

    String defaultRequestObject =
            " var request = { "
                + " path : null,"
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

    String keyValuePairFindFuncName = "lookUpKvp";
    String defaultKeyValuePairStoreObjectStart = "var kvpStore = ";
    String defaultKeyValuePairStoreObject = defaultKeyValuePairStoreObjectStart + "{};";

    String keyValuePairFindFunc = "function " + keyValuePairFindFuncName + "(k) {"
                        + "var result = kvpStore[k];"
                        + "return (result != null) ? result : 'N/A';"
                        + "}";

    String userResponseFunctionInvoker =
            " if (typeof handleResponse === 'function') { "
            + " handleResponse(request, response); "
            + "} else {"
            + " response.body = 'Expected handleResponse(request, response) function is undefined!';"
            + " response;"
            + "}";

    RestfulResponseDTO executeUserResponse(final Context ctx, final RestfulMock mock);

}
