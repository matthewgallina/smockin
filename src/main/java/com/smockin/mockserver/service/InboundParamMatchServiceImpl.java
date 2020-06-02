package com.smockin.mockserver.service;

import com.smockin.admin.dto.UserKeyValueDataDTO;
import com.smockin.admin.service.SmockinUserService;
import com.smockin.admin.service.UserKeyValueDataService;
import com.smockin.mockserver.exception.InboundParamMatchException;
import com.smockin.mockserver.service.enums.ParamMatchTypeEnum;
import com.smockin.utils.GeneralUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import spark.Request;

import java.text.SimpleDateFormat;

/**
 * Created by mgallina on 09/08/17.
 */
@Service
public class InboundParamMatchServiceImpl implements InboundParamMatchService {

    private final Logger logger = LoggerFactory.getLogger(InboundParamMatchServiceImpl.class);

    @Autowired
    private SmockinUserService smockinUserService;

    @Autowired
    private UserKeyValueDataService userKeyValueDataService;


    private static final String GENERAL_ERROR = "Error processing inbound param matching. Please check your token syntax";

    @Override
    public String enrichWithInboundParamMatches(final Request req,
                                                final String mockPath,
                                                final String responseBody,
                                                final String userCtxPath,
                                                final long mockOwnerUserId) throws InboundParamMatchException {

        if (responseBody == null) {
            return null;
        }

        final String sanitizedUserCtxInboundPath = GeneralUtils.sanitizeMultiUserPath(smockinUserService.getUserMode(), req.pathInfo(), userCtxPath);

        String enrichedResponseBody = responseBody;

        final int MAX = 10000;
        int index = 0;

        while (true) {

            if (index > MAX) {
                logger.error("Error MAX iterations reached in 'while loop', whilst trying to swap out inbound param tokens.");
                throw new InboundParamMatchException(GENERAL_ERROR);
            }

            final String r;

            try {
                r = processParamMatch(req, mockPath, enrichedResponseBody, sanitizedUserCtxInboundPath, mockOwnerUserId);
            } catch (Throwable ex) {
                logger.error(ex.getMessage());
                throw new InboundParamMatchException(GENERAL_ERROR);
            }

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
                             final String sanitizedUserCtxInboundPath,
                             final long mockOwnerUserId) {

        // Look up for any 'inbound param token' matches
        final Pair<ParamMatchTypeEnum, Integer> matchResult = findInboundParamMatch(responseBody);

        if (matchResult == null) {
            // No tokens found so do nothing.
            return null;
        }

        final ParamMatchTypeEnum paramMatchType = matchResult.getLeft();
        final int matchStartingPosition = matchResult.getRight();

        // Determine the matching token type, is it a requestHeader, requestParameter, pathVar, etc...
        if (ParamMatchTypeEnum.lookUpKvp.equals(paramMatchType)) {
            return processKvp(matchStartingPosition, sanitizedUserCtxInboundPath, mockPath, req, responseBody, mockOwnerUserId);
        }

        if (ParamMatchTypeEnum.requestHeader.equals(paramMatchType)) {
            return processRequestHeader(matchStartingPosition, req, responseBody);
        }

        if (ParamMatchTypeEnum.requestParameter.equals(paramMatchType)) {
            return processRequestParameter(matchStartingPosition, req, responseBody);
        }

        if (ParamMatchTypeEnum.pathVar.equals(paramMatchType)) {
            return processPathVariable(sanitizedUserCtxInboundPath, matchStartingPosition, mockPath, responseBody);
        }

        if (ParamMatchTypeEnum.requestBody.equals(paramMatchType)) {
            return StringUtils.replaceIgnoreCase(responseBody,
                    ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.requestBody,
                    (req.body() != null) ? req.body() : "",
                    1);
        }

        if (ParamMatchTypeEnum.isoDate.equals(paramMatchType)) {
            return StringUtils.replaceIgnoreCase(responseBody,
                    ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.isoDate,
                    new SimpleDateFormat(GeneralUtils.ISO_DATE_FORMAT).format(GeneralUtils.getCurrentDate()),
                    1);
        }

        if (ParamMatchTypeEnum.isoDatetime.equals(paramMatchType)) {
            return StringUtils.replaceIgnoreCase(responseBody,
                    ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.isoDatetime,
                    new SimpleDateFormat(GeneralUtils.ISO_DATETIME_FORMAT).format(GeneralUtils.getCurrentDate()),
                    1);
        }

        if (ParamMatchTypeEnum.uuid.equals(paramMatchType)) {
            return StringUtils.replaceIgnoreCase(responseBody,
                    ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.uuid,
                    GeneralUtils.generateUUID(),
                    1);
        }

        if (ParamMatchTypeEnum.randomNumber.equals(paramMatchType)) {
            return processRandomNumber(matchStartingPosition, responseBody);
        }

        throw new IllegalArgumentException("Unsupported token : " + matchResult);
    }

    Pair<ParamMatchTypeEnum, Integer> findInboundParamMatch(final String responseBody) {

        if (responseBody == null) {
            return null;
        }

        for (ParamMatchTypeEnum p : ParamMatchTypeEnum.values()) {
            final int pos = StringUtils.indexOf(responseBody, ParamMatchTypeEnum.PARAM_PREFIX + p.name() + ((p.takesArg()) ? "(" : ""));
            if (pos > -1) {
                return Pair.of(p, pos + ((p.takesArg()) ? 1 : 0));
            }
        }

        return null;
    }

    String extractArgName(final int matchStartPos, final ParamMatchTypeEnum paramMatchType, final String responseBody, final boolean isNested) {

        final int start = matchStartPos + (ParamMatchTypeEnum.PARAM_PREFIX + paramMatchType).length();
        final int closingPos = StringUtils.indexOf(responseBody, (isNested) ? "))" : ")", start);

        return StringUtils.substring(responseBody, start, closingPos);
    }

    String sanitiseArgName(String argName) {

        argName = StringUtils.remove(argName, "'");

        return StringUtils.remove(argName, "\"");
    }

    String processKvp(final int matchStartingPosition,
                      final String sanitizedUserCtxInboundPath,
                      final String mockPath,
                      final Request req,
                      final String responseBody,
                      final long mockOwnerUserId) {

        // Determine the matching token type, is it a requestHeader, requestParameter, pathVar, etc...

        final String kvpKey = extractArgName(matchStartingPosition, ParamMatchTypeEnum.lookUpKvp, responseBody, false);
        String sanitisedKvpKey = sanitiseArgName(kvpKey);

        if (sanitisedKvpKey.contains("(") && !sanitisedKvpKey.contains(")")) {
            sanitisedKvpKey = sanitisedKvpKey.concat(")");
        }

        if (logger.isDebugEnabled()) {
            logger.debug("RAW KVP: " + kvpKey);
            logger.debug("Cleaned KVP : " + sanitisedKvpKey);
        }

        // Check if kvpKey is a nested ParamMatchTypeEnum itself
        final Pair<ParamMatchTypeEnum, Integer> kvpMatchResult = findInboundParamMatch(sanitisedKvpKey);
        final boolean isNested = (kvpMatchResult != null);

        if (isNested) {

            if (logger.isDebugEnabled()) {
                logger.debug("Nested KVP request type: " + kvpMatchResult.getLeft());
            }

            final String nestedRequestKey = extractArgName(kvpMatchResult.getRight(), kvpMatchResult.getLeft(), sanitisedKvpKey, isNested);

            if (logger.isDebugEnabled()) {
                logger.debug("Nested KVP request key: " + nestedRequestKey);
            }

            switch (kvpMatchResult.getLeft()) {
                case requestHeader:
                    sanitisedKvpKey = GeneralUtils.findHeaderIgnoreCase(req, sanitiseArgName(nestedRequestKey));
                    break;
                case requestParameter:
                    sanitisedKvpKey = GeneralUtils.findRequestParamIgnoreCase(req, sanitiseArgName(nestedRequestKey));
                    break;
                case pathVar:
                    sanitisedKvpKey = GeneralUtils.findPathVarIgnoreCase(sanitizedUserCtxInboundPath, mockPath, sanitiseArgName(nestedRequestKey));
                    break;
                case requestBody:
                    sanitisedKvpKey = req.body();
                    break;
                default:
                    sanitisedKvpKey = null;
                    break;
            }
        }

        final UserKeyValueDataDTO userKeyValueDataDTO = (sanitisedKvpKey != null)
                ? userKeyValueDataService.loadByKey(sanitisedKvpKey, mockOwnerUserId)
                : null;

        if (logger.isDebugEnabled()) {
            logger.debug("KVP value: " + ((userKeyValueDataDTO != null) ? userKeyValueDataDTO.getValue() : null));
        }

        return StringUtils.replaceIgnoreCase(responseBody,
                ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.lookUpKvp + "(" + kvpKey + ((kvpKey.contains("(")) ? "))" : ")"),
                (userKeyValueDataDTO != null) ? userKeyValueDataDTO.getValue() : "",
                1);
    }

    String processRequestHeader(final int matchStartingPosition, final Request req, final String responseBody) {

        final String headerName = extractArgName(matchStartingPosition, ParamMatchTypeEnum.requestHeader, responseBody, false);
        final String headerValue = GeneralUtils.findHeaderIgnoreCase(req, sanitiseArgName(headerName));

        if (logger.isDebugEnabled()) {
            logger.debug("raw header: " + headerName);
            logger.debug("cleaned header: " + sanitiseArgName(headerName));
            logger.debug("header value: " + headerValue);
        }

        return StringUtils.replaceIgnoreCase(responseBody,
                ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.requestHeader + "(" + headerName + ")",
                (headerValue != null) ? headerValue : "",
                1);
    }

    String processRequestParameter(final int matchStartingPosition, final Request req, final String responseBody) {

        final String requestParamName = extractArgName(matchStartingPosition, ParamMatchTypeEnum.requestParameter, responseBody, false);
        final String requestParamValue = GeneralUtils.findRequestParamIgnoreCase(req, sanitiseArgName(requestParamName));

        if (logger.isDebugEnabled()) {
            logger.debug("RAW request param: " + requestParamName);
            logger.debug("Cleaned request param: " + sanitiseArgName(requestParamName));
            logger.debug("Request param value: " + requestParamValue);
        }

        return StringUtils.replaceIgnoreCase(responseBody,
                ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.requestParameter + "(" + requestParamName + ")",
                (requestParamValue != null) ? requestParamValue : "",
                1);

    }

    String processPathVariable(final String sanitizedUserCtxInboundPath, final int matchStartingPosition, final String mockPath, final String responseBody) {

        final String pathVariableName = extractArgName(matchStartingPosition, ParamMatchTypeEnum.pathVar, responseBody, false);
        final String pathVariableValue = GeneralUtils.findPathVarIgnoreCase(sanitizedUserCtxInboundPath, mockPath, sanitiseArgName(pathVariableName));

        if (logger.isDebugEnabled()) {
            logger.debug("RAW path var: " + pathVariableName);
            logger.debug("Cleaned path var : " + sanitiseArgName(pathVariableName));
            logger.debug("Path var value: " + pathVariableValue);
        }

        return StringUtils.replaceIgnoreCase(responseBody,
                ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.pathVar + "(" + pathVariableName + ")",
                (pathVariableValue != null) ? pathVariableValue : "",
                1);

    }

    String processRandomNumber(final int matchStartingPosition, final String responseBody) {

        final String randomNumberContent = extractArgName(matchStartingPosition, ParamMatchTypeEnum.randomNumber, responseBody, false);

        if (logger.isDebugEnabled()) {
            logger.debug("Random number params: " + randomNumberContent);
        }

        if (randomNumberContent == null) {
            throw new IllegalArgumentException(ParamMatchTypeEnum.randomNumber.name() + " is missing args");
        }

        final String[] randomNumberContentParams = StringUtils.split(randomNumberContent, ",");

        if (randomNumberContentParams.length == 0) {
            throw new IllegalArgumentException(ParamMatchTypeEnum.randomNumber.name() + " is missing args");
        }

        if (randomNumberContentParams.length > 2) {
            throw new IllegalArgumentException(ParamMatchTypeEnum.randomNumber.name() + " has too many args");
        }

        final int startInc = (randomNumberContentParams.length == 2) ? Integer.parseInt(randomNumberContentParams[0].trim()) : 0;
        final int endExcl = (randomNumberContentParams.length == 2) ? Integer.parseInt(randomNumberContentParams[1].trim()) : Integer.parseInt(randomNumberContentParams[0].trim());
        final int randomValue = RandomUtils.nextInt(startInc, endExcl);

        if (logger.isDebugEnabled()) {
            logger.debug("Random number value: " + randomValue);
        }

        return StringUtils.replaceIgnoreCase(responseBody,
                ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.randomNumber + "(" + randomNumberContent + ")",
                String.valueOf(randomValue),
                1);
    }

}
