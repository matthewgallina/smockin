package com.smockin.admin.dto;

import javax.persistence.Column;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mgallina.
 */
public class RestfulMockDefinitionDTO {

    private String extId;
    private int orderNo;
    private int httpStatusCode;
    private String responseContentType;
    private String responseBody;
    private long sleepInMillis;
    private boolean suspend;
    private int frequencyCount;
    private int frequencyPercentage;
    private Map<String, String> responseHeaders = new HashMap<String, String>();

    public RestfulMockDefinitionDTO() {

    }

    public RestfulMockDefinitionDTO(String extId, int orderNo, int httpStatusCode, String responseContentType, String responseBody, long sleepInMillis, boolean suspend, int frequencyCount, int frequencyPercentage) {
        this.extId = extId;
        this.orderNo = orderNo;
        this.httpStatusCode = httpStatusCode;
        this.responseContentType = responseContentType;
        this.responseBody = responseBody;
        this.sleepInMillis = sleepInMillis;
        this.suspend = suspend;
        this.frequencyCount = frequencyCount;
        this.frequencyPercentage = frequencyPercentage;
    }

    public RestfulMockDefinitionDTO(int orderNo, int httpStatusCode, String responseContentType, String responseBody, int frequencyCount) {
        this.orderNo = orderNo;
        this.httpStatusCode = httpStatusCode;
        this.responseContentType = responseContentType;
        this.responseBody = responseBody;
        this.frequencyCount = frequencyCount;
    }

    public String getExtId() {
        return extId;
    }
    public void setExtId(String extId) {
        this.extId = extId;
    }

    public int getOrderNo() {
        return orderNo;
    }
    public void setOrderNo(int orderNo) {
        this.orderNo = orderNo;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }
    public void setHttpStatusCode(int httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }

    public String getResponseContentType() {
        return responseContentType;
    }
    public void setResponseContentType(String responseContentType) {
        this.responseContentType = responseContentType;
    }

    public String getResponseBody() {
        return responseBody;
    }
    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public long getSleepInMillis() {
        return sleepInMillis;
    }
    public void setSleepInMillis(long sleepInMillis) {
        this.sleepInMillis = sleepInMillis;
    }

    public boolean isSuspend() {
        return suspend;
    }
    public void setSuspend(boolean suspend) {
        this.suspend = suspend;
    }

    public int getFrequencyCount() {
        return frequencyCount;
    }
    public void setFrequencyCount(int frequencyCount) {
        this.frequencyCount = frequencyCount;
    }

    public int getFrequencyPercentage() {
        return frequencyPercentage;
    }
    public void setFrequencyPercentage(int frequencyPercentage) {
        this.frequencyPercentage = frequencyPercentage;
    }

    public Map<String, String> getResponseHeaders() {
        return responseHeaders;
    }
    public void setResponseHeaders(Map<String, String> responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

}
