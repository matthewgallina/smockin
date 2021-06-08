package com.smockin.mockserver.dto;

import com.smockin.admin.persistence.enums.ProxyModeTypeEnum;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Created by mgallina.
 */
@Data
@NoArgsConstructor
public class ProxyForwardConfigResponseDTO extends ProxyForwardConfigDTO {

    private boolean proxyMode;

    public ProxyForwardConfigResponseDTO(final boolean proxyMode,
                                         final ProxyModeTypeEnum proxyModeType,
                                         final boolean doNotForwardWhen404Mock,
                                         final List<ProxyForwardMappingDTO> proxyForwardMappings) {

        super(proxyModeType, doNotForwardWhen404Mock, proxyForwardMappings);
        this.proxyMode = proxyMode;
    }

}
