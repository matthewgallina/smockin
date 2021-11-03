package com.smockin.utils;

import com.smockin.admin.dto.response.*;
import com.smockin.admin.enums.LiveLoggingDirectionEnum;
import com.smockin.admin.enums.LiveLoggingMessageTypeEnum;
import org.apache.commons.lang3.StringUtils;
import java.util.Map;

/**
 * Created by mgallina.
 */
public final class LiveLoggingUtils {

    private static final String NOT_AVAILABLE = "n/a";

    public static LiveLoggingDTO buildLiveLogInterceptedResponseDTO(final String reqId,
                                                                    final String url,
                                                                    final Integer status,
                                                                    final Map<String, String> headers,
                                                                    final String responseBody,
                                                                    final boolean viaProxy) {

        return new LiveLoggingDTO(LiveLoggingMessageTypeEnum.BLOCKED_RESPONSE,
                new LiveLoggingTrafficDTO(
                        reqId,
                        LiveLoggingDirectionEnum.RESPONSE,
                        viaProxy,
                        new LiveLoggingOutboundContentDTO(
                                url,
                                headers,
                                StringUtils.defaultIfBlank(responseBody, NOT_AVAILABLE), status)));
    }

    public static LiveLoggingDTO buildLiveLogInboundDTO(final String reqId,
                                                        final String method,
                                                        final String url,
                                                        final Map<String, String> headers,
                                                        final String reqBody,
                                                        final boolean viaProxy,
                                                        final Map<String, String> requestParams) {

        return new LiveLoggingDTO(LiveLoggingMessageTypeEnum.TRAFFIC,
                new LiveLoggingTrafficDTO(
                        reqId,
                        LiveLoggingDirectionEnum.REQUEST,
                        viaProxy,
                        new LiveLoggingInboundContentDTO(
                                headers,
                                method,
                                url,
                                StringUtils.defaultIfBlank(reqBody, NOT_AVAILABLE),
                                requestParams)));
    }

    public static LiveLoggingDTO buildLiveLogOutboundDTO(final String reqId,
                                                         final String url,
                                                         final Integer status,
                                                         final Map<String, String> headers,
                                                         final String responseBody,
                                                         final boolean viaProxy) {

        return new LiveLoggingDTO(LiveLoggingMessageTypeEnum.TRAFFIC,
                new LiveLoggingTrafficDTO(
                        reqId,
                        LiveLoggingDirectionEnum.RESPONSE,
                        viaProxy,
                        new LiveLoggingOutboundContentDTO(
                                url,
                                headers,
                                StringUtils.defaultIfBlank(responseBody, NOT_AVAILABLE),
                                status)));
    }

    public static LiveLoggingDTO buildS3LiveLogging(final String message) {

        final LiveLoggingS3DTO liveLoggingS3DTO = new LiveLoggingS3DTO(null, message);

        return new LiveLoggingDTO(LiveLoggingMessageTypeEnum.S3, liveLoggingS3DTO);
    }

}
