package com.smockin.mockserver.proxy;

public class LittleProxyLoggingDTO {

    private boolean loggingRequestReceived;
    private int loggingResponseStatus;
    private String loggingResponseContentType;

    public boolean isLoggingRequestReceived() {
        return loggingRequestReceived;
    }
    public void setLoggingRequestReceived(boolean loggingRequestReceived) {
        this.loggingRequestReceived = loggingRequestReceived;
    }

    public int getLoggingResponseStatus() {
        return loggingResponseStatus;
    }
    public void setLoggingResponseStatus(int loggingResponseStatus) {
        this.loggingResponseStatus = loggingResponseStatus;
    }

    public String getLoggingResponseContentType() {
        return loggingResponseContentType;
    }
    public void setLoggingResponseContentType(String loggingResponseContentType) {
        this.loggingResponseContentType = loggingResponseContentType;
    }

}
