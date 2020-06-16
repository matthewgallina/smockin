package com.smockin.mockserver.dto;

import com.smockin.admin.persistence.enums.ProxyModeTypeEnum;
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
    private boolean proxyMode;
    private ProxyModeTypeEnum proxyModeType;
    private String proxyForwardUrl;
    private boolean doNotForwardWhen404Mock;
    private Map<String, String> nativeProperties = new HashMap<>();

    public MockedServerConfigDTO() {

    }

    public MockedServerConfigDTO(final ServerTypeEnum serverType, Integer port, Integer maxThreads, Integer minThreads, Integer timeOutMillis,
                                 boolean autoStart, boolean proxyMode, ProxyModeTypeEnum proxyModeType,
                                 final String proxyForwardUrl, final boolean doNotForwardWhen404Mock, Map<String, String> nativeProperties) {
        this.serverType = serverType;
        this.port = port;
        this.maxThreads = maxThreads;
        this.minThreads = minThreads;
        this.timeOutMillis = timeOutMillis;
        this.autoStart = autoStart;
        this.proxyMode = proxyMode;
        this.proxyModeType = proxyModeType;
        this.proxyForwardUrl = proxyForwardUrl;
        this.doNotForwardWhen404Mock = doNotForwardWhen404Mock;
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

    public boolean isProxyMode() {
        return proxyMode;
    }
    public void setProxyMode(boolean proxyMode) {
        this.proxyMode = proxyMode;
    }

    public ProxyModeTypeEnum getProxyModeType() {
        return proxyModeType;
    }
    public void setProxyModeType(ProxyModeTypeEnum proxyModeType) {
        this.proxyModeType = proxyModeType;
    }

    public String getProxyForwardUrl() {
        return proxyForwardUrl;
    }
    public void setProxyForwardUrl(String proxyForwardUrl) {
        this.proxyForwardUrl = proxyForwardUrl;
    }

    public boolean isDoNotForwardWhen404Mock() {
        return doNotForwardWhen404Mock;
    }
    public void setDoNotForwardWhen404Mock(boolean doNotForwardWhen404Mock) {
        this.doNotForwardWhen404Mock = doNotForwardWhen404Mock;
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
                + ", AutoStart : " + autoStart
                + ", ProxyMode : " + proxyMode
                + ", ProxyModeType : " + proxyModeType
                + ", proxyForwardUrl : " + proxyForwardUrl
                + ", doNotForwardWhen404Mock : " + doNotForwardWhen404Mock;
    }
}
