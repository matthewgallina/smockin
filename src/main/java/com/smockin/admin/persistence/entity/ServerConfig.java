package com.smockin.admin.persistence.entity;

import com.smockin.admin.persistence.enums.ProxyModeTypeEnum;
import com.smockin.admin.persistence.enums.ServerTypeEnum;
import org.hibernate.annotations.ColumnDefault;

import javax.persistence.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mgallina.
 */
@Entity
@Table(name = "SERVER_CONFIG")
public class ServerConfig extends Identifier {

    @Enumerated(EnumType.STRING)
    @Column(name = "SERVER_TYPE", nullable = false, length = 20, unique = true)
    private ServerTypeEnum serverType;

    @Column(name = "PORT", nullable = false, unique = true)
    private Integer port;

    @Column(name = "MAX_THREADS", nullable = false)
    private Integer maxThreads;

    @Column(name = "MIN_THREADS", nullable = false)
    private Integer minThreads;

    @Column(name = "TIME_OUT_MILLIS", nullable = false)
    private Integer timeOutMillis;

    @Column(name = "AUTO_START", nullable = false)
    private boolean autoStart;

    @ColumnDefault("false")
    @Column(name = "PROXY_MODE", nullable = false)
    private boolean proxyMode;

    @Column(name = "PROXY_MODE_TYPE", length = 8)
    @Enumerated(EnumType.STRING)
    private ProxyModeTypeEnum proxyModeType;

    @Column(name = "PROXY_FORWARD_URL", length = 200)
    private String proxyForwardUrl;

    @ColumnDefault("false")
    @Column(name = "NO_FORWARD_WHEN_404_MOCK", nullable = false)
    private boolean doNotForwardWhen404Mock;

    @ElementCollection(fetch = FetchType.EAGER)
    private Map<String, String> nativeProperties = new HashMap<>();

    public ServerConfig() {

    }

    public ServerConfig(final ServerTypeEnum serverType) {
        this.serverType = serverType;
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

}
