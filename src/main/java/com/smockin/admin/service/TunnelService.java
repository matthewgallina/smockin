package com.smockin.admin.service;

import com.smockin.admin.dto.TunnelRequestDTO;
import com.smockin.admin.dto.response.TunnelResponseDTO;
import com.smockin.admin.exception.AuthException;

public interface TunnelService {

    TunnelResponseDTO load(final String token);
    TunnelResponseDTO update(final TunnelRequestDTO dto, final String token) throws AuthException;

}
