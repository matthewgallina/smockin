package com.smockin.admin.dto.response;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by mgallina on 02/08/17.
 */
public class HttpClientResponseDTO {

    private int status;
    private String contentType;
    private Map<String, String> headers = new HashMap<String, String>();
    private String body;

    public HttpClientResponseDTO() {

    }

    public HttpClientResponseDTO(final int status, final String body) {
        this.status = status;
        this.body = body;
    }

    public HttpClientResponseDTO(final int status, final String contentType, final Map<String, String> headers, final String body) {
        this.status = status;
        this.contentType = contentType;
        this.headers = headers;
        this.body = body;
    }

    public int getStatus() {
        return status;
    }
    public void setStatus(int status) {
        this.status = status;
    }

    public String getContentType() {
        return contentType;
    }
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public String getBody() {
        return body;
    }
    public void setBody(String responseBody) {
        this.body = body;
    }

}
