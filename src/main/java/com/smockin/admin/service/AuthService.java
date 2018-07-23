package com.smockin.admin.service;

import com.smockin.admin.dto.AuthDTO;
import com.smockin.admin.exception.AuthException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.enums.SmockinUserRoleEnum;

public interface AuthService {

    String authenticate(final AuthDTO dto) throws ValidationException, AuthException;
    void checkTokenRoles(final String jwt, SmockinUserRoleEnum... roles) throws AuthException;
    void verifyToken(final String jwt) throws AuthException;

}
