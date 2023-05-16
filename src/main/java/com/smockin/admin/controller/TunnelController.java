package com.smockin.admin.controller;

import com.smockin.admin.dto.TunnelRequestDTO;
import com.smockin.admin.dto.response.TunnelResponseDTO;
import com.smockin.admin.exception.AuthException;
import com.smockin.admin.service.TunnelService;
import com.smockin.utils.GeneralUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class TunnelController {

    static final String PATH = "/tunnel";

    @Autowired
    private TunnelService tunnelService;


    @RequestMapping(path=PATH,
                    method = RequestMethod.GET,
                    produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody
    ResponseEntity<TunnelResponseDTO> get(@RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken) {

        return ResponseEntity.ok(tunnelService.load(GeneralUtils.extractOAuthToken(bearerToken)));
    }

    @RequestMapping(path=PATH,
                    method = RequestMethod.PUT,
                    consumes = MediaType.APPLICATION_JSON_VALUE,
                    produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody
    ResponseEntity<TunnelResponseDTO> update(@RequestBody final TunnelRequestDTO dto,
                                             @RequestHeader(value = GeneralUtils.OAUTH_HEADER_NAME, required = false) final String bearerToken)
            throws AuthException {

        return ResponseEntity.ok(tunnelService.update(dto, GeneralUtils.extractOAuthToken(bearerToken)));
    }

}
