package com.smockin.mockserver.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by mgallina.
 */
public class MockedServerConfigDTO {

    private Integer port;
    private Integer maxThreads;
    private Integer minThreads;
    private Integer timeOutMillis;
    private boolean autoStart;
    private boolean autoRefresh;
    private boolean enableCors;
    private Map<String, String> nativeProperties = new HashMap<String, String>();

    public MockedServerConfigDTO() {
    }

    public MockedServerConfigDTO(Integer port, Integer maxThreads, Integer minThreads, Integer timeOutMillis, boolean autoStart, boolean autoRefresh, boolean enableCors, Map<String, String> nativeProperties) {
        this.port = port;
        this.maxThreads = maxThreads;
        this.minThreads = minThreads;
        this.timeOutMillis = timeOutMillis;
        this.autoStart = autoStart;
        this.autoRefresh = autoRefresh;
        this.enableCors = enableCors;
        this.nativeProperties = nativeProperties;
    }

    public Integer getPort() {
        return port;
    }
    public void setPort(Integer port) {
        this.port = port;
    }

    public Integer getMaxThreads() {
        return maxThreads;
    }
    public void setMaxThreads(Integer maxThreads) {
        this.maxThreads = maxThreads;
    }

    public Integer getMinThreads() {
        return minThreads;
    }
    public void setMinThreads(Integer minThreads) {
        this.minThreads = minThreads;
    }

    public Integer getTimeOutMillis() {
        return timeOutMillis;
    }
    public void setTimeOutMillis(Integer timeOutMillis) {
        this.timeOutMillis = timeOutMillis;
    }

    public boolean isAutoStart() {
        return autoStart;
    }
    public void setAutoStart(boolean autoStart) {
        this.autoStart = autoStart;
    }

    public boolean isAutoRefresh() {
        return autoRefresh;
    }
    public void setAutoRefresh(boolean autoRefresh) {
        this.autoRefresh = autoRefresh;
    }

    public boolean isEnableCors() {
        return enableCors;
    }
    public void setEnableCors(boolean enableCors) {
        this.enableCors = enableCors;
    }

    public Map<String, String> getNativeProperties() {
        return nativeProperties;
    }
    public void setNativeProperties(Map<String, String> nativeProperties) {
        this.nativeProperties = nativeProperties;
    }

    @Override
    public String toString() {
        return "Mocked Server Config :- "
                + " Port : " + port
                + ", MaxThreads : " + maxThreads
                + ", MinThreads : " + minThreads
                + ", TimeOutMillis : " + timeOutMillis
                + ", AutoStart : " + autoStart
                + ", EnableCors : " + enableCors
                + ", AutoRefresh : " + autoRefresh;
    }
}
