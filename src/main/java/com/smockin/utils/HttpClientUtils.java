package com.smockin.utils;

import com.smockin.admin.dto.HttpClientCallDTO;
import com.smockin.admin.dto.response.HttpClientResponseDTO;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.fluent.Request;
import org.apache.http.message.BasicNameValuePair;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class HttpClientUtils {

    public static HttpClientResponseDTO get(final HttpClientCallDTO reqDto) throws IOException {

        final Request request = Request.Get(reqDto.getUrl());

        return executeRequest(request, reqDto.getHeaders());
    }

    public static HttpClientResponseDTO post(final HttpClientCallDTO reqDto) throws IOException {

        final Request request = Request.Post(reqDto.getUrl());

        handleRequestData(request, reqDto.getHeaders(), reqDto);

        return executeRequest(request, reqDto.getHeaders());
    }

    public static HttpClientResponseDTO put(final HttpClientCallDTO reqDto) throws IOException {

        final Request request = Request.Put(reqDto.getUrl());

        handleRequestData(request, reqDto.getHeaders(), reqDto);

        return executeRequest(request, reqDto.getHeaders());
    }

    public static HttpClientResponseDTO delete(final HttpClientCallDTO reqDto) throws IOException {

        final Request request = Request.Delete(reqDto.getUrl());

        return executeRequest(request, reqDto.getHeaders());
    }

    public static HttpClientResponseDTO patch(final HttpClientCallDTO reqDto) throws IOException {

        final Request request = Request.Patch(reqDto.getUrl())
                .bodyByteArray((reqDto.getBody() != null)?reqDto.getBody().getBytes():null);

        return executeRequest(request, reqDto.getHeaders());
    }

    public static void applyRequestHeaders(final Request request, final Map<String, String> requestHeaders) {

        if (requestHeaders == null)
            return;

        requestHeaders.entrySet().forEach(h ->
                request.addHeader(h.getKey(), h.getValue()));

    }

    public static Map<String, String> extractResponseHeaders(final HttpResponse httpResponse) {

        return new HashMap<String, String>() {
            {
                for (Header h : httpResponse.getAllHeaders()) {
                    put(h.getName(), h.getValue());
                }
            }
        };
    }

    static String extractResponseBody(final HttpResponse httpResponse) throws IOException {

        return IOUtils.toString(httpResponse.getEntity().getContent(), StandardCharsets.UTF_8.name());
    }

    static HttpClientResponseDTO executeRequest(final Request request, final Map<String, String> requestHeaders) throws IOException {

        applyRequestHeaders(request, requestHeaders);

        final HttpResponse httpResponse = request.execute().returnResponse();

        return new HttpClientResponseDTO(
                httpResponse.getStatusLine().getStatusCode(),
                httpResponse.getEntity().getContentType().getValue(),
                extractResponseHeaders(httpResponse),
                extractResponseBody(httpResponse)
        );
    }

    static void handleRequestData(final Request request, final Map<String, String> requestHeaders, final HttpClientCallDTO reqDto) {

        if (requestHeaders.containsValue(MediaType.APPLICATION_FORM_URLENCODED_VALUE)) {

            final List<NameValuePair> postParameters = new ArrayList<>();

            if (reqDto.getBody() != null && reqDto.getBody().contains("&")) {

                final String[] formParameterPairsArray = reqDto.getBody().split("&");

                Stream.of(formParameterPairsArray).forEach(pa -> {

                    if (pa.contains("=")) {

                        final String[] pairArray = pa.split("=");

                        if (pairArray.length == 2) {
                            postParameters.add(new BasicNameValuePair(pairArray[0], pairArray[1]));
                        }

                    }

                });

            }

            request.bodyForm(postParameters);
            return;
        }

        request.bodyByteArray((reqDto.getBody() != null)?reqDto.getBody().getBytes():null);
    }

}
