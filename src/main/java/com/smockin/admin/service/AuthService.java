package com.smockin.admin.service;

import com.smockin.admin.dto.AuthDTO;
import com.smockin.admin.exception.AuthException;
import com.smockin.admin.exception.ValidationException;

public interface AuthService {

    String authenticate(final AuthDTO dto) throws ValidationException, AuthException;

}
