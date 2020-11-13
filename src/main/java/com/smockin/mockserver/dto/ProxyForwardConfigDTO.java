package com.smockin.mockserver.dto;

import com.smockin.admin.persistence.enums.ProxyModeTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Created by mgallina.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProxyForwardConfigDTO {

    private boolean proxyMode;
    private ProxyModeTypeEnum proxyModeType;
    private boolean doNotForwardWhen404Mock;
    private List<ProxyForwardMappingDTO> proxyForwardMappings;

}
