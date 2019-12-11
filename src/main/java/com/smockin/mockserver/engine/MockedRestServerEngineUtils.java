package com.smockin.mockserver.engine;

import com.smockin.admin.persistence.dao.RestfulMockDAO;
import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.persistence.entity.RestfulMockDefinitionOrder;
import com.smockin.admin.persistence.entity.RestfulMockDefinitionRule;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.admin.persistence.enums.RestMockTypeEnum;
import com.smockin.mockserver.service.*;
import com.smockin.mockserver.service.dto.RestfulResponseDTO;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spark.Request;
import spark.Response;
import java.io.IOException;
import java.util.*;

/**
 * Created by mgallina.
 */
@Service
@Transactional
public class MockedRestServerEngineUtils {

    private final Logger logger = LoggerFactory.getLogger(MockedRestServerEngineUtils.class);

    @Autowired
    private RestfulMockDAO restfulMockDAO;

    @Autowired
    private MockOrderingCounterService mockOrderingCounterService;

    @Autowired
    private RuleEngine ruleEngine;

    @Autowired
    private HttpProxyService proxyService;

    @Autowired
    private InboundParamMatchService inboundParamMatchService;

    @Autowired
    private ServerSideEventService serverSideEventService;

    public Optional<String> loadMockedResponse(final Request request,
                                               final Response response,
                                               final boolean isMultiUserMode) {
        logger.debug("loadMockedResponse called");

        try {

            final RestfulMock mock = (isMultiUserMode)
                    ? restfulMockDAO.findActiveByMethodAndPathPatternAndTypesAndUserCtxPath(
                            RestMethodEnum.findByName(request.requestMethod()),
                            request.pathInfo(),
                            Arrays.asList(RestMockTypeEnum.PROXY_SSE,
                                    RestMockTypeEnum.PROXY_HTTP,
                                    RestMockTypeEnum.SEQ,
                                    RestMockTypeEnum.RULE))
                    : restfulMockDAO.findActiveByMethodAndPathPatternAndTypes(
                            RestMethodEnum.findByName(request.requestMethod()),
                            request.pathInfo(),
                            Arrays.asList(RestMockTypeEnum.PROXY_SSE,
                                          RestMockTypeEnum.PROXY_HTTP,
                                          RestMockTypeEnum.SEQ,
                                          RestMockTypeEnum.RULE));

            if (mock == null) {
                return Optional.empty();
            }

            if (RestMockTypeEnum.PROXY_SSE.equals(mock.getMockType())) {
                return Optional.of(processSSERequest(mock, request, response));
            }

            removeSuspendedResponses(mock);

            return Optional.of(processRequest(mock, request, response));

        } catch (Exception ex) {
            logger.error("Error processing mock request", ex);
            response.status(HttpStatus.INTERNAL_SERVER_ERROR.value());
            return Optional.of("Oops, looks like something went wrong with this mock!");
        }

    }

    String processRequest(final RestfulMock mock, final Request req, final Response res) {
        logger.debug("processRequest called");

        RestfulResponseDTO outcome;

        switch (mock.getMockType()) {
            case RULE:
                outcome = ruleEngine.process(req, mock.getRules());
                break;
            case PROXY_HTTP:
                outcome = proxyService.waitForResponse(req.pathInfo(), mock);
                break;
            case SEQ:
            default:
                outcome = mockOrderingCounterService.process(mock);
                break;
        }

        if (outcome == null) {
            // Load in default values
            outcome = getDefault(mock);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("status " + outcome.getHttpStatusCode());
            logger.debug("content type " + outcome.getResponseContentType());
            logger.debug("status " + outcome.getHttpStatusCode());
            logger.debug("response body " + outcome.getResponseBody());
        }

        res.status(outcome.getHttpStatusCode());
        res.type(outcome.getResponseContentType());

        // Apply any response headers
        outcome.getHeaders().entrySet().forEach(e ->
            res.header(e.getKey(), e.getValue())
        );

        final String response = inboundParamMatchService.enrichWithInboundParamMatches(req, outcome.getResponseBody());

        handleLatency(mock);

        if (logger.isDebugEnabled()) {
            logger.debug("final response " + response);
        }

        return StringUtils.defaultIfBlank(response,"");
    }

    RestfulResponseDTO getDefault(final RestfulMock restfulMock) {
        logger.debug("getDefault called");

        if (RestMockTypeEnum.PROXY_HTTP.equals(restfulMock.getMockType())) {
            return new RestfulResponseDTO(HttpStatus.NOT_FOUND.value());
        }

        if (RestMockTypeEnum.RULE.equals(restfulMock.getMockType())
                && restfulMock.isProxyForwardWhenNoRuleMatch()) {
            return new RestfulResponseDTO(HttpStatus.TEMPORARY_REDIRECT.value());
        }

        final RestfulMockDefinitionOrder mockDefOrder = restfulMock.getDefinitions().get(0);
        return new RestfulResponseDTO(mockDefOrder.getHttpStatusCode(), mockDefOrder.getResponseContentType(), mockDefOrder.getResponseBody(), mockDefOrder.getResponseHeaders().entrySet());
    }

    void removeSuspendedResponses(final RestfulMock mock) {
        logger.debug("removeSuspendedResponses called");

        final Iterator<RestfulMockDefinitionOrder> definitionsIter = mock.getDefinitions().iterator();

        while (definitionsIter.hasNext()) {
            final RestfulMockDefinitionOrder d = definitionsIter.next();

            if (d.isSuspend()) {
                definitionsIter.remove();
            }
        }

        final Iterator<RestfulMockDefinitionRule> rulesIter =  mock.getRules().iterator();

        while (rulesIter.hasNext()) {
            final RestfulMockDefinitionRule r = rulesIter.next();

            if (r.isSuspend()) {
                rulesIter.remove();
            }
        }

    }

    String processSSERequest(final RestfulMock mock, final Request req, final Response res) {

        try {
            serverSideEventService.register(mock.getPath(), mock.getSseHeartBeatInMillis(), mock.isProxyPushIdOnConnect(), req, res);
        } catch (IOException e) {
            logger.error("Error registering SEE client", e);
        }

        return "";
    }

    private void handleLatency(final RestfulMock mock) {

        if (!mock.isRandomiseLatency()) {
            return;
        }

        long min = (mock.getRandomiseLatencyRangeMinMillis() > 0) ? mock.getRandomiseLatencyRangeMinMillis() : 1000;
        long max = (mock.getRandomiseLatencyRangeMaxMillis() > 0) ? mock.getRandomiseLatencyRangeMaxMillis() : 5000;

        try {
            Thread.sleep(RandomUtils.nextLong(min, (max + 1)));
        } catch (InterruptedException ex) {
            logger.error("Failed to apply randomised latency and prolong current thread execution", ex);
        }

    }

}
