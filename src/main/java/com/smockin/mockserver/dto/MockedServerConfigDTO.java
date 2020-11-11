package com.smockin.mockserver.dto;

import com.smockin.admin.persistence.enums.ServerTypeEnum;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by mgallina.
 */
public class MockedServerConfigDTO {

    private ServerTypeEnum serverType;
    private Integer port;
    private Integer maxThreads;
    private Integer minThreads;
    private Integer timeOutMillis;
    private boolean autoStart;
    private Map<String, String> nativeProperties = new HashMap<>();

    public MockedServerConfigDTO() {

    }

    public MockedServerConfigDTO(final ServerTypeEnum serverType, final Integer port, final Integer maxThreads,
                                 final Integer minThreads, final Integer timeOutMillis, final boolean autoStart,
                                 final Map<String, String> nativeProperties) {
        this.serverType = serverType;
        this.port = port;
        this.maxThreads = maxThreads;
        this.minThreads = minThreads;
        this.timeOutMillis = timeOutMillis;
        this.autoStart = autoStart;
        this.nativeProperties = nativeProperties;
    }

    public ServerTypeEnum getServerType() {
        return serverType;
    }
    public void setServerType(ServerTypeEnum serverType) {
        this.serverType = serverType;
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

    public Map<String, String> getNativeProperties() {
        return nativeProperties;
    }
    public void setNativeProperties(Map<String, String> nativeProperties) {
        this.nativeProperties = nativeProperties;
    }

    @Override
    public String toString() {
        return "Mocked Server Config :- "
                + " ServerType : " + serverType
                + " Port : " + port
                + ", MaxThreads : " + maxThreads
                + ", MinThreads : " + minThreads
                + ", TimeOutMillis : " + timeOutMillis
                + ", AutoStart : " + autoStart;
    }
}
