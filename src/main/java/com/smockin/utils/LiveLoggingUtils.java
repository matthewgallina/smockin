package com.smockin.utils;

import org.apache.commons.lang3.StringUtils;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Created by mgallina.
 */
public final class LiveLoggingUtils {

    private static final String INBOUND_ARROW = "-------------------------------------------------------->";
    private static final String OUTBOUND_ARROW = "<--------------------------------------------------------";
    private static final String DISPLAY_TIME_FORMAT = "HH:mm:ss:SSS";
    private static final String NOT_AVAILABLE = "n/a";
    private static final String CARRIAGE_RET = "\n";

    // Thread safe
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(DISPLAY_TIME_FORMAT)
            .withZone(ZoneId.systemDefault());

    private enum LiveLoggingDirection {
        INBOUND, OUTBOUND
    }

    public static String buildLiveLogInboundEntry(final String reqId, final String method, final String url, final String contentType, final String reqBody, final boolean viaProxy) {

        final StringBuilder sb = buildLiveLogHeader(LiveLoggingDirection.INBOUND, reqId, (viaProxy) ? "(via proxy)" : null);

        sb.append("method: ");
        sb.append(method);
        sb.append(CARRIAGE_RET);
        sb.append("url: ");
        sb.append(url);
        sb.append(CARRIAGE_RET);
        sb.append("content type: ");
        sb.append(StringUtils.defaultIfBlank(contentType, NOT_AVAILABLE));
        sb.append(CARRIAGE_RET);
        sb.append("body: ");
        sb.append(StringUtils.defaultIfBlank(reqBody, NOT_AVAILABLE));
        sb.append(CARRIAGE_RET);
        sb.append(getDirectionArrow(LiveLoggingDirection.INBOUND));

        return sb.toString();
    }

    public static String buildLiveLogOutboundEntry(final String reqId, final int status, final String contentType, final String responseBody, final boolean viaProxy, final boolean isProxyMockedResponse) {

        final StringBuilder sb = buildLiveLogHeader(LiveLoggingDirection.OUTBOUND, reqId, ((viaProxy) ? "(via proxy -> " + ((isProxyMockedResponse) ? "mocked " : "original") + " response)" : null));

        sb.append("status: ");
        sb.append(status);
        sb.append(CARRIAGE_RET);
        sb.append("content type: ");
        sb.append(StringUtils.defaultIfBlank(contentType, NOT_AVAILABLE));
        sb.append(CARRIAGE_RET);
        sb.append("body: ");
        sb.append(StringUtils.defaultIfBlank(responseBody, NOT_AVAILABLE));
        sb.append(CARRIAGE_RET);
        sb.append(getDirectionArrow(LiveLoggingDirection.OUTBOUND));

        return sb.toString();
    }

    private static StringBuilder buildLiveLogHeader(final LiveLoggingDirection direction, final String reqId, final String directionMeta) {

        final StringBuilder sb = new StringBuilder();

        sb.append(getDirectionArrow(direction));
        sb.append(CARRIAGE_RET);

        sb.append(direction.name());

        if (directionMeta != null) {
            sb.append(" ");
            sb.append(directionMeta);
        }

        sb.append(CARRIAGE_RET);

        sb.append("id: ");
        sb.append(reqId);
        sb.append(CARRIAGE_RET);

        sb.append("time: ");
        sb.append(timeFormatter.format(GeneralUtils.getCurrentDateTime()));
        sb.append(CARRIAGE_RET);

        return sb;
    }

    private static String getDirectionArrow(final LiveLoggingDirection direction) {

        switch (direction) {
            case INBOUND:
                return INBOUND_ARROW;
            case OUTBOUND:
                return OUTBOUND_ARROW;
            default:
                throw new IllegalArgumentException("Unsupported Live Logging Direction: " + direction);
        }

    }

}
