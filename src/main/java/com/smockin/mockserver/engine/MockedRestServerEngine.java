package com.smockin.mockserver.engine;

import com.smockin.admin.persistence.dao.RestfulMockDAO;
import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.persistence.entity.RestfulMockDefinitionOrder;
import com.smockin.admin.persistence.entity.RestfulMockDefinitionRule;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.admin.persistence.enums.RestMockTypeEnum;
import com.smockin.admin.persistence.enums.SmockinUserRoleEnum;
import com.smockin.admin.websocket.LiveLoggingHandler;
import com.smockin.mockserver.dto.MockServerState;
import com.smockin.mockserver.dto.MockedServerConfigDTO;
import com.smockin.mockserver.dto.ProxyActiveMock;
import com.smockin.mockserver.exception.MockServerException;
import com.smockin.mockserver.proxy.ProxyServer;
import com.smockin.mockserver.service.*;
import com.smockin.mockserver.service.dto.RestfulResponseDTO;
import com.smockin.mockserver.service.ws.SparkWebSocketEchoService;
import com.smockin.utils.GeneralUtils;
import com.smockin.utils.LiveLoggingUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spark.Request;
import spark.Response;
import spark.Spark;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by mgallina.
 */
@Service
public class MockedRestServerEngine implements MockServerEngine<MockedServerConfigDTO, List<RestfulMock>> {

    private final Logger logger = LoggerFactory.getLogger(MockedRestServerEngine.class);
    private final Logger mockTrafficLogger = LoggerFactory.getLogger("mock_traffic_logger");

    @Autowired
    private RestfulMockDAO restfulMockDAO;

    @Autowired
    private RuleEngine ruleEngine;

    @Autowired
    private HttpProxyService proxyService;

    @Autowired
    private MockOrderingCounterService mockOrderingCounterService;

    @Autowired
    private InboundParamMatchService inboundParamMatchService;

    @Autowired
    private WebSocketService webSocketService;

    @Autowired
    private ServerSideEventService serverSideEventService;

    @Autowired
    private ProxyServer proxyServer;

    @Autowired
    private LiveLoggingHandler mockLogFeedHandler;


    private final Object monitor = new Object();
    private MockServerState serverState = new MockServerState(false, 0);
    private Map<Long, Date> deployedMocks;

    @Override
    public void start(final MockedServerConfigDTO config, final List<RestfulMock> mocks) throws MockServerException {
        logger.debug("start called");

        initServerConfig(config);

        // Invoke all lazily loaded data and detach entity.
        invokeAndDetachData(mocks);

        // Define all web socket routes first as the Spark framework requires this
        buildWebSocketEndpoints(mocks);

        // Handle Cross-Origin Resource Sharing (CORS) support
        handleCORS(config);

        // Next handle all HTTP RESTFul web service routes
        final List<RestfulMock> activeRestfulMocks = buildRESTEndpoints(mocks);

        setDeployedMocks(mocks);

        // Next handle all HTTP SSE web service routes
        buildSSEEndpoints(mocks);

        applyTrafficLogging(config);

        applyDefaultRoutes404Hack();

        initServer(config.getPort());

        initProxyServer(activeRestfulMocks, config);

    }

    @Override
    public Map<Long, Date> loadDeployedMocks() {
        return Collections.unmodifiableMap(deployedMocks);
    }

    @Override
    public MockServerState getCurrentState() throws MockServerException {
        synchronized (monitor) {
            return serverState;
        }
    }

    @Override
    public void shutdown() throws MockServerException {

        try {

            serverSideEventService.interruptAndClearAllHeartBeatThreads();

            Spark.stop();

            // Having dug around the source code, 'Spark.stop()' runs off a different thread when stopping the server and removing it's state such as routes, etc.
            // This means that calling 'Spark.port()' immediately after stop, results in an IllegalStateException, as the
            // 'initialized' flag is checked in the current thread and is still marked as true.
            // (The error thrown: java.lang.IllegalStateException: This must be done before route mapping has begun)
            // Short of editing the Spark source to fix this, I have therefore had to add this hack to buy the 'stop' thread time to complete.
            Thread.sleep(3000);

            synchronized (monitor) {
                serverState.setRunning(false);
            }

            clearState();

            proxyServer.shutdown();

        } catch (Throwable ex) {
            throw new MockServerException(ex);
        } finally {
            clearDeployedMocks();
        }

    }

