package com.smockin.mockserver.service;

import com.smockin.admin.service.utils.GeneralUtils;
import com.smockin.mockserver.service.enums.InboundParamTypeEnum;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import spark.Request;

/**
 * Created by mgallina on 09/08/17.
 */
@Service
public class InboundParamMatchServiceImpl implements InboundParamMatchService {

    @Override
    public String enrichWithInboundParamMatches(final Request req, final String responseBody) {

        if (responseBody == null) {
            return null;
        }

        String enrichedResponseBody = responseBody;

        final int MAX = 10000;
        int index = 0;

        while (true) {

            if (index > MAX) {
                throw new StackOverflowError("Error MAX iterations reached in 'while loop', whilst trying to swap out inbound param tokens.");
            }

            final String r = processParamMatch(req, enrichedResponseBody);

            if (r == null) {
                break;
            }

            enrichedResponseBody = r;

            index++;
        }

        return enrichedResponseBody;
    }

    String processParamMatch(final Request req, final String responseBody) {

        // Look up for any 'inbound param token' matches
        final String matchResult = GeneralUtils.findFirstInboundParamMatch(responseBody);

        if (matchResult == null) {
            // No tokens found so do nothing.
            return null;
        }

        // Determine the matching token type, is it a REQ_HEAD, REQ_PARAM or PATH_VAR...
        if (matchResult.startsWith(InboundParamTypeEnum.REQ_HEAD.name())) {

            final String headerName = StringUtils.trim(StringUtils.remove(matchResult, InboundParamTypeEnum.REQ_HEAD.name() + "="));
            final String headerValue = GeneralUtils.findHeaderIgnoreCase(req, headerName);

            return StringUtils.replace(responseBody, "${" + matchResult + "}", (headerValue != null)?headerValue:"", 1);
        } else if (matchResult.startsWith(InboundParamTypeEnum.REQ_PARAM.name())) {

            final String requestParamName = StringUtils.trim(StringUtils.remove(matchResult, InboundParamTypeEnum.REQ_PARAM.name() + "="));
            final String requestParamValue = GeneralUtils.findRequestParamIgnoreCase(req, requestParamName);

            return StringUtils.replace(responseBody, "${" + matchResult + "}", (requestParamValue != null)?requestParamValue:"", 1);
        } else if (matchResult.startsWith(InboundParamTypeEnum.PATH_VAR.name())) {

            final String pathVariableName = StringUtils.trim(StringUtils.remove(matchResult, InboundParamTypeEnum.PATH_VAR.name() + "="));
            final String pathVariableValue = GeneralUtils.findPathVarIgnoreCase(req, pathVariableName);

            return StringUtils.replace(responseBody, "${" + matchResult + "}", (pathVariableValue != null)?pathVariableValue:"", 1);
        } else {

            throw new IllegalArgumentException("Unsupported token : " + matchResult);
        }

    }

}
