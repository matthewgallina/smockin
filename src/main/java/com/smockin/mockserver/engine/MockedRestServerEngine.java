package com.smockin.mockserver.engine;

import com.smockin.admin.enums.UserModeEnum;
import com.smockin.admin.persistence.dao.RestfulMockDAO;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.admin.service.SmockinUserService;
import com.smockin.admin.websocket.LiveLoggingHandler;
import com.smockin.mockserver.dto.*;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Created by mgallina.
 */
@Service
@Transactional(readOnly = true)
public class MockedRestServerEngine implements MockServerEngine<MockedServerConfigDTO, ProxyForwardConfigDTO> {

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

    private final Object serverStateMonitor = new Object();
    private final Object blockingMonitor = new Object();
private boolean liveBlockActive;
private LiveloggingUserOverrideResponse userOverrideResponse = null;

    private MockServerState serverState = new MockServerState(false, 0);

    private AtomicBoolean liveBlockingModeEnabled = new AtomicBoolean();
    private AtomicReference<List<LiveBlockPath>> liveBlockPathsRef = new AtomicReference<>(new ArrayList<>());


    @Override
    public void start(final MockedServerConfigDTO config,
                      final ProxyForwardConfigDTO proxyForwardConfigDTO) throws MockServerException {

        logger.debug("start called");

        initServerConfig(config);

        final boolean isMultiUserMode = UserModeEnum.ACTIVE.equals(smockinUserService.getUserMode());

        // Define all web socket routes first as the Spark framework requires this
        buildWebSocketEndpoints(isMultiUserMode);

        // Handle Cross-Origin Resource Sharing (CORS) support
        handleCORS(config);

        filterProxyConfigMappings(proxyForwardConfigDTO);

        // Next handle all HTTP RESTFul web service routes
        buildGlobalHttpEndpointsHandler(isMultiUserMode, config, proxyForwardConfigDTO);

        applyTrafficLogging(proxyForwardConfigDTO.isProxyMode());

        initServer(config.getPort());
    }

