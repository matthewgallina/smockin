package com.smockin.admin.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class LiveLoggingBlockedResponseAmendmentDTO {

    private String traceId;
    private int status;
    private Map<String, String> headers;
    private String body;

}
