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
public class ProxyForwardConfigCacheDTO extends ProxyForwardConfigDTO {

    private String createdByUserExtId;
    private String userCtxPath;

}
