package com.smockin.mockserver.service;

import spark.Request;

/**
 * Created by mgallina.
 */

public interface InboundParamMatchService {

    String TO_ARG = "TO";
    String UNTIL_ARG = "UNTIL";

    String enrichWithInboundParamMatches(final Request req, final String mockPath, final String responseBody);

}
