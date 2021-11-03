package com.smockin.admin.dto.response;

import com.smockin.admin.enums.LiveLoggingDirectionEnum;
import com.smockin.utils.GeneralUtils;

import java.util.Date;

public class LiveLoggingTrafficDTO implements LiveLoggingPayloadDTO {

    private final String id;
    private final LiveLoggingDirectionEnum direction;
    private final Date date;
    private final boolean proxied;
    private final LiveLoggingContentDTO content;

    public LiveLoggingTrafficDTO(final String id,
                                 final LiveLoggingDirectionEnum direction,
                                 final boolean proxied,
                                 final LiveLoggingContentDTO content) {
        this.id = id;
        this.direction = direction;
        this.date = GeneralUtils.getCurrentDate();
        this.proxied = proxied;
        this.content = content;
    }

    public String getId() {
        return id;
    }
    public LiveLoggingDirectionEnum getDirection() {
        return direction;
    }
    public Date getDate() {
        return date;
    }
    public boolean isProxied() {
        return proxied;
    }
    public LiveLoggingContentDTO getContent() {
        return content;
    }

}

