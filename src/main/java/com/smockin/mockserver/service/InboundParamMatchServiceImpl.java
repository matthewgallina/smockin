package com.smockin.mockserver.service;

import com.smockin.utils.GeneralUtils;
import com.smockin.mockserver.service.enums.ParamMatchTypeEnum;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import spark.Request;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.UUID;

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
        if (matchResult.startsWith(ParamMatchTypeEnum.REQ_HEAD.name())) {

            final String headerName = StringUtils.trim(StringUtils.remove(matchResult, ParamMatchTypeEnum.REQ_HEAD.name() + "="));
            final String headerValue = GeneralUtils.findHeaderIgnoreCase(req, headerName);

            return StringUtils.replace(responseBody, "${" + matchResult + "}", (headerValue != null)?headerValue:"", 1);
        } else if (matchResult.startsWith(ParamMatchTypeEnum.REQ_PARAM.name())) {

            final String requestParamName = StringUtils.trim(StringUtils.remove(matchResult, ParamMatchTypeEnum.REQ_PARAM.name() + "="));
            final String requestParamValue = GeneralUtils.findRequestParamIgnoreCase(req, requestParamName);

            return StringUtils.replace(responseBody, "${" + matchResult + "}", (requestParamValue != null)?requestParamValue:"", 1);
        } else if (matchResult.startsWith(ParamMatchTypeEnum.PATH_VAR.name())) {

            final String pathVariableName = StringUtils.trim(StringUtils.remove(matchResult, ParamMatchTypeEnum.PATH_VAR.name() + "="));
            final String pathVariableValue = GeneralUtils.findPathVarIgnoreCase(req, pathVariableName);

            return StringUtils.replace(responseBody, "${" + matchResult + "}", (pathVariableValue != null)?pathVariableValue:"", 1);
        } else if (matchResult.equals(ParamMatchTypeEnum.ISO_DATETIME.name())) {

            return StringUtils.replace(responseBody, "${" + matchResult + "}", new SimpleDateFormat(GeneralUtils.ISO_DATETIME_FORMAT).format(GeneralUtils.getCurrentDate()), 1);
        } else if (matchResult.equals(ParamMatchTypeEnum.ISO_DATE.name())) {

            return StringUtils.replace(responseBody, "${" + matchResult + "}", new SimpleDateFormat(GeneralUtils.ISO_DATE_FORMAT).format(GeneralUtils.getCurrentDate()), 1);
        } else if (matchResult.equals(ParamMatchTypeEnum.UUID.name())) {

            return StringUtils.replace(responseBody, "${" + matchResult + "}", GeneralUtils.generateUUID(), 1);
        } else if (matchResult.startsWith(ParamMatchTypeEnum.RANDOM_NUMBER.name())) {

            if (matchResult.equals(ParamMatchTypeEnum.RANDOM_NUMBER.name())) {
                return StringUtils.replace(responseBody, "${" + matchResult + "}", String.valueOf(RandomUtils.nextInt()), 1);
            }

            // TODO
            final String range = StringUtils.trim(StringUtils.remove(matchResult, ParamMatchTypeEnum.RANDOM_NUMBER.name() + "="));

            return StringUtils.replace(responseBody, "${" + matchResult + "}", String.valueOf(RandomUtils.nextInt()), 1);
        } else {

            throw new IllegalArgumentException("Unsupported token : " + matchResult);
        }

    }

}
