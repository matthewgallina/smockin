package com.smockin.utils;

import com.smockin.admin.dto.response.LiveLoggingDTO;
import com.smockin.admin.dto.response.LiveLoggingInboundContentDTO;
import com.smockin.admin.dto.response.LiveLoggingOutboundContentDTO;
import com.smockin.admin.enums.LiveLoggingDirectionEnum;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Created by mgallina.
 */
public final class LiveLoggingUtils {

    private static final String INBOUND_ARROW = "------>";
    private static final String OUTBOUND_ARROW = "<------";
    private static final String NOT_AVAILABLE = "n/a";
    private static final String CARRIAGE_RET = "\n";
    private static final String HEADER_DELIMITER = ",";

    private static final AtomicInteger atomicTraceIdInt = new AtomicInteger(1);

    public static String getTraceId() {
        return String.valueOf("t-" + atomicTraceIdInt.getAndIncrement());
    }

    /**
        REQUEST ------> PROXY SERVER ------> MOCK SERVER
        GET http://localhost:9000/proxypass
        trace id: t-6
        headers:
        body: n/a
    */
    public static String buildLiveLogInboundFileEntry(final String reqId, final String method, final String url, final String contentType, final Map<String, String> headers, final String reqBody, final boolean viaProxy) {

        final StringBuilder sb = new StringBuilder();

        sb.append(buildLiveLogFileHeader(LiveLoggingDirectionEnum.REQUEST, viaProxy, null));
        sb.append(method)
            .append(" ")
            .append(url)
            .append(CARRIAGE_RET);
        sb.append("trace id:")
            .append(reqId)
            .append(CARRIAGE_RET);
        sb.append("headers: ")
            .append(headers.entrySet().stream().map(h -> h.getKey() + "=" + h.getValue()).collect(Collectors.joining(HEADER_DELIMITER)))
            .append(CARRIAGE_RET);
        sb.append("body: ")
            .append(StringUtils.defaultIfBlank(reqBody, NOT_AVAILABLE))
            .append(CARRIAGE_RET);

        return sb.toString();
    }

    /**
         200 RESPONSE <------ PROXY SERVER <------ MOCK SERVER
         trace id: t-6
         headers:
         body: { "name" : "jane" }
     */
    public static String buildLiveLogOutboundFileEntry(final String reqId, final int status, final String contentType, final Map<String, String> headers, final String responseBody, final boolean viaProxy, final boolean isProxyMockedResponse) {

        final StringBuilder sb = new StringBuilder();

        sb.append(status)
                .append(" ")
                .append(buildLiveLogFileHeader(LiveLoggingDirectionEnum.RESPONSE, viaProxy, isProxyMockedResponse));
        sb.append("trace id:")
                .append(reqId)
                .append(CARRIAGE_RET);
        sb.append("headers: ")
                .append(headers.entrySet().stream().map(h -> h.getKey() + "=" + h.getValue()).collect(Collectors.joining(HEADER_DELIMITER)))
                .append(CARRIAGE_RET);
        sb.append("body: ")
            .append(StringUtils.defaultIfBlank(responseBody, NOT_AVAILABLE))
            .append(CARRIAGE_RET);

        return sb.toString();
    }

    /**
     *
     * e.g
     * REQUEST ------> PROXY SERVER ------> MOCK SERVER
     * RESPONSE <------  PROXY SERVER <------ MOCK SERVER
     *
     * REQUEST ------> PROXY SERVER ------> ORIGINAL DESTINATION
     * RESPONSE <------  PROXY SERVER <------ ORIGINAL DESTINATION
     *
     * REQUEST ------> MOCK SERVER
     * RESPONSE <------  MOCK SERVER
     *
     * @param direction
     * @param viaProxy
     *
     * @returns String
     */
    private static String buildLiveLogFileHeader(final LiveLoggingDirectionEnum direction, final boolean viaProxy, final Boolean mockedResponse) {

        final String directionalArrow = getDirectionalArrow(direction);

        final StringBuilder sb = new StringBuilder();

        sb.append(direction.name());
        sb.append(" ");

        sb.append(directionalArrow);

        if (viaProxy) {
            sb.append(" PROXY SERVER ");
            sb.append(directionalArrow);
        }

        if (viaProxy && mockedResponse != null && !mockedResponse) {
            sb.append(" ORIGINAL DESTINATION");
        } else {
            sb.append(" MOCK SERVER");
        }
        sb.append(CARRIAGE_RET);

        return sb.toString();
    }

    public static LiveLoggingDTO buildLiveLogInboundDTO(final String reqId, final String method, final String url, final String contentType, final Map<String, String> headers, final String reqBody, final boolean viaProxy) {

        return new LiveLoggingDTO(reqId, LiveLoggingDirectionEnum.REQUEST, viaProxy, new LiveLoggingInboundContentDTO(StringUtils.defaultIfBlank(contentType, NOT_AVAILABLE), headers, method, url, StringUtils.defaultIfBlank(reqBody, NOT_AVAILABLE)));
    }

    public static LiveLoggingDTO buildLiveLogOutboundDTO(final String reqId, final int status, final String contentType, final Map<String, String> headers, final String responseBody, final boolean viaProxy, final boolean isProxyMockedResponse) {

        return new LiveLoggingDTO(reqId, LiveLoggingDirectionEnum.RESPONSE, viaProxy, new LiveLoggingOutboundContentDTO(StringUtils.defaultIfBlank(contentType, NOT_AVAILABLE), headers, StringUtils.defaultIfBlank(responseBody, NOT_AVAILABLE), status, isProxyMockedResponse));
    }

    private static String getDirectionalArrow(final LiveLoggingDirectionEnum direction) {

        switch (direction) {
            case REQUEST:
                return INBOUND_ARROW;
            case RESPONSE:
                return OUTBOUND_ARROW;
            default:
                throw new IllegalArgumentException("Unsupported Live Logging Direction: " + direction);
        }

    }

}
