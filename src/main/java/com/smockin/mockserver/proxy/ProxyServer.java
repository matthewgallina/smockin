package com.smockin.mockserver.proxy;

import com.smockin.admin.dto.HttpClientCallDTO;
import com.smockin.admin.dto.response.HttpClientResponseDTO;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.mockserver.dto.MockServerState;
import com.smockin.mockserver.engine.BaseServerEngine;
import com.smockin.mockserver.exception.MockServerException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.apache.http.HttpStatus;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

@Service
public class ProxyServer implements BaseServerEngine<Integer[], Map<String, List<RestMethodEnum>>> {

    private final Logger logger = LoggerFactory.getLogger(ProxyServer.class);

    private HttpProxyServer httpProxyServer;
    private final Object monitor = new Object();
    private MockServerState serverState = new MockServerState(false, 0);

    @Autowired
    private ProxyServerUtils proxyServerUtils;

    @Override
    public void start(final Integer[] ports, final Map<String, List<RestMethodEnum>> activeMocks) {

        try {

            final int proxyPort = ports[0];
            final int mockServerPort = ports[1];

            httpProxyServer = DefaultHttpProxyServer.bootstrap()
                    .withPort(proxyPort)
                    .withManInTheMiddle(new SmockinSelfSignedMitmManager())
                    .withFiltersSource(new HttpFiltersSourceAdapter() {

                        @Override
                        public int getMaximumRequestBufferSizeInBytes() {
                            return 512 * 1024;
                        }

                        public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {
                            logger.debug("filterRequest called");

                            final LittleProxyContext context = new LittleProxyContext();

                            return new HttpFiltersAdapter(originalRequest) {

                                @Override
                                public HttpResponse proxyToServerRequest(HttpObject httpObject) {
                                    logger.debug("proxyToServerRequest called");

                                    if (httpObject instanceof FullHttpRequest) {

                                        final FullHttpRequest request = (FullHttpRequest) httpObject;

                                        try {

                                            if (originalRequest.getDecoderResult().isFailure())
                                                return proxyServerUtils.buildBadResponse();

                                            proxyServerUtils.debugInboundRequest(originalRequest);

                                            if (proxyServerUtils.excludeInboundMethod(originalRequest.getMethod().name()))
                                                return null;

                                            if (!proxyServerUtils.mockMatchFound(originalRequest, activeMocks))
                                                return null;

                                            context.setUseMock(true);
                                            context.getRequestHeaders().addAll(originalRequest.headers().entries());

                                            if (request.getMethod() == HttpMethod.POST)
                                                context.setRequestBody(request.content().toString(CharsetUtil.UTF_8));

                                        } catch (MalformedURLException e) {
                                            logger.error("Error parsing inbound URL", e);
                                        } catch (IOException e) {
                                            logger.error("Error using mock substitute", e);
                                        }

                                    }

                                    return null;
                                }

                                @Override
                                public HttpObject proxyToClientResponse(final HttpObject httpObject) {
                                    logger.debug("proxyToClientResponse called");

                                    // Re-use response if already retreived from mock server
                                    if (context.getClientResponse() != null) {
                                        return context.getClientResponse();
                                    }

                                    proxyServerUtils.debugInboundRequest(originalRequest);

                                    if (!context.isUseMock()) {
                                        return httpObject;
                                    }

                                    try {

                                        final URL inboundUrl = new URL(proxyServerUtils.fixProtocolWithDummyPrefix(originalRequest.getUri()));
                                        final RestMethodEnum inboundMethod = RestMethodEnum.findByName(originalRequest.getMethod().name());
                                        final HttpClientCallDTO dto = proxyServerUtils.buildRequestDTO(context, inboundMethod, proxyServerUtils.buildMockUrl(inboundUrl, mockServerPort));

                                        // Store response from mock server for re-use
                                        final HttpClientResponseDTO response = proxyServerUtils.callMock(dto);

                                        if (response.getStatus() == HttpStatus.SC_TEMPORARY_REDIRECT) {
                                            return httpObject;
                                        }

                                        context.setClientResponse(proxyServerUtils.buildResponse(response));

                                        return context.getClientResponse();

                                    } catch (MalformedURLException e) {
                                        logger.error("Error parsing inbound URL", e);
                                    } catch (IOException e) {
                                        logger.error("Error using mock substitute", e);
                                    }

                                    return httpObject;
                                }
                            };
                        }
                    })
                    .start();

            synchronized (monitor) {
                serverState.setRunning(true);
                serverState.setPort(proxyPort);
            }

        } catch (Throwable ex) {
            throw new MockServerException(ex);
        }

    }

    @Override
    public void shutdown() {

        try {

            synchronized (monitor) {

                if (httpProxyServer == null || !serverState.isRunning()) {
                    return;
                }

                httpProxyServer.stop();
                serverState.setRunning(false);
            }

        } catch (Throwable ex) {
            throw new MockServerException(ex);
        }

    }

    @Override
    public MockServerState getCurrentState() {
        synchronized (monitor) {
            return serverState;
        }
    }

}
