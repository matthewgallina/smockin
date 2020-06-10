package com.smockin.admin.dto;

import com.smockin.admin.persistence.enums.RestMethodEnum;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by mgallina.
 */
public class HttpClientCallDTO {

    private RestMethodEnum method;
    private Map<String, String> headers = new HashMap<>();
    private String url;
    private String body;

    public HttpClientCallDTO() { }

    public HttpClientCallDTO(final String url, final RestMethodEnum method) {
        this.url = url;
        this.method = method;
    }

    public RestMethodEnum getMethod() {
        return method;
    }
    public void setMethod(RestMethodEnum method) {
        this.method = method;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
    }

    public String getBody() {
        return body;
    }
    public void setBody(String body) {
        this.body = body;
    }

}
