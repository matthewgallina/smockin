package com.smockin.admin.dto;

import com.smockin.admin.persistence.enums.RestMethodEnum;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LiveLoggingBlockingEndpointDTO {

    private RestMethodEnum method;
    private String path;

}
