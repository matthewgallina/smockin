package com.smockin.admin.dto;

/**
 * Created by mgallina.
 */
public class JmsMockDefinitionDTO {

    private String extId;
    private String responseBody;
    private long timeoutInMillis;
    private boolean suspend;
    private int frequencyCount;
    private int frequencyPercentage;

    public JmsMockDefinitionDTO() {

    }

    public JmsMockDefinitionDTO(String extId, String responseBody, long timeoutInMillis, boolean suspend, int frequencyCount, int frequencyPercentage) {
        this.extId = extId;
        this.responseBody = responseBody;
        this.timeoutInMillis = timeoutInMillis;
        this.suspend = suspend;
        this.frequencyCount = frequencyCount;
        this.frequencyPercentage = frequencyPercentage;
    }

    public String getExtId() {
        return extId;
    }
    public void setExtId(String extId) {
        this.extId = extId;
    }

    public String getResponseBody() {
        return responseBody;
    }
    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public long getTimeoutInMillis() {
        return timeoutInMillis;
    }
    public void setTimeoutInMillis(long timeoutInMillis) {
        this.timeoutInMillis = timeoutInMillis;
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

}
