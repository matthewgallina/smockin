package com.smockin.admin.dto.response;

import com.smockin.admin.dto.TunnelRequestDTO;
import lombok.Data;

@Data
@lombok.EqualsAndHashCode(callSuper = true)
public class TunnelResponseDTO extends TunnelRequestDTO {

    private String uri;

    public TunnelResponseDTO(boolean enabled, String uri) {
        super(enabled);
        this.uri = uri;
    }
}
