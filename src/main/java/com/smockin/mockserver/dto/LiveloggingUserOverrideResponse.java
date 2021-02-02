package com.smockin.mockserver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@Data
@AllArgsConstructor
@EqualsAndHashCode
public class LiveloggingUserOverrideResponse {

    private int status;
    private String contentType;
    private Map<String, String> responseHeaders;
    private String body;

}
