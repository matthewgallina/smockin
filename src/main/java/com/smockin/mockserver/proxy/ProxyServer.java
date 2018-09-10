package com.smockin.mockserver.proxy;

import com.smockin.admin.dto.HttpClientCallDTO;
import com.smockin.admin.dto.response.HttpClientResponseDTO;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.mockserver.dto.MockServerState;
import com.smockin.mockserver.engine.BaseServerEngine;
import com.smockin.mockserver.exception.MockServerException;
import com.smockin.utils.HttpClientUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
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
import java.util.stream.Collectors;

@Service
public class ProxyServer implements BaseServerEngine<Integer[], Map<String, List<RestMethodEnum>>> {

    private final Logger logger = LoggerFactory.getLogger(ProxyServer.class);

    private static final String MOCK_SERVER_HOST = "http://localhost:";

    private HttpProxyServer httpProxyServer;
    private final Object monitor = new Object();
    private MockServerState serverState = new MockServerState(false, 0);

    @Override
    public void start(final Integer[] ports, final Map<String, List<RestMethodEnum>> activeMocks) {

        try {

            final int proxyPort = ports[0];
            final int mockServerPort = ports[1];

            httpProxyServer = DefaultHttpProxyServer.bootstrap()
                    .withPort(proxyPort)
                    .withFiltersSource(new HttpFiltersSourceAdapter() {

                        @Override
                        public int getMaximumRequestBufferSizeInBytes() {
                            return 512 * 1024;
                        }

                        public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {

                            final LittleProxyContext context = new LittleProxyContext();

                            return new HttpFiltersAdapter(originalRequest) {

                                @Override
                                public HttpResponse proxyToServerRequest(HttpObject httpObject) {

                                    if (httpObject instanceof FullHttpRequest){
                                        final FullHttpRequest request = (FullHttpRequest) httpObject;

                                        if(request.getMethod() == HttpMethod.POST){

                                            final CompositeByteBuf contentBuf = (CompositeByteBuf) request.content();
                                            final String contentStr = contentBuf.toString(CharsetUtil.UTF_8);

// TEMP
logger.debug("Post content for " + request.getUri() + " : " + contentStr);
logger.debug("request headers " + request.headers().entries());
// TEMP

                                            context.setRequestBody(contentStr);
                                            context.getRequestHeaders().addAll(request.headers().entries());

                                            /*
                                            final String newBody = contentStr.replace("e", "ei");
                                            final ByteBuf bodyContent = Unpooled.copiedBuffer(newBody, CharsetUtil.UTF_8);
                                            contentBuf.clear().writeBytes(bodyContent);
                                            HttpHeaders.setContentLength(request, newBody.length());
                                            */
                                        }
                                    }

                                    return null;
                                }

                                @Override
                                public HttpObject proxyToClientResponse(final HttpObject httpObject) {

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

                                        final Optional<RestMethodEnum> restMethodOpt = checkForMockMethodMatch(originalRequest.getMethod().name(), pathMatchOpt.get());

                                        if (!restMethodOpt.isPresent()) {
                                            return httpObject;
                                        }

                                        //
                                        // Use mock
                                        final String mockUrl = MOCK_SERVER_HOST + mockServerPort + url.getPath();
                                        final HttpClientCallDTO dto = buildRequestDTO(context, restMethodOpt.get(), mockUrl);

                                        return buildResponse(callMock(dto));

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

    HttpClientCallDTO buildRequestDTO(final LittleProxyContext context, final RestMethodEnum method, final String destUrl) {
        logger.debug("buildRequestDTO called");

        final HttpClientCallDTO dto = new HttpClientCallDTO();

        if (RestMethodEnum.POST.equals(method)
                || RestMethodEnum.PUT.equals(method)) {
            dto.setBody(context.getRequestBody());
        }

        context.getRequestHeaders().stream().forEach(h ->
            dto.getHeaders().put(h.getKey(), h.getValue())
        );

        dto.setUrl(destUrl);
        dto.setMethod(method);

        return dto;
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
        logger.debug("callMock called");

        sanitizeRequestHeaders(dto);

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

    private void sanitizeRequestHeaders(final HttpClientCallDTO dto) {
        dto.setHeaders(dto.getHeaders()
                .entrySet()
                .stream()
                .filter(h -> !"Content-Length".equalsIgnoreCase(h.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    private class LittleProxyContext {

        private String requestBody;
        private List<Map.Entry<String, String>> requestHeaders = new ArrayList<>();

        public String getRequestBody() {
            return requestBody;
        }
        public void setRequestBody(String requestBody) {
            this.requestBody = requestBody;
        }

        public List<Map.Entry<String, String>> getRequestHeaders() {
            return requestHeaders;
        }
    }

}
