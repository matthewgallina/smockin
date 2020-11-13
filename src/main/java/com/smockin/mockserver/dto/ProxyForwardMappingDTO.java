package com.smockin.mockserver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by mgallina.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProxyForwardMappingDTO {

    private String path;
    private String proxyForwardUrl;
    private boolean disabled;

}
