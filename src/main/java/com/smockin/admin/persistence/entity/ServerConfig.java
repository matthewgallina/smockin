package com.smockin.admin.persistence.entity;

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

    @Column(name = "AUTO_REFRESH", nullable = false)
    private boolean autoRefresh;

    @ColumnDefault("false")
    @Column(name = "USE_CORS", nullable = false)
    private boolean enableCors;

    @ElementCollection(fetch = FetchType.EAGER)
    private Map<String, String> nativeProperties = new HashMap<String, String>();

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

}
