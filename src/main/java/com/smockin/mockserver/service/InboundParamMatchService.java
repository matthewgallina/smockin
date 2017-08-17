package com.smockin.mockserver.service;

import spark.Request;

/**
 * Created by mgallina.
 */

public interface InboundParamMatchService {

    String TO_ARG = "to";
    String UNTIL_ARG = "until";

    String enrichWithInboundParamMatches(final Request req, final String responseBody);

}
