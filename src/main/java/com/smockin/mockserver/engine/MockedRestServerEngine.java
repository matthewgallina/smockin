package com.smockin.mockserver.engine;

import com.smockin.admin.enums.UserModeEnum;
import com.smockin.admin.persistence.dao.RestfulMockDAO;
import com.smockin.admin.service.SmockinUserService;
import com.smockin.admin.websocket.LiveLoggingHandler;
import com.smockin.mockserver.dto.MockServerState;
import com.smockin.mockserver.dto.MockedServerConfigDTO;
import com.smockin.mockserver.exception.MockServerException;
import com.smockin.mockserver.service.*;
import com.smockin.mockserver.service.ws.SparkWebSocketEchoService;
import com.smockin.utils.GeneralUtils;
import com.smockin.utils.LiveLoggingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spark.Request;
import spark.Response;
import spark.Spark;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by mgallina.
 */
@Service
@Transactional
public class MockedRestServerEngine implements MockServerEngine<MockedServerConfigDTO, Optional<Void>> {

    private final Logger logger = LoggerFactory.getLogger(MockedRestServerEngine.class);

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
    private MockedRestServerEngineUtils mockedRestServerEngineUtils;

    @Autowired
    private LiveLoggingHandler liveLoggingHandler;

    @Autowired
    private SmockinUserService smockinUserService;


    private final Object monitor = new Object();
    private MockServerState serverState = new MockServerState(false, 0);
    private final String wildcardPath = "*";


    @Override
    public void start(final MockedServerConfigDTO config, final Optional opt) throws MockServerException {
        logger.debug("start called");

        initServerConfig(config);

        final boolean isMultiUserMode = UserModeEnum.ACTIVE.equals(smockinUserService.getUserMode());

        // Define all web socket routes first as the Spark framework requires this
        buildWebSocketEndpoints(isMultiUserMode);

        // Handle Cross-Origin Resource Sharing (CORS) support
        handleCORS(config);

        // Next handle all HTTP RESTFul web service routes
        buildGlobalHttpEndpointsHandler(isMultiUserMode);

        applyTrafficLogging();

        initServer(config.getPort());
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

        } catch (Throwable ex) {
            throw new MockServerException(ex);
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

    void buildWebSocketEndpoints(final boolean isMultiUserMode) {

        Spark.webSocket("/*", new SparkWebSocketEchoService(webSocketService, isMultiUserMode));
    }

    private void applyTrafficLogging() {

        // Live logging filter
        Spark.before((request, response) -> {

            if (request.headers().contains(GeneralUtils.PROXY_MOCK_INTERCEPT_HEADER)) {
                return;
            }

            final String traceId = GeneralUtils.generateUUID();

            request.attribute(GeneralUtils.LOG_REQ_ID, traceId);
            response.raw().addHeader(GeneralUtils.LOG_REQ_ID, traceId);

            final Map<String, String> reqHeaders = request.headers()
                    .stream()
                    .collect(Collectors.toMap(h -> h, h -> request.headers(h)));

            reqHeaders.put(GeneralUtils.LOG_REQ_ID, traceId);

            liveLoggingHandler.broadcast(LiveLoggingUtils.buildLiveLogInboundDTO(request.attribute(GeneralUtils.LOG_REQ_ID), request.requestMethod(), request.pathInfo(), reqHeaders, request.body(), false));
        });

        Spark.afterAfter((request, response) -> {

            if (request.headers().contains(GeneralUtils.PROXY_MOCK_INTERCEPT_HEADER)
                    || serverSideEventService.SSE_EVENT_STREAM_HEADER.equals(response.raw().getHeader(HttpHeaders.CONTENT_TYPE))) {
                return;
            }

            final Map<String, String> respHeaders = response.raw().getHeaderNames()
                    .stream()
                    .collect(Collectors.toMap(h -> h, h -> response.raw().getHeader(h)));

            respHeaders.put(GeneralUtils.LOG_REQ_ID, request.attribute(GeneralUtils.LOG_REQ_ID));

            liveLoggingHandler.broadcast(LiveLoggingUtils.buildLiveLogOutboundDTO(request.attribute(GeneralUtils.LOG_REQ_ID), response.raw().getStatus(), respHeaders, response.body(), false, false));
        });

    }

    void buildGlobalHttpEndpointsHandler(final boolean isMultiUserMode) {
        logger.debug("buildGlobalHttpEndpointsHandler called");

        Spark.head(wildcardPath, (request, response) ->
                mockedRestServerEngineUtils.loadMockedResponse(request, response, isMultiUserMode)
                        .orElseGet(() -> handleNotFoundResponse(response)));

        Spark.get(wildcardPath, (request, response) -> {

            if (isWebSocketUpgradeRequest(request)) {
                response.status(HttpStatus.OK.value());
                return null;
            }

            return mockedRestServerEngineUtils.loadMockedResponse(request, response, isMultiUserMode)
                    .orElseGet(() -> handleNotFoundResponse(response));
        });

        Spark.post(wildcardPath, (request, response) ->
                mockedRestServerEngineUtils.loadMockedResponse(request, response, isMultiUserMode)
                        .orElseGet(() -> handleNotFoundResponse(response)));

        Spark.put(wildcardPath, (request, response) ->
                mockedRestServerEngineUtils.loadMockedResponse(request, response, isMultiUserMode)
                        .orElseGet(() -> handleNotFoundResponse(response)));

        Spark.delete(wildcardPath, (request, response) ->
                mockedRestServerEngineUtils.loadMockedResponse(request, response, isMultiUserMode)
                        .orElseGet(() -> handleNotFoundResponse(response)));

        Spark.patch(wildcardPath, (request, response) ->
                mockedRestServerEngineUtils.loadMockedResponse(request, response, isMultiUserMode)
                        .orElseGet(() -> handleNotFoundResponse(response)));

    }

    private String handleNotFoundResponse(final Response response) {

        response.status(HttpStatus.NOT_FOUND.value());
        return "";
    }

    private boolean isWebSocketUpgradeRequest(final Request request) {

        final Set<String> headerNames = request.headers();

        return headerNames.contains(HttpHeaders.UPGRADE)
                && headerNames.contains("Sec-WebSocket-Key")
                && "websocket".equalsIgnoreCase(request.headers(HttpHeaders.UPGRADE));
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

            final String accessControlRequestHeaders = request.headers(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);

            if (accessControlRequestHeaders != null) {
                response.header(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, accessControlRequestHeaders);
            }

            final String accessControlRequestMethod = request.headers(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);

            if (accessControlRequestMethod != null) {

                response.header(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, accessControlRequestMethod);
            }

            return HttpStatus.OK.name();
        });

        Spark.before((request, response) -> {

            response.header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, wildcardPath);
        });

    }

}
