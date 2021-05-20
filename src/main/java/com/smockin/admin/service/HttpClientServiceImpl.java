package com.smockin.admin.service;

import com.smockin.admin.dto.HttpClientCallDTO;
import com.smockin.admin.dto.response.HttpClientResponseDTO;
import com.smockin.admin.exception.ValidationException;
import com.smockin.mockserver.dto.MockServerState;
import com.smockin.utils.HttpClientUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mgallina.
 */
@Service
public class HttpClientServiceImpl implements HttpClientService {

    private final Logger logger = LoggerFactory.getLogger(HttpClientServiceImpl.class);

    @Autowired
    private MockedServerEngineService mockedServerEngineService;

    @Override
    public HttpClientResponseDTO handleExternalCall(final HttpClientCallDTO dto) throws ValidationException {
        logger.debug("handleExternalCall called");

        debugDTO(dto);
        validateRequest(dto);

        try {

            switch (dto.getMethod()) {
                case GET:
                    return get(dto);
                case POST:
                    return post(dto);
                case PUT:
                    return put(dto);
                case DELETE:
                    return delete(dto);
                case PATCH:
                    return patch(dto);
                default:
                    throw new ValidationException("Invalid / Unsupported method: " + dto.getMethod());
            }

        } catch (Exception ex) {
            logger.debug("Error performing external call ", ex);
            return new HttpClientResponseDTO(HttpStatus.NOT_FOUND.value());
        }

    }

    @Override
    public HttpClientResponseDTO handleCallToMock(final HttpClientCallDTO dto) throws ValidationException {
        logger.debug("handleCallToMock called");

        debugDTO(dto);

        validateRequest(dto);

        try {

            final MockServerState state = mockedServerEngineService.getRestServerState();

            if (!state.isRunning()) {
                return new HttpClientResponseDTO(HttpStatus.NOT_FOUND.value());
            }

            dto.setUrl("http://localhost:" + state.getPort() + dto.getUrl());

            switch (dto.getMethod()) {
                case GET:
                    return get(dto);
                case POST:
                    return post(dto);
                case PUT:
                    return put(dto);
                case DELETE:
                    return delete(dto);
                case PATCH:
                    return patch(dto);
                default:
                    throw new ValidationException("Invalid / Unsupported method: " + dto.getMethod());
            }

        } catch (Exception ex) {
            logger.debug("Error performing call to mock", ex);
            return new HttpClientResponseDTO(HttpStatus.NOT_FOUND.value());
        }

    }

    boolean isHttps(final String url) {
        return StringUtils.startsWith(url, HTTPS_PROTOCOL);
    }

    HttpClientResponseDTO get(final HttpClientCallDTO reqDto) throws IOException {

        final Request request = Request.Get(reqDto.getUrl());

        return executeRequest(request, reqDto, isHttps(reqDto.getUrl()));
    }

    HttpClientResponseDTO post(final HttpClientCallDTO reqDto) throws IOException {

        final Request request = Request.Post(reqDto.getUrl());

        HttpClientUtils.handleRequestData(request, reqDto.getHeaders(), reqDto);

        return executeRequest(request, reqDto, isHttps(reqDto.getUrl()));
    }

    HttpClientResponseDTO put(final HttpClientCallDTO reqDto) throws IOException {

        final Request request = Request.Put(reqDto.getUrl());

        HttpClientUtils.handleRequestData(request, reqDto.getHeaders(), reqDto);

        return executeRequest(request, reqDto, isHttps(reqDto.getUrl()));
    }

    HttpClientResponseDTO delete(final HttpClientCallDTO reqDto) throws IOException {

        final Request request = Request.Delete(reqDto.getUrl());

        return executeRequest(request, reqDto, isHttps(reqDto.getUrl()));
    }

    HttpClientResponseDTO patch(final HttpClientCallDTO reqDto) throws IOException {

        final Request request = Request.Patch(reqDto.getUrl())
                .bodyByteArray((reqDto.getBody() != null) ? reqDto.getBody().getBytes() : null);

        return executeRequest(request, reqDto, isHttps(reqDto.getUrl()));
    }

