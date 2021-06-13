package com.smockin.mockserver.engine;

import com.smockin.admin.dto.HttpClientCallDTO;
import com.smockin.admin.dto.response.HttpClientResponseDTO;
import com.smockin.admin.persistence.dao.RestfulMockDAO;
import com.smockin.admin.persistence.dao.SmockinUserDAO;
import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.persistence.entity.RestfulMockDefinitionOrder;
import com.smockin.admin.persistence.entity.RestfulMockDefinitionRule;
import com.smockin.admin.persistence.enums.ProxyModeTypeEnum;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.admin.persistence.enums.RestMockTypeEnum;
import com.smockin.admin.persistence.enums.SmockinUserRoleEnum;
import com.smockin.admin.service.HttpClientService;
import com.smockin.mockserver.dto.ProxyForwardConfigCacheDTO;
import com.smockin.mockserver.dto.ProxyForwardMappingDTO;
import com.smockin.mockserver.exception.InboundParamMatchException;
import com.smockin.mockserver.service.*;
import com.smockin.mockserver.service.dto.RestfulResponseDTO;
import com.smockin.utils.GeneralUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by mgallina.
 */
@Service
@Transactional(readOnly = true)
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
    private JavaScriptResponseHandler javaScriptResponseHandler;

    @Autowired
    private InboundParamMatchService inboundParamMatchService;

    @Autowired
    private ServerSideEventService serverSideEventService;

    @Autowired
    private StatefulService statefulService;

    @Autowired
    private HttpClientService httpClientService;

    @Autowired
    private SmockinUserDAO smockinUserDAO;

    @Autowired
    private ProxyMappingCache proxyMappingCache;


    public Optional<String> loadMockedResponse(final Request request,
                                               final Response response,
                                               final boolean isMultiUserMode,
                                               final boolean isProxyMode) {

        logger.debug("loadMockedResponse called");

        debugInboundRequest(request);

        return (isProxyMode)
            ? handleProxyInterceptorMode(isMultiUserMode,
                                         request,
                                         response)
            : handleMockLookup(request, response, isMultiUserMode, false);
    }

    Optional<String> handleMockLookup(final Request request,
                           final Response response,
                           final boolean isMultiUserMode,
                           final boolean ignore404MockResponses) {
        logger.debug("handleMockLookup called");

        try {

            RestMethodEnum method = RestMethodEnum.findByName(request.requestMethod());

            if (RestMethodEnum.HEAD.equals(method)) {
                method = RestMethodEnum.GET;
            }

            final RestfulMock mock = (isMultiUserMode)
                    ? restfulMockDAO.findActiveByMethodAndPathPatternAndTypesForMultiUser(
                    method,
                    request.pathInfo(),
                    Arrays.asList(RestMockTypeEnum.PROXY_SSE,
                            RestMockTypeEnum.PROXY_HTTP,
                            RestMockTypeEnum.SEQ,
                            RestMockTypeEnum.RULE,
                            RestMockTypeEnum.STATEFUL,
                            RestMockTypeEnum.CUSTOM_JS))
                    : restfulMockDAO.findActiveByMethodAndPathPatternAndTypesForSingleUser(
                    method,
                    request.pathInfo(),
                    Arrays.asList(RestMockTypeEnum.PROXY_SSE,
                            RestMockTypeEnum.PROXY_HTTP,
                            RestMockTypeEnum.SEQ,
                            RestMockTypeEnum.RULE,
                            RestMockTypeEnum.STATEFUL,
                            RestMockTypeEnum.CUSTOM_JS));

            if (mock == null) {
                logger.debug("no mock was found");
                return Optional.empty();
            }

            debugLoadedMock(mock);

            if (RestMockTypeEnum.PROXY_SSE.equals(mock.getMockType())) {
                return Optional.of(processSSERequest(mock, request, response));
            }

            removeSuspendedResponses(mock);

            final String responseBody = processRequest(mock, request, response, ignore404MockResponses);

            // Yuk! Bit of a hacky work around returning null from processRequest, so as to distinguish an ignored 404...
            return (responseBody != null)
                    ? Optional.of(responseBody)
                    : Optional.empty();

        } catch (Exception ex) {
            return handleFailure(ex, response);
        }

    }

    private String amendPathForMultiUser(final Request request, final boolean isMultiUserMode) {

        String inboundPath = request.pathInfo();

        if (isMultiUserMode) {

            final String userCtxPathSegment = extractMultiUserCtxPathSegment(inboundPath);

            if (isInboundPathMultiUserPath(userCtxPathSegment)) {
                // Strip off User Path prefix for proxy mapping lookup
                return StringUtils.remove(inboundPath, GeneralUtils.URL_PATH_SEPARATOR + userCtxPathSegment);
            }

        }

        return inboundPath;
    }

    public String extractMultiUserCtxPathSegment(final String inboundPath) {

        // As long as path is not null this will always return at least 1 element.
        return StringUtils.split(inboundPath, GeneralUtils.URL_PATH_SEPARATOR)[0];
    }

    public boolean isInboundPathMultiUserPath(final String userCtxPathSegment) {
        // TODO remove this DB query and replace with a cached list of user CTX paths.
        return smockinUserDAO.doesUserExistWithCtxPath(userCtxPathSegment);
    }

    Optional<String> handleProxyInterceptorMode(final boolean isMultiUserMode,
                                                final Request request,
                                                final Response response) {

        logger.debug("handleProxyInterceptorMode called");

        try {

            final String amendedInboundPath = amendPathForMultiUser(request, isMultiUserMode);
            final String inboundPath = request.pathInfo();

            final String userCtxPath = (!StringUtils.equals(inboundPath, amendedInboundPath))
                ? extractMultiUserCtxPathSegment(inboundPath)
                : "";

            final Optional<ProxyForwardConfigCacheDTO> configOpt = proxyMappingCache.find(userCtxPath);

            String proxyDownstreamURL = (configOpt.isPresent())
                                            ? lookUpProxyMappingDownstreamUrl(amendedInboundPath, configOpt.get().getProxyForwardMappings())
                                            : null;

            if (proxyDownstreamURL == null && configOpt.isPresent()) {
                proxyDownstreamURL = lookUpDefaultProxyMappingDownstreamUrl(configOpt.get().getProxyForwardMappings());
            }

            // No relevant proxy mappings were found for the inbound path, so skip this section and just look for a mock.
            if (proxyDownstreamURL == null) {
                return handleMockLookup(request, response, isMultiUserMode, false);
            }

            // ACTIVE Mode...
            if (configOpt.isPresent() && ProxyModeTypeEnum.ACTIVE.equals(configOpt.get().getProxyModeType())) {

                // Look for mock...
                final Optional<String> result = handleMockLookup(request, response, isMultiUserMode, !configOpt.get().isDoNotForwardWhen404Mock());

                if (result.isPresent()) {
                    return result;
                }

                // Make downstream client call of no mock was found
                return handleClientDownstreamProxyCallResponse(
                        executeClientDownstreamProxyCall(amendedInboundPath, request, proxyDownstreamURL),
                        response,
                        proxyDownstreamURL);
            }

            // Default to REACTIVE mode...
            final Optional<HttpClientResponseDTO> httpClientResponse = executeClientDownstreamProxyCall(amendedInboundPath, request, proxyDownstreamURL);

            if (!httpClientResponse.isPresent()) {
                return Optional.empty();
            }

            if (HttpStatus.NOT_FOUND.value() == httpClientResponse.get().getStatus()) {

                // Look for mock substitute if downstream client returns a 404
                return handleMockLookup(request, response, isMultiUserMode, false);
            }

            // Pass back downstream client response directly back to caller
            return handleClientDownstreamProxyCallResponse(httpClientResponse, response, proxyDownstreamURL);

        } catch (Exception ex) {
            return handleFailure(ex, response);
        }

    }

    Optional<HttpClientResponseDTO> executeClientDownstreamProxyCall(final String inboundPath,
                                                                     final Request request,
                                                                     final String proxyDownstreamURL) {

        if (proxyDownstreamURL == null) {
            return Optional.empty();
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Initiating proxied call to downstream client for path: " + inboundPath);
        }

        final HttpClientCallDTO httpClientCallDTO = new HttpClientCallDTO();

        final String reqParams = (request.queryString() != null)
                ? ( "?" + request.queryString() )
                : "";

        if (logger.isDebugEnabled()) {
            logger.debug("Forwarding call : " + proxyDownstreamURL + inboundPath + reqParams);
        }

        httpClientCallDTO.setUrl(proxyDownstreamURL + inboundPath + reqParams);
        httpClientCallDTO.setMethod(RestMethodEnum.valueOf(request.requestMethod()));
        httpClientCallDTO.setBody(request.body());

        httpClientCallDTO.setHeaders(request
                .headers()
                .stream()
                .collect(Collectors.toMap(k -> k, v -> request.headers(v))));

        httpClientCallDTO.getHeaders().put(HttpHeaders.HOST, sanitizeHost(proxyDownstreamURL));

        try {
            return Optional.of(httpClientService.handleExternalCall(httpClientCallDTO));
        } catch (Throwable ex) {
            logger.error("Error making proxy downstream call: " + ex.getMessage());
            return Optional.empty();
        }

    }

    String sanitizeHost(final String proxyDownstreamURL) {

        String host = StringUtils.remove(proxyDownstreamURL, HttpClientService.HTTPS_PROTOCOL);
        host = StringUtils.remove(host, HttpClientService.HTTP_PROTOCOL);
        host = StringUtils.removeAll(host, GeneralUtils.URL_PATH_SEPARATOR);

        return host;
    }

    Optional<String> handleClientDownstreamProxyCallResponse(final Optional<HttpClientResponseDTO> httpClientResponseOpt,
                                                             final Response response,
                                                             final String proxyDownstreamURL) {

        if (!httpClientResponseOpt.isPresent()) {
            return Optional.empty();
        }

        final HttpClientResponseDTO httpClientResponse = httpClientResponseOpt.get();

        if (logger.isDebugEnabled()) {
            logger.debug("Downstream client response status: " + httpClientResponse.getStatus());
        }

        response.status(httpClientResponse.getStatus());
        response.type(httpClientResponse.getContentType());
        response.body(httpClientResponse.getBody());

        applyHeadersToResponse(httpClientResponse.getHeaders(), response);

        response.header(GeneralUtils.PROXIED_DOWNSTREAM_URL_HEADER, proxyDownstreamURL);

        return Optional.of(StringUtils.defaultIfBlank(httpClientResponse.getBody(),""));
    }

    String processRequest(final RestfulMock mock,
                          final Request req,
                          final Response res,
                          final boolean ignore404MockResponses) {
        logger.debug("processRequest called");

        RestfulResponseDTO outcome;

        switch (mock.getMockType()) {
            case RULE:
                outcome = ruleEngine.process(req, mock.getRules());
                break;
            case PROXY_HTTP:
                outcome = proxyService.waitForResponse(req.pathInfo(), mock);
                break;
            case CUSTOM_JS:
                outcome = javaScriptResponseHandler.executeUserResponse(req, mock);
                break;
            case STATEFUL:
                outcome = statefulService.process(req, mock);
                break;
            case SEQ:
            default:
                outcome = mockOrderingCounterService.process(mock);
                break;
        }

        if (outcome == null) {
            // Load in default values
            outcome = getDefault(mock);
        } else if (ignore404MockResponses
                        && HttpStatus.NOT_FOUND.value() == outcome.getHttpStatusCode()) {
            // Yuk! Bit of a hacky work around returning null so as to distinguish an ignored 404...
            return null;
        }

        debugOutcome(outcome);

        res.status(outcome.getHttpStatusCode());
        res.type(outcome.getResponseContentType());

        // Apply any response headers
        applyHeadersToResponse(outcome.getHeaders(), res);

        String response;

        try {
            response = inboundParamMatchService.enrichWithInboundParamMatches(req, mock.getPath(), outcome.getResponseBody(), mock.getCreatedBy().getCtxPath(), mock.getCreatedBy().getId());
            handleLatency(mock);
        } catch (InboundParamMatchException e) {
            logger.error(e.getMessage());
            res.status(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response = e.getMessage();
        }

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
            serverSideEventService.register(buildUserPath(mock), mock.getSseHeartBeatInMillis(), mock.isProxyPushIdOnConnect(), req, res);
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

    public String buildUserPath(final RestfulMock mock) {

        if (!SmockinUserRoleEnum.SYS_ADMIN.equals(mock.getCreatedBy().getRole())) {
            return GeneralUtils.URL_PATH_SEPARATOR + mock.getCreatedBy().getCtxPath() + mock.getPath();
        }

        return mock.getPath();
    }

    Optional<String> handleFailure(final Exception ex, final Response response) {
        logger.error("Error processing mock request", ex);

        response.status(HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.body((ex instanceof IllegalArgumentException) ? ex.getMessage() : "Oops, looks like something went wrong with this mock!");

        return Optional.of("Oops"); // this message does not come through to caller when it is a 500 for some reason, so setting in body above

    }

    String lookUpDefaultProxyMappingDownstreamUrl(final List<ProxyForwardMappingDTO> proxyForwardMappings) {

        return lookUpProxyMappingDownstreamUrl(GeneralUtils.PATH_WILDCARD, proxyForwardMappings);
    }

    String lookUpProxyMappingDownstreamUrl(final String inboundPath,
                                           final List<ProxyForwardMappingDTO> proxyForwardMappings) {

        return proxyForwardMappings
                .stream()
                .filter(p -> {

                    if (StringUtils.endsWith(p.getPath(), GeneralUtils.PATH_WILDCARD)
                            && StringUtils.startsWith(inboundPath, StringUtils.removeEnd(p.getPath(), GeneralUtils.PATH_WILDCARD))) {
                        return true;
                    }

                    return StringUtils.equals(inboundPath, p.getPath());

                })
                .map(p ->
                        p.getProxyForwardUrl())
                .findFirst()
                .orElse(null);
    }

    void applyHeadersToResponse(final Map<String, String> headers,
                              final Response response) {

        headers.entrySet()
            .forEach(e ->
                response.header(e.getKey(), e.getValue()));

    }

    Map<String, String> extractResponseHeadersAsMap(final Response response) {

        return response
                .raw()
                .getHeaderNames()
                .stream()
                .collect(Collectors.toMap(h -> h, h -> response.raw().getHeader(h)));
    }

    private void debugInboundRequest(final Request request) {

        if (logger.isDebugEnabled()) {

            logger.debug("inbound request url: " + request.url());
            logger.debug("inbound request query string : " + request.queryString());
            logger.debug("inbound request method: " + request.requestMethod());
            logger.debug("inbound request path: " + request.pathInfo());
            logger.debug("inbound request body: " + request.body());

            request.headers()
                .stream()
                .forEach(h ->
                    logger.debug("inbound request header: " + h + " = " + request.headers(h)));
        }

    }

    private void debugLoadedMock(final RestfulMock mock) {

        if (logger.isDebugEnabled()) {

            logger.debug("mock ext id: " + mock.getExtId());
            logger.debug("mock method: " + mock.getMethod());
            logger.debug("mock path: " + mock.getPath());
            logger.debug("mock type: " + mock.getMockType());

        }

    }

    private void debugOutcome(final RestfulResponseDTO outcome) {

        if (logger.isDebugEnabled()) {

            logger.debug("status " + outcome.getHttpStatusCode());
            logger.debug("content type " + outcome.getResponseContentType());
            logger.debug("status " + outcome.getHttpStatusCode());
            logger.debug("response body " + outcome.getResponseBody());

        }

    }

}