    @Override
    public MockServerState getCurrentState() throws MockServerException {
        synchronized (serverStateMonitor) {
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

            synchronized (serverStateMonitor) {
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

            synchronized (serverStateMonitor) {
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

    private void applyTrafficLogging(final boolean isUsingProxyMode) {

        // Live logging filter
        Spark.before((request, response) -> {

            final String traceId = GeneralUtils.generateUUID();

            request.attribute(GeneralUtils.LOG_REQ_ID, traceId);
            response.raw()
                    .addHeader(GeneralUtils.LOG_REQ_ID, traceId);

            final Map<String, String> reqHeaders = request
                    .headers()
                    .stream()
                    .collect(Collectors.toMap(h -> h, h -> request.headers(h)));

            reqHeaders.put(GeneralUtils.LOG_REQ_ID, traceId);

            liveLoggingHandler.broadcast(
                    LiveLoggingUtils.buildLiveLogInboundDTO(
                        request.attribute(GeneralUtils.LOG_REQ_ID),
                        request.requestMethod(),
                        request.pathInfo(),
                        reqHeaders,
                        request.body(),
                        isUsingProxyMode,
                        GeneralUtils.extractAllRequestParams(request)));
        });

        Spark.afterAfter((request, response) -> {

            if (serverSideEventService.SSE_EVENT_STREAM_HEADER.equals(response.raw().getHeader(HttpHeaders.CONTENT_TYPE))) {
                return;
            }

            final Map<String, String> respHeaders = mockedRestServerEngineUtils.extractResponseHeadersAsMap(response);

            respHeaders.put(GeneralUtils.LOG_REQ_ID, request.attribute(GeneralUtils.LOG_REQ_ID));

            liveLoggingHandler.broadcast(
                    LiveLoggingUtils.buildLiveLogOutboundDTO(
                        request.attribute(GeneralUtils.LOG_REQ_ID),
                        response.raw().getStatus(),
                        respHeaders,
                        response.body(),
                        isUsingProxyMode));
        });

    }

    void buildGlobalHttpEndpointsHandler(final boolean isMultiUserMode,
                                         final MockedServerConfigDTO serverConfig,
                                         final ProxyForwardConfigDTO proxyForwardConfig) {
        logger.debug("buildGlobalHttpEndpointsHandler called");

        Spark.head(GeneralUtils.PATH_WILDCARD, (request, response) ->
                mockedRestServerEngineUtils.loadMockedResponse(request, response, isMultiUserMode, serverConfig, proxyForwardConfig)
                        .orElseGet(() -> handleNotFoundResponse(response)));

        Spark.get(GeneralUtils.PATH_WILDCARD, (request, response) -> {

            if (isWebSocketUpgradeRequest(request)) {
                response.status(HttpStatus.OK.value());
                return null;
            }

            String responseBody = mockedRestServerEngineUtils.loadMockedResponse(request, response, isMultiUserMode, serverConfig, proxyForwardConfig)
                    .orElseGet(() -> handleNotFoundResponse(response));

            if (blockLoggingResponse(request, response, proxyForwardConfig.isProxyMode())) {

                logger.debug("Endpoint match made. Blocking response...");

                synchronized (blockingMonitor) {

                    this.liveBlockActive = true;

                    while (liveBlockActive) {

                        blockingMonitor.wait();

                        if (userOverrideResponse != null) {

                            userOverrideResponse.getResponseHeaders().entrySet().forEach(h ->
                                response.header(h.getKey(), h.getValue()));

                            response.type(userOverrideResponse.getContentType());
                            response.status(userOverrideResponse.getStatus());
                            response.body(userOverrideResponse.getBody());
                            responseBody = userOverrideResponse.getBody();
                        }

                    }
                }
            }

            return responseBody;
        });

        Spark.post(GeneralUtils.PATH_WILDCARD, (request, response) ->
                mockedRestServerEngineUtils.loadMockedResponse(request, response, isMultiUserMode, serverConfig, proxyForwardConfig)
                        .orElseGet(() -> handleNotFoundResponse(response)));

        Spark.put(GeneralUtils.PATH_WILDCARD, (request, response) ->
                mockedRestServerEngineUtils.loadMockedResponse(request, response, isMultiUserMode, serverConfig, proxyForwardConfig)
                        .orElseGet(() -> handleNotFoundResponse(response)));

        Spark.delete(GeneralUtils.PATH_WILDCARD, (request, response) ->
                mockedRestServerEngineUtils.loadMockedResponse(request, response, isMultiUserMode, serverConfig, proxyForwardConfig)
                        .orElseGet(() -> handleNotFoundResponse(response)));

        Spark.patch(GeneralUtils.PATH_WILDCARD, (request, response) ->
                mockedRestServerEngineUtils.loadMockedResponse(request, response, isMultiUserMode, serverConfig, proxyForwardConfig)
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

    boolean blockLoggingResponse(final Request request,
                          final Response response,
                          final boolean proxyMode) {

        if (logger.isDebugEnabled()) {
            logger.debug("liveBlockEnabled: " + liveBlockingModeEnabled.get());
        }

        if (this.liveBlockingModeEnabled.get()) {

            // Check if this call matches any endpoints that should be blocked...
            if (liveBlockPathsRef.get()
                    .stream()
                    .anyMatch(p ->
                            p.getMethod().name().equalsIgnoreCase(request.requestMethod())
                                    && GeneralUtils.matchPaths(p.getPath(), request.pathInfo()))) {

                // If so then send response details to live logging console via WS...
                liveLoggingHandler.broadcast(
                        LiveLoggingUtils.buildLiveLogInterceptedResponseDTO(
                                request.attribute(GeneralUtils.LOG_REQ_ID),
                                response.raw().getStatus(),
                                mockedRestServerEngineUtils.extractResponseHeadersAsMap(response),
                                response.body(),
                                proxyMode));

                return true;
            }

        }

        return false;
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

        Spark.before((request, response) ->
            response.header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, GeneralUtils.PATH_WILDCARD));

    }

    void filterProxyConfigMappings(final ProxyForwardConfigDTO proxyForwardConfig) {

        proxyForwardConfig.setProxyForwardMappings(
            proxyForwardConfig.getProxyForwardMappings()
            .stream()
            .filter(p ->
                    !p.isDisabled())
            .collect(Collectors.toList()));

    }

    public void releaseBlockedLiveLoggingResponse(final int status,
                                                  final String contentType,
                                                  final Map<String, String> responseHeaders,
                                                  final String body) {

        logger.debug("releasing blocked response...");

        synchronized (blockingMonitor) {
            this.liveBlockActive = false;
            userOverrideResponse = new LiveloggingUserOverrideResponse(status, contentType, responseHeaders, body);
            blockingMonitor.notify();
        }

        logger.debug("block released");
    }

    public void updateLiveBlockingMode(final boolean liveBlockEnabled) {

        if (logger.isDebugEnabled())
            logger.debug("Live blocking enabled: " + liveBlockEnabled);

        this.liveBlockingModeEnabled.set(liveBlockEnabled);
    }

    public void addPathToLiveBlocking(final RestMethodEnum method,
                                      final String path) {

        if (logger.isDebugEnabled())
            logger.debug("Adding blocking rule for: " + method.name() + " " + path);

        liveBlockPathsRef.get().add(new LiveBlockPath(method, path));
    }

    public void removePathFromLiveBlocking(final RestMethodEnum method,
                                           final String path) {

        if (logger.isDebugEnabled())
            logger.debug("Removing blocking rule for: " + method.name() + " " + path);

        liveBlockPathsRef.get().remove(new LiveBlockPath(method, path));
    }

    public void clearAllPathsFromLiveBlocking() {

        if (logger.isDebugEnabled())
            logger.debug("clearing down all blocking rules...");

        liveBlockPathsRef.get().clear();
    }

}
