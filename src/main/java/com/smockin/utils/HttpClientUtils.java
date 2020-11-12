package com.smockin.utils;

import com.smockin.admin.dto.HttpClientCallDTO;
import org.apache.http.NameValuePair;
import org.apache.http.client.fluent.Request;
import org.apache.http.message.BasicNameValuePair;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class HttpClientUtils {

    static String EQUALS = "=";
    static String AND = "&";

    public static void handleRequestData(final Request request,
                                         final Map<String, String> requestHeaders,
                                         final HttpClientCallDTO reqDto) {

        if (requestHeaders.containsKey(HttpHeaders.CONTENT_TYPE)
                && requestHeaders.containsValue(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                && reqDto.getBody() != null
                && reqDto.getBody().contains(EQUALS)) {

            final List<NameValuePair> postParameters = new ArrayList<>();
            final String[] formParameterPairsArray = reqDto.getBody().split(AND);

            Stream.of(formParameterPairsArray).forEach(pa -> {

                if (pa.contains(EQUALS)) {

                    final String[] pairArray = pa.split(EQUALS);

                    if (pairArray.length == 2) {
                        postParameters.add(new BasicNameValuePair(pairArray[0], pairArray[1]));
                    }

                }

            });

            request.bodyForm(postParameters);

            return;
        }

        request.bodyByteArray((reqDto.getBody() != null) ? reqDto.getBody().getBytes() : null);

    }

}
