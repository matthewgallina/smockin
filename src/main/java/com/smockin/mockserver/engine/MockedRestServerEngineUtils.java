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
import com.smockin.mockserver.service.HttpProxyService;
import com.smockin.mockserver.service.InboundParamMatchService;
import com.smockin.mockserver.service.JavaScriptResponseHandler;
import com.smockin.mockserver.service.MockOrderingCounterService;
import com.smockin.mockserver.service.RuleEngine;
import com.smockin.mockserver.service.ServerSideEventService;
import com.smockin.mockserver.service.StatefulService;
import com.smockin.mockserver.service.dto.RestfulResponseDTO;
import com.smockin.utils.GeneralUtils;
import io.javalin.http.Context;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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


    public Optional<String> loadMockedResponse(final Context ctx,
                                               final boolean isMultiUserMode,
                                               final boolean isProxyMode) {

        logger.debug("loadMockedResponse called");

        debugInboundRequest(ctx);

        return (isProxyMode)
            ? handleProxyInterceptorMode(isMultiUserMode, ctx)
            : handleMockLookup(ctx, isMultiUserMode, false);
    }

    Optional<String> handleMockLookup(final Context ctx,
                           final boolean isMultiUserMode,
                           final boolean ignore404MockResponses) {
        logger.debug("handleMockLookup called");

        try {

            RestMethodEnum method = RestMethodEnum.findByName(ctx.req().getMethod());

            if (RestMethodEnum.HEAD.equals(method)) {
                method = RestMethodEnum.GET;
            }

            final RestfulMock mock = (isMultiUserMode)
                    ? restfulMockDAO.findActiveByMethodAndPathPatternAndTypesForMultiUser(
                    method,
                    ctx.req().getPathInfo(),
                    Arrays.asList(RestMockTypeEnum.PROXY_SSE,
                            RestMockTypeEnum.PROXY_HTTP,
                            RestMockTypeEnum.SEQ,
                            RestMockTypeEnum.RULE,
                            RestMockTypeEnum.STATEFUL,
                            RestMockTypeEnum.CUSTOM_JS))
                    : restfulMockDAO.findActiveByMethodAndPathPatternAndTypesForSingleUser(
                    method,
                    ctx.req().getPathInfo(),
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
                return Optional.of(processSSERequest(mock, ctx));
            }

            removeSuspendedResponses(mock);

            final String responseBody = processRequest(mock, ctx, ignore404MockResponses);

            // Yuk! Bit of a hacky work around returning null from processRequest, so as to distinguish an ignored 404...
            return (responseBody != null)
                    ? Optional.of(responseBody)
                    : Optional.empty();

        } catch (Exception ex) {
            return handleFailure(ex, ctx);
        }

    }

    private String amendPathForMultiUser(final Context ctx, final boolean isMultiUserMode) {

        String inboundPath = ctx.req().getPathInfo();

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
                                                final Context ctx) {

        logger.debug("handleProxyInterceptorMode called");

        try {

            final String amendedInboundPath = amendPathForMultiUser(ctx, isMultiUserMode);
            final String inboundPath = ctx.req().getPathInfo();

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
                return handleMockLookup(ctx, isMultiUserMode, false);
            }

            // ACTIVE Mode...
            if (configOpt.isPresent() && ProxyModeTypeEnum.ACTIVE.equals(configOpt.get().getProxyModeType())) {

                // Look for mock...
                final Optional<String> result = handleMockLookup(ctx, isMultiUserMode, !configOpt.get().isDoNotForwardWhen404Mock());

                if (result.isPresent()) {
                    return result;
                }

                // Make downstream client call of no mock was found
                return handleClientDownstreamProxyCallResponse(
                        executeClientDownstreamProxyCall(amendedInboundPath, ctx, proxyDownstreamURL),
                        ctx,
                        proxyDownstreamURL);
            }

            // Default to REACTIVE mode...
            final Optional<HttpClientResponseDTO> httpClientResponse = executeClientDownstreamProxyCall(amendedInboundPath, ctx, proxyDownstreamURL);

            if (!httpClientResponse.isPresent()) {
                return Optional.empty();
            }

            if (HttpStatus.NOT_FOUND.value() == httpClientResponse.get().getStatus()) {

                // Look for mock substitute if downstream client returns a 404
                return handleMockLookup(ctx, isMultiUserMode, false);
            }

            // Pass back downstream client response directly back to caller
            return handleClientDownstreamProxyCallResponse(httpClientResponse, ctx, proxyDownstreamURL);

        } catch (Exception ex) {
            return handleFailure(ex, ctx);
        }

    }

    Optional<HttpClientResponseDTO> executeClientDownstreamProxyCall(final String inboundPath,
                                                                     final Context ctx,
                                                                     final String proxyDownstreamURL) {

        if (proxyDownstreamURL == null) {
            return Optional.empty();
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Initiating proxied call to downstream client for path: " + inboundPath);
        }

        final HttpClientCallDTO httpClientCallDTO = new HttpClientCallDTO();

        final String reqParams = (ctx.req().getQueryString() != null)
                ? ( "?" + ctx.req().getQueryString() )
                : "";

        if (logger.isDebugEnabled()) {
            logger.debug("Forwarding call : " + proxyDownstreamURL + inboundPath + reqParams);
        }

        httpClientCallDTO.setUrl(proxyDownstreamURL + inboundPath + reqParams);
        httpClientCallDTO.setMethod(RestMethodEnum.valueOf(ctx.req().getMethod()));
        httpClientCallDTO.setBody(ctx.body());

        httpClientCallDTO.setHeaders(ctx.headerMap());

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
                                                             final Context ctx,
                                                             final String proxyDownstreamURL) {

        if (!httpClientResponseOpt.isPresent()) {
            return Optional.empty();
        }

        final HttpClientResponseDTO httpClientResponse = httpClientResponseOpt.get();

        if (logger.isDebugEnabled()) {
            logger.debug("Downstream client response status: " + httpClientResponse.getStatus());
        }

        ctx.status(httpClientResponse.getStatus());
        ctx.contentType(httpClientResponse.getContentType());
        ctx.result(httpClientResponse.getBody());

        applyHeadersToResponse(httpClientResponse.getHeaders(), ctx);

        ctx.header(GeneralUtils.PROXIED_DOWNSTREAM_URL_HEADER, proxyDownstreamURL);

        return Optional.of(StringUtils.defaultIfBlank(httpClientResponse.getBody(),""));
    }

    String processRequest(final RestfulMock mock,
                          final Context ctx,
                          final boolean ignore404MockResponses) {
        logger.debug("processRequest called");

        RestfulResponseDTO outcome;

        switch (mock.getMockType()) {
            case RULE:
                outcome = ruleEngine.process(ctx, mock.getRules());
                break;
            case PROXY_HTTP:
                outcome = proxyService.waitForResponse(ctx.req().getPathInfo(), mock);
                break;
            case CUSTOM_JS:
                outcome = javaScriptResponseHandler.executeUserResponse(ctx, mock);
                break;
            case STATEFUL:
                outcome = statefulService.process(ctx, mock);
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

        ctx.status(outcome.getHttpStatusCode());
        ctx.contentType(outcome.getResponseContentType());

        // Apply any response headers
        applyHeadersToResponse(outcome.getHeaders(), ctx);

        String response;

        try {
            response = inboundParamMatchService.enrichWithInboundParamMatches(ctx, mock.getPath(), outcome.getResponseBody(), mock.getCreatedBy().getCtxPath(), mock.getCreatedBy().getId());
            handleLatency(mock);
        } catch (InboundParamMatchException e) {
            logger.error(e.getMessage());
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR.value());
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

    String processSSERequest(final RestfulMock mock, final Context ctx) {

        try {
            serverSideEventService.register(buildUserPath(mock), mock.getSseHeartBeatInMillis(), mock.isProxyPushIdOnConnect(), ctx);
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

    Optional<String> handleFailure(final Exception ex, final Context ctx) {
        logger.error("Error processing mock request", ex);

        ctx.status(HttpStatus.INTERNAL_SERVER_ERROR.value());
        ctx.result((ex instanceof IllegalArgumentException) ? ex.getMessage() : "Oops, looks like something went wrong with this mock!");

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
                              final Context ctx) {

        headers.entrySet()
            .forEach(e ->
                ctx.header(e.getKey(), e.getValue()));

    }

    Map<String, String> extractResponseHeadersAsMap(final Context ctx) {

        return ctx.res()
                .getHeaderNames()
                .stream()
                .collect(Collectors.toMap(h -> h, h -> ctx.res().getHeader(h)));
    }

    private void debugInboundRequest(final Context ctx) {

        if (logger.isDebugEnabled()) {

            logger.debug("inbound request url: " + ctx.req().getRequestURI());
            logger.debug("inbound request query string : " + ctx.req().getQueryString());
            logger.debug("inbound request method: " + ctx.req().getMethod());
            logger.debug("inbound request path: " + ctx.req().getPathInfo());
            logger.debug("inbound request body: " + ctx.body());

            ctx.headerMap().forEach((h, v) ->
                    logger.debug("inbound request header: " + h + " = " + v));
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
