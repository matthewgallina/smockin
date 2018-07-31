package com.smockin.admin.service;

import com.smockin.admin.dto.PasswordDTO;
import com.smockin.admin.dto.SmockinNewUserDTO;
import com.smockin.admin.dto.SmockinUserDTO;
import com.smockin.admin.dto.response.SmockinUserResponseDTO;
import com.smockin.admin.enums.UserModeEnum;
import com.smockin.admin.exception.AuthException;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.entity.SmockinUser;

import java.util.List;
import java.util.Optional;

/**
 * Created by gallina on 26/05/2018.
 */
public interface SmockinUserService {

    List<SmockinUserResponseDTO> loadAllUsers(final String token) throws RecordNotFoundException, AuthException;
    String createUser(final SmockinNewUserDTO dto, final String token) throws ValidationException, RecordNotFoundException, AuthException;
    void updateUser(final String externalId, final SmockinUserDTO dto, final String token) throws ValidationException, RecordNotFoundException, AuthException;
    void deleteUser(final String extId, final String token) throws ValidationException, RecordNotFoundException, AuthException;
    void resetPassword(final PasswordDTO dto, final String token) throws ValidationException, RecordNotFoundException, AuthException;
    void resetToken(final String token) throws RecordNotFoundException;
    void lookUpToken(final String sessionToken) throws AuthException;
    UserModeEnum getUserMode();
    SmockinUser loadCurrentUser(final String sessionToken) throws RecordNotFoundException;
    Optional<SmockinUser> loadDefaultUser();

}
