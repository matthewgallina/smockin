package com.smockin.admin.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by mgallina.
 */
public class RuleDTO {

    private String extId;
    private int orderNo;
    private int httpStatusCode;
    private String responseContentType;
    private String responseBody;
    private long sleepInMillis;
    private boolean suspend;
    private Map<String, String> responseHeaders = new HashMap<String, String>();
    private List<RuleGroupDTO> groups = new ArrayList<RuleGroupDTO>();

    public RuleDTO() {
    }

    public RuleDTO(int httpStatusCode, String responseBody) {
        this.httpStatusCode = httpStatusCode;
        this.responseBody = responseBody;
    }

    public RuleDTO(String extId, int orderNo, int httpStatusCode, String responseContentType, String responseBody, long sleepInMillis, boolean suspend) {
        this.extId = extId;
        this.orderNo = orderNo;
        this.httpStatusCode = httpStatusCode;
        this.responseContentType = responseContentType;
        this.responseBody = responseBody;
        this.sleepInMillis = sleepInMillis;
        this.suspend = suspend;
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

    public List<RuleGroupDTO> getGroups() {
        return groups;
    }
    public void setGroups(List<RuleGroupDTO> groups) {
        this.groups = groups;
    }

    public Map<String, String> getResponseHeaders() {
        return responseHeaders;
    }
    public void setResponseHeaders(Map<String, String> responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

}
