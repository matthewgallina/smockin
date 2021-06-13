package com.smockin.mockserver.dto;

import com.smockin.admin.persistence.enums.ServerTypeEnum;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by mgallina.
 */
@Data
public class MockedServerConfigDTO {

    private ServerTypeEnum serverType;
    private Integer port;
    private Integer maxThreads;
    private Integer minThreads;
    private Integer timeOutMillis;
    private boolean autoStart;
    private boolean proxyMode;
    private Map<String, String> nativeProperties = new HashMap<>();

    public MockedServerConfigDTO() {

    }

    public MockedServerConfigDTO(final ServerTypeEnum serverType, final Integer port, final Integer maxThreads,
                                 final Integer minThreads, final Integer timeOutMillis, final boolean autoStart,
                                 final boolean proxyMode,
                                 final Map<String, String> nativeProperties) {
        this.serverType = serverType;
        this.port = port;
        this.maxThreads = maxThreads;
        this.minThreads = minThreads;
        this.timeOutMillis = timeOutMillis;
        this.autoStart = autoStart;
        this.proxyMode = proxyMode;
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
                + ", ProxyMode : " + proxyMode;
    }

}