    void initServer(final int port) throws MockServerException {
        logger.debug("initServer called");

        try {

            clearState();

            Spark.init();

            // Blocks the current thread (using a CountDownLatch under the hood) until the server is fully initialised.
            Spark.awaitInitialization();

            synchronized (monitor) {
                serverState.setRunning(true);
                serverState.setPort(port);
            }

        } catch (Throwable ex) {
            throw new MockServerException(ex);
        }

    }

    void initServerConfig(final MockedServerConfigDTO config) {
        logger.debug("initServerConfig called");

        if (logger.isDebugEnabled())
            logger.debug(config.toString());

        Spark.port(config.getPort());
        Spark.threadPool(config.getMaxThreads(), config.getMinThreads(), config.getTimeOutMillis());
    }

    void initProxyServer(final List<RestfulMock> activeMocks, final MockedServerConfigDTO config) {

        if (!isProxyServerModeEnabled(config)) {
            return;
        }

        final List<ProxyActiveMock> activeProxyMocks = new ArrayList<>();

        activeMocks.stream()
                .collect(Collectors.groupingBy(RestfulMock::getPath))
                .entrySet()
                .stream()
                .forEach(g -> {

                    final RestfulMock mock = (g.getValue().size() > 1)
                            ? g.getValue()
                                .stream()
                                .filter(m -> m.isProxyPriority())
                                .findFirst()
                                .orElseThrow(() -> new IllegalArgumentException(GeneralUtils.PROXY_PATH_CONFLICT))
                            : g.getValue()
                                .get(0);

                    activeProxyMocks.add(new ProxyActiveMock(mock.getPath(), mock.getCreatedBy().getCtxPath(), mock.getMethod()));
                });

        proxyServer.start(config, activeProxyMocks);
    }

    public boolean isProxyServerModeEnabled(final MockedServerConfigDTO config) {
        return BooleanUtils.toBoolean(config.getNativeProperties().get(GeneralUtils.PROXY_SERVER_ENABLED_PARAM));
    }

    @Transactional
    void invokeAndDetachData(final List<RestfulMock> mocks) {

        mocks.stream().forEach(m -> {
            // Invoke lazily Loaded rules and definitions whilst in this active transaction before
            // the entity is detached below.
            m.getRules().size();
            m.getDefinitions().size();

            // Important!
            // Detach all JPA entity beans from EntityManager Context, so they can be
            // continually accessed again here as a simple data bean
            // within each request to the mocked REST endpoint.
            restfulMockDAO.detach(m);
        });

    }

    // Expects RestfulMock to be detached
    void buildWebSocketEndpoints(final List<RestfulMock> mocks) {

        //
        // Define all web socket routes first as the Spark framework requires this
        mocks.stream().forEach(m -> {

            if (RestMockTypeEnum.PROXY_WS.equals(m.getMockType())) {
                // Create an echo service instance per web socket route, as we need to hold the path as state within this.
                final String path = buildUserPath(m);
                Spark.webSocket(path, new SparkWebSocketEchoService(m.getExtId(), path, m.getWebSocketTimeoutInMillis(), m.isProxyPushIdOnConnect(), webSocketService));
            }
        });
    }

    // Expects RestfulMock to be detached
    void buildSSEEndpoints(final List<RestfulMock> mocks) throws MockServerException {

        mocks.stream().forEach(m -> {

            if (RestMockTypeEnum.PROXY_SSE.equals(m.getMockType())) {

                // Remove any suspended rule or sequence responses
                removeSuspendedResponses(m);

                // NOTE, Java Spark does not currently provide support for NIO SSE. This code therefore BLOCKS the request
                // thread until the connection is closed by either party.
                Spark.get(buildUserPath(m), (req, res) -> processSSERequest(m, req, res));

            }

        });

    }

