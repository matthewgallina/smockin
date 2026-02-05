package com.smockin.mockserver.service;

import com.smockin.mockserver.exception.InboundParamMatchException;
import io.javalin.http.Context;

/**
 * Created by mgallina.
 */

public interface InboundParamMatchService {

    String enrichWithInboundParamMatches(final Context ctx,
                                         final String mockPath,
                                         final String responseBody,
                                         final String userCtxPath,
                                         final long mockOwnerUserId) throws InboundParamMatchException;

}
