package com.smockin.mockserver.proxy;

import com.smockin.admin.dto.HttpClientCallDTO;
import com.smockin.admin.dto.response.HttpClientResponseDTO;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.mockserver.dto.MockServerState;
import com.smockin.mockserver.engine.BaseServerEngine;
import com.smockin.mockserver.exception.MockServerException;
import com.smockin.utils.HttpClientUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;
import org.littleshoot.proxy.impl.ProxyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;

@Service
public class ProxyServer implements BaseServerEngine<Integer, Map<String, List<RestMethodEnum>>> {

    private final Logger logger = LoggerFactory.getLogger(ProxyServer.class);

    private HttpProxyServer httpProxyServer;
    private final Object monitor = new Object();
    private MockServerState serverState = new MockServerState(false, 0);

    @Override
    public void start(final Integer proxyPort, final Map<String, List<RestMethodEnum>> activeMocks) {

        try {

            httpProxyServer = DefaultHttpProxyServer.bootstrap()
                    .withPort(proxyPort)
                    .withFiltersSource(new HttpFiltersSourceAdapter() {

                        @Override
                        public int getMaximumRequestBufferSizeInBytes() {
                            return 512 * 1024;
                        }

                        public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {

                            return new HttpFiltersAdapter(originalRequest) {

                                @Override
                                public HttpObject proxyToClientResponse(HttpObject httpObject) {

                                    if (originalRequest.getDecoderResult().isFailure()) {
                                        return buildBadResponse();
                                    }

                                    if (logger.isDebugEnabled()) {
                                        logger.debug("Inbound URI " + originalRequest.getUri());
                                        logger.debug("Inbound method " + originalRequest.getMethod());
                                        logger.debug("Inbound HTTP version " + originalRequest.getProtocolVersion());
                                    }

                                    try {

                                        final URL url = new URL(originalRequest.getUri());

                                        // TEMP
                                        if (logger.isDebugEnabled()) {
                                            logger.debug("path " + url.getPath());
                                            logger.debug("query " + url.getQuery());

                                            activeMocks.entrySet().stream().forEach(e -> {
                                                logger.debug("k " + e.getKey());
                                                logger.debug("v " + e.getValue());
                                            });
                                        }
                                        // TEMP

                                        final Optional<Map.Entry<String, List<RestMethodEnum>>> pathMatchOpt = checkForMockPathMatch(url, activeMocks);

                                        if (!pathMatchOpt.isPresent()) {
                                            return httpObject;
                                        }

                                        logger.debug("matched path");

                                        final Optional<RestMethodEnum> restMethodOpt = checkForMockMethodMatch(originalRequest.getMethod().name(), pathMatchOpt.get());

                                        if (!restMethodOpt.isPresent()) {
                                            return httpObject;
                                        }

                                        logger.debug("matched method");

                                        final HttpClientCallDTO dto = new HttpClientCallDTO();

                                        if (RestMethodEnum.POST.equals(restMethodOpt.get())
                                                || RestMethodEnum.PUT.equals(restMethodOpt.get())) {

                                            final HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(originalRequest);
                                            final List<InterfaceHttpData> bodyList = decoder.getBodyHttpDatas();

                                            logger.debug("body size : " + bodyList.size());

                                            //                                        dto.setBody(); // TODO how to get
                                        }

                                        //
                                        // Use mock
                                        dto.setUrl(originalRequest.getUri());
//                                        dto.setHeaders(); // TODO how to get headers
                                        dto.setMethod(restMethodOpt.get());

                                        return buildResponse(callMock(dto));

                                    } catch (MalformedURLException e) {
                                        logger.error("Error parsing inbound URL", e);
                                    } catch (IOException e) {
                                        logger.error("Error using mock substitute");
                                    }

//                                    if (originalRequest.getUri().indexOf("?name=bob") > -1) {
//                                        return buildBadReqResponse();
//                                    }

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

    HttpResponse buildBadResponse() {
        return buildResponse(new HttpClientResponseDTO(400, "text/html; charset=UTF-8", new HashMap<>() ,""));
    }

    HttpResponse buildResponse(final HttpClientResponseDTO dto) {
        logger.debug("buildResponse called");

        final byte[] bytes = dto.getBody().getBytes(Charset.forName("UTF-8"));
        final ByteBuf content = Unpooled.copiedBuffer(bytes);
        HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(dto.getStatus()), content);
        response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, bytes.length);
        response.headers().set("Content-Type", dto.getContentType());
        response.headers().set("Date", ProxyUtils.formatDate(new Date()));
        response.headers().set(HttpHeaders.Names.CONNECTION, "close");

        dto.getHeaders()
            .entrySet()
            .stream()
            .forEach(h -> response.headers().set(h.getKey(), h.getValue()));

        return response;
    }

    Optional<Map.Entry<String, List<RestMethodEnum>>> checkForMockPathMatch(final URL url, final Map<String, List<RestMethodEnum>> activeMocks) {
        logger.debug("checkForMockPathMatch called");

        return activeMocks.entrySet()
                .stream()
                .filter(e -> e.getKey().equalsIgnoreCase(url.getPath()))
                .findFirst();
    }

    Optional<RestMethodEnum> checkForMockMethodMatch(final String method, final Map.Entry<String, List<RestMethodEnum>> pathMatch) {
        logger.debug("checkForMockMethodMatch called");

        return pathMatch.getValue()
                .stream()
                .filter(m -> m.name().equalsIgnoreCase(method))
                .findFirst();
    }

    HttpClientResponseDTO callMock(final HttpClientCallDTO dto) throws IOException {

        switch (dto.getMethod()) {
            case GET:
                return HttpClientUtils.get(dto);
            case POST:
                return HttpClientUtils.post(dto);
            case PUT:
                return HttpClientUtils.put(dto);
            case DELETE:
                return HttpClientUtils.delete(dto);
            case PATCH:
                return HttpClientUtils.patch(dto);
            default:
                return null;
        }

    }

}
