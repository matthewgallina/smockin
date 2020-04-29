package com.smockin.mockserver.service;

import com.smockin.admin.service.SmockinUserService;
import com.smockin.mockserver.service.enums.ParamMatchTypeEnum;
import com.smockin.utils.GeneralUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import spark.Request;

import java.text.SimpleDateFormat;

/**
 * Created by mgallina on 09/08/17.
 */
@Service
public class InboundParamMatchServiceImpl implements InboundParamMatchService {

    @Autowired
    private SmockinUserService smockinUserService;

    @Override
    public String enrichWithInboundParamMatches(final Request req, final String mockPath, final String responseBody, final String userCtxPath) {

        if (responseBody == null) {
            return null;
        }

        final String sanitizedUserCtxInboundPath = GeneralUtils.sanitizeMultiUserPath(smockinUserService.getUserMode(), req.pathInfo(), userCtxPath);

        String enrichedResponseBody = responseBody;

        final int MAX = 10000;
        int index = 0;

        while (true) {

            if (index > MAX) {
                throw new StackOverflowError("Error MAX iterations reached in 'while loop', whilst trying to swap out inbound param tokens.");
            }

            final String r = processParamMatch(req, mockPath, enrichedResponseBody, sanitizedUserCtxInboundPath);

            if (r == null) {
                break;
            }

            enrichedResponseBody = r;

            index++;
        }

        return enrichedResponseBody;
    }

    String processParamMatch(final Request req,
                             final String mockPath,
                             final String responseBody,
                             final String sanitizedUserCtxInboundPath) {

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
        }

        if (matchResult.startsWith(ParamMatchTypeEnum.REQ_PARAM.name())) {

            final String requestParamName = StringUtils.trim(StringUtils.remove(matchResult, ParamMatchTypeEnum.REQ_PARAM.name() + "="));
            final String requestParamValue = GeneralUtils.findRequestParamIgnoreCase(req, requestParamName);
            return StringUtils.replace(responseBody, "${" + matchResult + "}", (requestParamValue != null)?requestParamValue:"", 1);
        }

        if (matchResult.startsWith(ParamMatchTypeEnum.PATH_VAR.name())) {

            final String pathVariableName = StringUtils.trim(StringUtils.remove(matchResult, ParamMatchTypeEnum.PATH_VAR.name() + "="));
            final String pathVariableValue = GeneralUtils.findPathVarIgnoreCase(sanitizedUserCtxInboundPath, mockPath, pathVariableName);
            return StringUtils.replace(responseBody, "${" + matchResult + "}", (pathVariableValue != null)?pathVariableValue:"", 1);
        }

        if (matchResult.equals(ParamMatchTypeEnum.ISO_DATETIME.name())) {
            return StringUtils.replace(responseBody, "${" + matchResult + "}", new SimpleDateFormat(GeneralUtils.ISO_DATETIME_FORMAT).format(GeneralUtils.getCurrentDate()), 1);
        }

        if (matchResult.equals(ParamMatchTypeEnum.ISO_DATE.name())) {
            return StringUtils.replace(responseBody, "${" + matchResult + "}", new SimpleDateFormat(GeneralUtils.ISO_DATE_FORMAT).format(GeneralUtils.getCurrentDate()), 1);
        }

        if (matchResult.equals(ParamMatchTypeEnum.UUID.name())) {
            return StringUtils.replace(responseBody, "${" + matchResult + "}", GeneralUtils.generateUUID(), 1);
        }

        if (matchResult.equals(ParamMatchTypeEnum.RANDOM_NUMBER.name())) {
            return StringUtils.replace(responseBody, "${" + matchResult + "}", String.valueOf(RandomUtils.nextInt()), 1);
        }

        if (matchResult.startsWith(ParamMatchTypeEnum.RANDOM_NUMBER.name())) {

            final String range = StringUtils.trim(StringUtils.remove(matchResult, ParamMatchTypeEnum.RANDOM_NUMBER.name() + "="));

            return StringUtils.replace(responseBody, "${" + matchResult + "}", String.valueOf(generateRandomForRange(range)), 1);
        }

        throw new IllegalArgumentException("Unsupported token : " + matchResult);
    }

    private int generateRandomForRange(final String range) {

        String arg = null;

        if (range.toUpperCase().contains(TO_ARG)) {
            arg = TO_ARG;
        } else if (range.toUpperCase().contains(UNTIL_ARG)) {
            arg = UNTIL_ARG;
        }

        if (arg == null) {
            throw new IllegalArgumentException("Expected '" + TO_ARG + "' or '" + UNTIL_ARG + "' arg in '" + ParamMatchTypeEnum.RANDOM_NUMBER.name() + "=' token");
        }

        final String[] rangeToArray = range.toUpperCase().split(arg);

        if (rangeToArray.length != 2) {
            throw new IllegalArgumentException("Missing number range for '" + arg + "' args. (i.e expect 1 " + arg + " 5)");
        }

        if (!NumberUtils.isCreatable(StringUtils.trim(rangeToArray[0]))
                || !NumberUtils.isCreatable(StringUtils.trim(rangeToArray[1]))) {
            throw new IllegalArgumentException("Range does not contain valid numbers. (i.e expect 1 " + arg + " 5)");
        }

        final int start = NumberUtils.toInt(StringUtils.trim(rangeToArray[0]));
        final int end = NumberUtils.toInt(StringUtils.trim(rangeToArray[1]));

        if (start >= 0 && end > 0) {

            return RandomUtils.nextInt(start, (arg.equals(TO_ARG))?(end+1):end);
        } else if (start <= 0 && end >= 0) {

            int s = -Math.abs(RandomUtils.nextInt(0, (arg.equals(TO_ARG))?(Math.abs(start)+1):Math.abs(start)));
            int e = RandomUtils.nextInt(0, (arg.equals(TO_ARG))?(end+1):end);

            return (e + s);
        } else if (start < 0 && end < 0) {

           return -Math.abs(RandomUtils.nextInt(Math.abs(end), (arg.equals(TO_ARG))?(Math.abs(start)+1):Math.abs(start)));
        }

        return 0;
    }

}
