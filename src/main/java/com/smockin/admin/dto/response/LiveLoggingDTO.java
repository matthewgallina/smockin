package com.smockin.admin.dto.response;

import com.smockin.admin.enums.LiveLoggingDirectionEnum;

public class LiveLoggingDTO {

    private final String id;
    private final LiveLoggingDirectionEnum direction;
    private final boolean proxied;
    private final LiveLoggingContentDTO content;

    public LiveLoggingDTO(final String id, final LiveLoggingDirectionEnum direction, final boolean proxied, final LiveLoggingContentDTO content) {
        this.id = id;
        this.direction = direction;
        this.proxied = proxied;
        this.content = content;
    }

    public String getId() {
        return id;
    }
    public LiveLoggingDirectionEnum getDirection() {
        return direction;
    }
    public boolean isProxied() {
        return proxied;
    }
    public LiveLoggingContentDTO getContent() {
        return content;
    }

}

