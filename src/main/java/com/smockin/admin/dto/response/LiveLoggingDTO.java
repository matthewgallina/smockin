package com.smockin.admin.dto.response;

import com.smockin.admin.enums.LiveLoggingMessageTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LiveLoggingDTO<P> {

    private LiveLoggingMessageTypeEnum type;
    private P payload;

}

