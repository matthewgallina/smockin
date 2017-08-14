package com.smockin.mockserver.service;

import spark.Request;

/**
 * Created by mgallina.
 */

public interface InboundParamMatchService {

    String enrichWithInboundParamMatches(final Request req, final String responseBody);

}
