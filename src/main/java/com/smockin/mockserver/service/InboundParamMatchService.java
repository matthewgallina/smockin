package com.smockin.mockserver.service;

import com.smockin.mockserver.exception.InboundParamMatchException;
import spark.Request;

/**
 * Created by mgallina.
 */

public interface InboundParamMatchService {

    String enrichWithInboundParamMatches(final Request req,
                                         final String mockPath,
                                         final String responseBody,
                                         final String userCtxPath,
                                         final long mockOwnerUserId) throws InboundParamMatchException;

}