    // Expects RestfulMock to be detached
    List<RestfulMock> buildRESTEndpoints(final List<RestfulMock> mocks) throws MockServerException {

        final List<RestfulMock> activeRestfulMocks = new ArrayList<>();

        mocks.stream().forEach( m -> {

            if (RestMockTypeEnum.PROXY_HTTP.equals(m.getMockType())
                    || RestMockTypeEnum.SEQ.equals(m.getMockType())
                    || RestMockTypeEnum.RULE.equals(m.getMockType())) {

                // Remove any suspended rule or sequence responses
                removeSuspendedResponses(m);

                final String path = buildUserPath(m);
                final RestMethodEnum method = m.getMethod();

                activeRestfulMocks.add(m);

                switch (method) {
                    case GET:
                        Spark.get(path, (req, res) -> processRequest(m, req, res));
                        break;
                    case POST:
                        Spark.post(path, (req, res) -> processRequest(m, req, res));
                        break;
                    case PUT:
                        Spark.put(path, (req, res) -> processRequest(m, req, res));
                        break;
                    case DELETE:
                        Spark.delete(path, (req, res) -> processRequest(m, req, res));
                        break;
                    case PATCH:
                        Spark.patch(path, (req, res) -> processRequest(m, req, res));
                        break;
                    default:
                        throw new MockServerException("Unsupported mock definition method type : " + m.getMethod());
                }

            }

        });

        return activeRestfulMocks;
    }

    private void applyTrafficLogging(final MockedServerConfigDTO config) {

        final boolean logMockCalls =
                Boolean.valueOf(config.getNativeProperties().getOrDefault(GeneralUtils.LOG_MOCK_CALLS_PARAM, Boolean.FALSE.toString()));

        // Live logging filter
        Spark.before((request, response) -> {

            if (request.headers().contains(GeneralUtils.PROXY_MOCK_INTERCEPT_HEADER)) {
                return;
            }

            request.attribute(GeneralUtils.LOG_REQ_ID, GeneralUtils.generateUUID());

            final Map<String, String> reqHeaders = request.headers()
                    .stream()
                    .collect(Collectors.toMap(h -> h, h -> request.headers(h)));

            if (logMockCalls)
                mockTrafficLogger.info(LiveLoggingUtils.buildLiveLogInboundFileEntry(request.attribute(GeneralUtils.LOG_REQ_ID), request.requestMethod(), request.pathInfo(), request.contentType(), reqHeaders, request.body(), false));

            mockLogFeedHandler.broadcast(LiveLoggingUtils.buildLiveLogInboundDTO(request.attribute(GeneralUtils.LOG_REQ_ID), request.requestMethod(), request.pathInfo(), request.contentType(), reqHeaders, request.body(), false));
        });

        Spark.afterAfter((request, response) -> {

            if (request.headers().contains(GeneralUtils.PROXY_MOCK_INTERCEPT_HEADER)) {
                return;
            }

            final Map<String, String> respHeaders = response.raw().getHeaderNames()
                    .stream()
                    .collect(Collectors.toMap(h -> h, h -> response.raw().getHeader(h)));

            if (logMockCalls)
                mockTrafficLogger.info(LiveLoggingUtils.buildLiveLogOutboundFileEntry(request.attribute(GeneralUtils.LOG_REQ_ID), response.raw().getStatus(), response.raw().getContentType(), respHeaders, response.body(), false, false));

            mockLogFeedHandler.broadcast(LiveLoggingUtils.buildLiveLogOutboundDTO(request.attribute(GeneralUtils.LOG_REQ_ID), response.raw().getStatus(), response.raw().getContentType(), respHeaders, response.body(), false, false));
        });

    }

