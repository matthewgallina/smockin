package com.smockin.admin.persistence.entity;

import com.smockin.admin.persistence.enums.ProxyModeTypeEnum;
import com.smockin.admin.persistence.enums.ServerTypeEnum;
import lombok.Data;
import org.hibernate.annotations.ColumnDefault;

import javax.persistence.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mgallina.
 */
@Entity
@Table(name = "SERVER_CONFIG")
@Data
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

}
