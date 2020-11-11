package com.smockin.utils;

import com.smockin.admin.dto.response.LiveLoggingDTO;
import com.smockin.admin.dto.response.LiveLoggingInboundContentDTO;
import com.smockin.admin.dto.response.LiveLoggingOutboundContentDTO;
import com.smockin.admin.enums.LiveLoggingDirectionEnum;
import org.apache.commons.lang3.StringUtils;
import java.util.Map;

/**
 * Created by mgallina.
 */
public final class LiveLoggingUtils {

    private static final String NOT_AVAILABLE = "n/a";

    public static LiveLoggingDTO buildLiveLogInboundDTO(final String reqId, final String method, final String url, final Map<String, String> headers, final String reqBody, final boolean viaProxy, final Map<String, String> requestParams) {

        return new LiveLoggingDTO(reqId, LiveLoggingDirectionEnum.REQUEST, viaProxy, new LiveLoggingInboundContentDTO(headers, method, url, StringUtils.defaultIfBlank(reqBody, NOT_AVAILABLE), requestParams));
    }

    public static LiveLoggingDTO buildLiveLogOutboundDTO(final String reqId, final Integer status, final Map<String, String> headers, final String responseBody, final boolean viaProxy) {

        return new LiveLoggingDTO(reqId, LiveLoggingDirectionEnum.RESPONSE, viaProxy, new LiveLoggingOutboundContentDTO(headers, StringUtils.defaultIfBlank(responseBody, NOT_AVAILABLE), status));
    }

}