    /**
     * HACK
     *
     * Spark.afterAfter does not handle 404 always returning a 200 for some reason.
     * Also Spark.notFound is not working either, so defaulting all other routes to 404
     * as a work around
     *
     */
    private void applyDefaultRoutes404Hack() {

        Spark.head("*", (request, response) -> {
            response.status(404);
            return "Mock not found";
        });

        Spark.get("*", (request, response) -> {

            if (isWebSocketUpgradeRequest(request)) {
                response.status(200);
                return null;
            }

            response.status(404);
            return "Mock not found";
        });

        Spark.post("*", (request, response) -> {
            response.status(404);
            return "Mock not found";
        });

        Spark.put("*", (request, response) -> {
            response.status(404);
            return "Mock not found";
        });

        Spark.delete("*", (request, response) -> {
            response.status(404);
            return "Mock not found";
        });

        Spark.patch("*", (request, response) -> {
            response.status(404);
            return "Mock not found";
        });

    }

    private boolean isWebSocketUpgradeRequest(final Request request) {

        final Set<String> headerNames = request.headers();

        return headerNames.contains("Upgrade")
                && headerNames.contains("Sec-WebSocket-Key")
                && "websocket".equalsIgnoreCase(request.headers("Upgrade"));
    }

    private void setDeployedMocks(final List<RestfulMock> activeMocks) {

        final Map<Long, Date> tempMap = new HashMap<>();
        activeMocks
                .stream()
                .forEach(m -> tempMap.put(m.getId(), m.getLastUpdated()));

        synchronized (monitor) {
            deployedMocks = tempMap;
        }
    }

    private void clearDeployedMocks() {
        synchronized (monitor) {
            deployedMocks = new HashMap<>();
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

        res.status(outcome.getHttpStatusCode());
        res.type(outcome.getResponseContentType());

        // Apply any response headers
        outcome.getHeaders().entrySet().forEach(e ->
            res.header(e.getKey(), e.getValue())
        );

        final String response = inboundParamMatchService.enrichWithInboundParamMatches(req, outcome.getResponseBody());

        return StringUtils.defaultIfBlank(response,"");
    }

    String processSSERequest(final RestfulMock mock, final Request req, final Response res) throws IOException {

        serverSideEventService.register(buildUserPath(mock), mock.getSseHeartBeatInMillis(), mock.isProxyPushIdOnConnect(), res);

        return null;
    }

    RestfulResponseDTO getDefault(final RestfulMock restfulMock) {

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

        final Iterator<RestfulMockDefinitionOrder> definitionsIter =  mock.getDefinitions().iterator();



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

    void clearState() {

        // Proxy related state
        webSocketService.clearSession();
        proxyService.clearAllSessions();
        mockOrderingCounterService.clearState();
        serverSideEventService.clearState();

    }

    void handleCORS(final MockedServerConfigDTO config) {

        final String enableCors = config.getNativeProperties().get(GeneralUtils.ENABLE_CORS_PARAM);

        if (!Boolean.TRUE.toString().equalsIgnoreCase(enableCors)) {
            return;
        }

        Spark.options("/*", (request, response) -> {

            final String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");

            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }

            final String accessControlRequestMethod = request.headers("Access-Control-Request-Method");

            if (accessControlRequestMethod != null) {
                response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }

            return HttpStatus.OK.name();
        });

        Spark.before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
//            response.header("Access-Control-Request-Method", "GET,PUT,POST,DELETE,OPTIONS");
//            response.header("Access-Control-Allow-Headers", "*");
//            response.header("Access-Control-Allow-Credentials", "true");
        });

    }

    public String buildUserPath(final RestfulMock mock) {

        if (!SmockinUserRoleEnum.SYS_ADMIN.equals(mock.getCreatedBy().getRole())) {
            return File.separator + mock.getCreatedBy().getCtxPath() + mock.getPath();
        }

        return mock.getPath();
    }

}
