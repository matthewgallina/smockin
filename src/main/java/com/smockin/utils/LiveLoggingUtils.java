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

    private static final String INBOUND_ARROW = "--------------------->";
    private static final String OUTBOUND_ARROW = "<---------------------";
    private static final String NOT_AVAILABLE = "n/a";
    private static final String CARRIAGE_RET = "\n";
    private static final String HEADER_DELIMITER = ",";

    private static final AtomicInteger atomicTraceIdInt = new AtomicInteger(1);

    public static String getTraceId() {
        return String.valueOf("t-" + atomicTraceIdInt.getAndIncrement());
    }

    public static String buildLiveLogInboundFileEntry(final String reqId, final String method, final String url, final String contentType, final Map<String, String> headers, final String reqBody, final boolean viaProxy) {

        final StringBuilder sb = buildLiveLogHeader(LiveLoggingDirectionEnum.REQUEST, reqId, (viaProxy) ? "(proxy)" : null);

        sb.append("method: ");
        sb.append(method);
        sb.append(CARRIAGE_RET);
        sb.append("url: ");
        sb.append(url);
        sb.append(CARRIAGE_RET);
        sb.append("content type: ");
        sb.append(StringUtils.defaultIfBlank(contentType, NOT_AVAILABLE));
        sb.append(CARRIAGE_RET);
        sb.append("headers: ");
        sb.append(headers.entrySet().stream().map(h -> h.getKey() + "=" + h.getValue()).collect(Collectors.joining(HEADER_DELIMITER)));
        sb.append(CARRIAGE_RET);
        sb.append("body: ");
        sb.append(StringUtils.defaultIfBlank(reqBody, NOT_AVAILABLE));
        sb.append(CARRIAGE_RET);

        return sb.toString();
    }

    public static String buildLiveLogOutboundFileEntry(final String reqId, final int status, final String contentType, final Map<String, String> headers, final String responseBody, final boolean viaProxy, final boolean isProxyMockedResponse) {

        final StringBuilder sb = buildLiveLogHeader(LiveLoggingDirectionEnum.RESPONSE, reqId, ((viaProxy) ? "(proxy <- " + ((isProxyMockedResponse) ? "mocked" : "original") + ")" : null));

        sb.append("status: ");
        sb.append(status);
        sb.append(CARRIAGE_RET);
        sb.append("content type: ");
        sb.append(StringUtils.defaultIfBlank(contentType, NOT_AVAILABLE));
        sb.append(CARRIAGE_RET);
        sb.append("headers: ");
        sb.append(headers.entrySet().stream().map(h -> h.getKey() + "=" + h.getValue()).collect(Collectors.joining(HEADER_DELIMITER)));
        sb.append(CARRIAGE_RET);
        sb.append("body: ");
        sb.append(StringUtils.defaultIfBlank(responseBody, NOT_AVAILABLE));
        sb.append(CARRIAGE_RET);

        return sb.toString();
    }

    private static StringBuilder buildLiveLogHeader(final LiveLoggingDirectionEnum direction, final String reqId, final String directionMeta) {

        final StringBuilder sb = new StringBuilder();

        sb.append(getDirectionArrow(direction));

        if (directionMeta != null) {
            sb.append(" ");
            sb.append(directionMeta);
        }

        sb.append(CARRIAGE_RET);

        sb.append("id: ");
        sb.append(reqId);
        sb.append(CARRIAGE_RET);

        return sb;
    }

    public static LiveLoggingDTO buildLiveLogInboundDTO(final String reqId, final String method, final String url, final String contentType, final Map<String, String> headers, final String reqBody, final boolean viaProxy) {

        return new LiveLoggingDTO(reqId, LiveLoggingDirectionEnum.REQUEST, viaProxy, new LiveLoggingInboundContentDTO(StringUtils.defaultIfBlank(contentType, NOT_AVAILABLE), headers, method, url, StringUtils.defaultIfBlank(reqBody, NOT_AVAILABLE)));
    }

    public static LiveLoggingDTO buildLiveLogOutboundDTO(final String reqId, final int status, final String contentType, final Map<String, String> headers, final String responseBody, final boolean viaProxy, final boolean isProxyMockedResponse) {

        return new LiveLoggingDTO(reqId, LiveLoggingDirectionEnum.RESPONSE, viaProxy, new LiveLoggingOutboundContentDTO(StringUtils.defaultIfBlank(contentType, NOT_AVAILABLE), headers, StringUtils.defaultIfBlank(responseBody, NOT_AVAILABLE), status, isProxyMockedResponse));
    }

    private static String getDirectionArrow(final LiveLoggingDirectionEnum direction) {

        switch (direction) {
            case REQUEST:
                return direction.name() + " " + INBOUND_ARROW;
            case RESPONSE:
                return OUTBOUND_ARROW + " " + direction.name();
            default:
                throw new IllegalArgumentException("Unsupported Live Logging Direction: " + direction);
        }

    }

}
