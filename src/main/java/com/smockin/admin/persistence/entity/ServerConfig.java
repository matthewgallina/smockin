package com.smockin.admin.persistence.entity;

import com.smockin.admin.persistence.enums.ServerTypeEnum;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.ColumnDefault;

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

    @ElementCollection(fetch = FetchType.EAGER)
    private Map<String, String> nativeProperties = new HashMap<>();

    public ServerConfig() {

    }

    public ServerConfig(final ServerTypeEnum serverType) {
        this.serverType = serverType;
    }

}
