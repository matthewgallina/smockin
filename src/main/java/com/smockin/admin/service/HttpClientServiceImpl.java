package com.smockin.admin.service;

import com.smockin.admin.dto.HttpClientCallDTO;
import com.smockin.admin.dto.response.HttpClientResponseDTO;
import com.smockin.admin.exception.ValidationException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mgallina.
 */
@Service
public class HttpClientServiceImpl implements HttpClientService {

    @Override
    public HttpClientResponseDTO handleCall(final HttpClientCallDTO dto) throws ValidationException {

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

        } catch (IOException ex) {
            return new HttpClientResponseDTO(404, "Error communicating with url: " + dto.getUrl() + ". Check the path is valid and that the mock server running...");
        }

    }

    HttpClientResponseDTO get(final HttpClientCallDTO reqDto) throws IOException {

        final Request request = Request.Get(reqDto.getUrl());

        return executeRequest(request, reqDto.getHeaders());
    }

    HttpClientResponseDTO post(final HttpClientCallDTO reqDto) throws IOException {

        final Request request = Request.Post(reqDto.getUrl())
                .bodyByteArray((reqDto.getBody() != null)?reqDto.getBody().getBytes():null);

        return executeRequest(request, reqDto.getHeaders());
    }

    HttpClientResponseDTO put(final HttpClientCallDTO reqDto) throws IOException {

        final Request request = Request.Put(reqDto.getUrl())
                .bodyByteArray((reqDto.getBody() != null)?reqDto.getBody().getBytes():null);

        return executeRequest(request, reqDto.getHeaders());
    }

    HttpClientResponseDTO delete(final HttpClientCallDTO reqDto) throws IOException {

        final Request request = Request.Delete(reqDto.getUrl());

        return executeRequest(request, reqDto.getHeaders());
    }

    HttpClientResponseDTO patch(final HttpClientCallDTO reqDto) throws IOException {

        final Request request = Request.Patch(reqDto.getUrl())
                .bodyByteArray((reqDto.getBody() != null)?reqDto.getBody().getBytes():null);

        return executeRequest(request, reqDto.getHeaders());
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

        for (Map.Entry<String, String> h : requestHeaders.entrySet()) {
            request.addHeader(h.getKey(), h.getValue());
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

    HttpClientResponseDTO executeRequest(final Request request, final Map<String, String> requestHeaders) throws IOException {

        applyRequestHeaders(request, requestHeaders);

        final HttpResponse httpResponse = request.execute().returnResponse();

        return new HttpClientResponseDTO(
                httpResponse.getStatusLine().getStatusCode(),
                httpResponse.getEntity().getContentType().getValue(),
                extractResponseHeaders(httpResponse),
                extractResponseBody(httpResponse)
        );
    }

}