    /**
     *
     * Assumes the request body is not mandatory.
     *
     * @param httpClientCallDTO
     * @throws ValidationException
     *
     */
    void validateRequest(final HttpClientCallDTO httpClientCallDTO) throws ValidationException {

        if (StringUtils.isBlank(httpClientCallDTO.getUrl())) {
            throw new ValidationException("url is required");
        }

        if (httpClientCallDTO.getMethod() == null) {
            throw new ValidationException("method is required");
        }

    }

    void applyRequestHeaders(final Request request, final Map<String, String> requestHeaders) {

        if (requestHeaders == null)
            return;

        requestHeaders
            .entrySet()
            .forEach(h ->
                request.addHeader(h.getKey(), h.getValue()));

    }

    /**
     * BUG FIX: The apache client automatically sets CONTENT_LENGTH, causing a header duplication error.
     */
    void duplicateContentLengthBugFix(final HttpClientCallDTO reqDto) {

        if (StringUtils.isBlank(reqDto.getBody())) {
            return;
        }

        if (reqDto.getHeaders().containsKey(HttpHeaders.CONTENT_LENGTH)) {
            reqDto.getHeaders().remove(HttpHeaders.CONTENT_LENGTH);
        }

    }

    Map<String, String> extractResponseHeaders(final HttpResponse httpResponse) {

        return new HashMap<String, String>() {
            {
                for (Header h : httpResponse.getAllHeaders()) {
                    put(h.getName(), h.getValue());
                }
            }
        };
    }

    String extractResponseBody(final HttpResponse httpResponse) throws IOException {

        return IOUtils.toString(httpResponse.getEntity().getContent(), StandardCharsets.UTF_8.name());
    }

    HttpClientResponseDTO executeRequest(final Request request,
                                         final HttpClientCallDTO reqDto,
                                         final boolean isHttpsCall) throws IOException {

        duplicateContentLengthBugFix(reqDto);
        applyRequestHeaders(request, reqDto.getHeaders());

        final HttpResponse httpResponse;

        if (isHttpsCall) {

            try {
                final Executor executor = Executor.newInstance(noSslHttpClient());
                httpResponse = executor.execute(request).returnResponse();
            } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
                throw new IOException();
            }

        } else {

            httpResponse = request.execute().returnResponse();
        }

        return new HttpClientResponseDTO(
                httpResponse.getStatusLine().getStatusCode(),
                (httpResponse.getEntity() != null)
                    ? httpResponse.getEntity().getContentType().getValue()
                    : null, // i.e. 204
                extractResponseHeaders(httpResponse),
                (httpResponse.getEntity() != null)
                    ? extractResponseBody(httpResponse)
                    : null // i.e. 204
        );
    }

    private CloseableHttpClient noSslHttpClient() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {

        final SSLContext sslContext = new SSLContextBuilder()
                .loadTrustMaterial(null, (x509CertChain, authType) -> true)
                .build();

        return HttpClientBuilder.create()
                .setSSLContext(sslContext)
                .setConnectionManager(
                        new PoolingHttpClientConnectionManager(
                                RegistryBuilder.<ConnectionSocketFactory>create()
                                        .register("http", PlainConnectionSocketFactory.INSTANCE)
                                        .register("https", new SSLConnectionSocketFactory(sslContext,
                                                NoopHostnameVerifier.INSTANCE))
                                        .build()
                        ))
                .build();
    }

    private void debugDTO(final HttpClientCallDTO dto) {

        if (logger.isDebugEnabled()) {
            logger.debug( "URL : " + dto.getUrl() );
            logger.debug( "METHOD : " + dto.getMethod().name() );
            logger.debug( "BODY : " + dto.getBody() );
            logger.debug( "HEADERS : " );

            dto.getHeaders().entrySet().forEach(h ->
                    logger.debug( h.getKey() +  " : " + h.getValue() ));
        }

    }

}
