package com.smockin.admin.controller;

import com.smockin.admin.dto.*;
import com.smockin.admin.dto.response.SimpleMessageResponseDTO;
import com.smockin.admin.exception.AuthException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * Created by mgallina.
 */
@Controller
public class AuthController {

    @Autowired
    private AuthService authService;

    @RequestMapping(path="/auth", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<SimpleMessageResponseDTO> authenticate(@RequestBody final AuthDTO dto)
                                                                                throws ValidationException, AuthException {
        return ResponseEntity.ok(new SimpleMessageResponseDTO(authService.authenticate(dto)));
    }

}
