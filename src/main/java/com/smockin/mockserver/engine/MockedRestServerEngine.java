package com.smockin.mockserver.engine;

import com.smockin.admin.persistence.dao.RestfulMockDAO;
import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.persistence.entity.RestfulMockDefinitionOrder;
import com.smockin.admin.persistence.entity.RestfulMockDefinitionRule;
import com.smockin.admin.persistence.enums.RestMockTypeEnum;
import com.smockin.mockserver.dto.MockServerState;
import com.smockin.mockserver.dto.MockedServerConfigDTO;
import com.smockin.mockserver.exception.MockServerException;
import com.smockin.mockserver.service.*;
import com.smockin.mockserver.service.dto.RestfulResponseDTO;
import com.smockin.mockserver.service.ws.SparkWebSocketEchoService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spark.Request;
import spark.Response;
import spark.Spark;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by mgallina.
 */
@Service
public class MockedRestServerEngine implements MockServerEngine<MockedServerConfigDTO, List<RestfulMock>> {

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


    private final Object monitor = new Object();
    private MockServerState serverState = new MockServerState(false, 0);

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
        buildRESTEndpoints(mocks);

        // Next handle all HTTP SSE web service routes
        buildSSEEndpoints(mocks);

//        buildIndex(mocks);

        initServer(config.getPort());

    }

    public MockServerState getCurrentState() throws MockServerException {
        synchronized (monitor) {
            return serverState;
        }
    }

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

    /*
    @Transactional
    void buildIndex(final List<RestfulMock> mocks) {
        logger.debug("buildIndex called");

        final StringBuilder sb = new StringBuilder("<h2>Smockin REST Service Index</h2>");
        sb.append("<br /><br />");

        // NOTE JPA entity beans are still attached at this stage (See buildEndpoints() below).
        for (RestfulMock m : mocks) {

            if (RestMockTypeEnum.PROXY_WS.equals(m.getMockType())) {
                continue;
            }

            sb.append(m.getMethod());
            sb.append(" ");
            sb.append(m.getPath());
            sb.append("<br /><br />");
        }

        Spark.get("/", (req, res) -> {
            res.type("text/html");
            return sb.toString();
        });

    }
    */

    @Transactional
    void invokeAndDetachData(final List<RestfulMock> mocks) {

        for (RestfulMock mock : mocks) {

            // Invoke lazily Loaded rules and definitions whilst in this active transaction before
            // the entity is detached below.
            mock.getRules().size();
            mock.getDefinitions().size();

            // Important!
            // Detach all JPA entity beans from EntityManager Context, so they can be
            // continually accessed again here as a simple data bean
            // within each request to the mocked REST endpoint.
            restfulMockDAO.detach(mock);
        }

    }

    // Expects RestfulMock to be detached
    void buildWebSocketEndpoints(final List<RestfulMock> mocks) {

        //
        // Define all web socket routes first as the Spark framework requires this
        for (RestfulMock mock : mocks) {

            if (!RestMockTypeEnum.PROXY_WS.equals(mock.getMockType())) {
                continue;
            }

            // Create an echo service instance per web socket route, as we need to hold the path as state within this.
            Spark.webSocket(mock.getPath(), new SparkWebSocketEchoService(mock.getExtId(), mock.getPath(), mock.getWebSocketTimeoutInMillis(), mock.isProxyPushIdOnConnect(), webSocketService));
        }

    }

    // Expects RestfulMock to be detached
    void buildSSEEndpoints(final List<RestfulMock> mocks) throws MockServerException {

        for (RestfulMock mock : mocks) {

            if (!RestMockTypeEnum.PROXY_SSE.equals(mock.getMockType())) {
                continue;
            }

            // Remove any suspended rule or sequence responses
            removeSuspendedResponses(mock);

            // NOTE, Java Spark does not currently provide support for NIO SSE. This code therefore BLOCKS the request
            // thread until the connection is closed by either party.
            Spark.get(mock.getPath(), (req, res) -> {
                return processSSERequest(mock, req, res);
            });

        }
    }

    // Expects RestfulMock to be detached
    void buildRESTEndpoints(final List<RestfulMock> mocks) throws MockServerException {

        for (RestfulMock mock : mocks) {

            if (!RestMockTypeEnum.PROXY_HTTP.equals(mock.getMockType())
                    && !RestMockTypeEnum.SEQ.equals(mock.getMockType())
                    && !RestMockTypeEnum.RULE.equals(mock.getMockType())) {
                continue;
            }

            // Remove any suspended rule or sequence responses
            removeSuspendedResponses(mock);

            switch (mock.getMethod()) {
                case GET:

                    Spark.get(mock.getPath(), (req, res) -> {
                        return processRequest(mock, req, res);
                    });

                    break;
                case POST:

                    Spark.post(mock.getPath(), (req, res) -> {
                        return processRequest(mock, req, res);
                    });

                    break;
                case PUT:

                    Spark.put(mock.getPath(), (req, res) -> {
                        return processRequest(mock, req, res);
                    });

                    break;
                case DELETE:

                    Spark.delete(mock.getPath(), (req, res) -> {
                        return processRequest(mock, req, res);
                    });

                    break;

                case PATCH:

                    Spark.patch(mock.getPath(), (req, res) -> {
                        return processRequest(mock, req, res);
                    });

                    break;
                default:
                    throw new MockServerException("Unsupported mock definition method type : " + mock.getMethod());
            }

        }

    }

    String processRequest(final RestfulMock mock, final Request req, final Response res) {

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
        for (Map.Entry<String, String> e : outcome.getHeaders().entrySet()) {
            res.header(e.getKey(), e.getValue());
        }

        final String response = inboundParamMatchService.enrichWithInboundParamMatches(req, outcome.getResponseBody());

        return StringUtils.defaultIfBlank(response,"");
    }

    String processSSERequest(final RestfulMock mock, final Request req, final Response res) throws IOException {

        serverSideEventService.register(mock.getPath(), mock.getSseHeartBeatInMillis(), mock.isProxyPushIdOnConnect(), res);

        return null;
    }

    RestfulResponseDTO getDefault(final RestfulMock restfulMock) {

        if (RestMockTypeEnum.PROXY_HTTP.equals(restfulMock.getMockType())) {
            return new RestfulResponseDTO(404);
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
        proxyService.clearSession();
        mockOrderingCounterService.clearState();
        serverSideEventService.clearState();

    }

    void handleCORS(final MockedServerConfigDTO config) {

        final String enableCors = config.getNativeProperties().get("ENABLE_CORS");

        if (!"TRUE".equals(enableCors)) {
            return;
        }

        Spark.options("/*", (request, response) -> {

            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }

            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }

            return "OK";
        });

        Spark.before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Request-Method", "GET,PUT,POST,DELETE,OPTIONS");
            response.header("Access-Control-Allow-Headers", "*");
            response.header("Access-Control-Allow-Credentials", "true");
            // Note: this may or may not be necessary...
//            response.type("application/json");
        });

    }

}
