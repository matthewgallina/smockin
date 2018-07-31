package com.smockin.admin.service;

import com.smockin.admin.dto.PasswordDTO;
import com.smockin.admin.dto.SmockinNewUserDTO;
import com.smockin.admin.dto.SmockinUserDTO;
import com.smockin.admin.dto.response.SmockinUserResponseDTO;
import com.smockin.admin.enums.UserModeEnum;
import com.smockin.admin.exception.AuthException;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.AppConfigDAO;
import com.smockin.admin.persistence.dao.SmockinUserDAO;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.persistence.enums.SmockinUserRoleEnum;
import com.smockin.utils.GeneralUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by gallina on 26/05/2018.
 */
@Service
@Transactional
public class SmockinUserServiceImpl implements SmockinUserService {

    final String USERNAME_REGEX = "^[a-zA-Z0-9]*$"; // alphanumeric only (and no spaces)
    final String PASSWORD_REGEX = "(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=\\S+$).{8,}"; // min 1 digit, 1 upper, 1 lower (and no spaces)

    @Autowired
    private EncryptionService encryptionService;

    @Autowired
    private SmockinUserDAO smockinUserDAO;

    @Autowired
    private AppConfigDAO appConfigDAO;

    @Value("${multi.user.mode:false}")
    private boolean multiUserMode;

    @Override
    public List<SmockinUserResponseDTO> loadAllUsers(final String token) throws RecordNotFoundException, AuthException {

        assertCurrentUserIsAdmin(token);

        return smockinUserDAO
                .findAll()
                .stream()
                .map(u -> new SmockinUserResponseDTO(u.getExtId(), u.getDateCreated(), u.getUsername(), u.getFullName(), u.getRole()))
                .collect(Collectors.toList());

    }

    @Override
    public String createUser(final SmockinNewUserDTO dto, final String token) throws ValidationException, RecordNotFoundException, AuthException {

        assertCurrentUserIsAdmin(token);

        validateDTO(dto);

        if (SmockinUserRoleEnum.SYS_ADMIN.equals(dto.getRole())) {
            throw new ValidationException("This role is not permitted");
        }

        final String passwordEnc = validateAndEncryptPassword(dto.getPassword());

        return smockinUserDAO
                .save(new SmockinUser(dto.getUsername(), passwordEnc, dto.getFullName(), dto.getUsername(), SmockinUserRoleEnum.REGULAR, RecordStatusEnum.ACTIVE, GeneralUtils.generateUUID()))
                .getExtId();
    }

    @Override
    public void updateUser(final String externalId, final SmockinUserDTO dto, final String token) throws ValidationException, RecordNotFoundException, AuthException {

        assertCurrentUserIsAdmin(token);

        validateDTO(dto);

        final SmockinUser smockinUser = loadUserByExtId(externalId);

        if (!SmockinUserRoleEnum.SYS_ADMIN.equals(smockinUser.getRole())
                && SmockinUserRoleEnum.SYS_ADMIN.equals(dto.getRole())) {
            throw new ValidationException("Updating to this role is not permitted");
        }

        smockinUser.setFullName(dto.getFullName());
        smockinUser.setUsername(dto.getUsername());
        smockinUser.setRole(dto.getRole());
        smockinUser.setCtxPath(dto.getUsername());

        smockinUserDAO.save(smockinUser);
    }

    @Override
    public void deleteUser(final String externalId, final String token) throws ValidationException, RecordNotFoundException, AuthException {

        assertCurrentUserIsAdmin(token);

        // Admin user can never be deleted.
        final SmockinUser smockinUser = loadUserByExtId(externalId);

        if (SmockinUserRoleEnum.SYS_ADMIN.equals(smockinUser.getRole())) {
            throw new ValidationException("System Admin user cannot be deleted");
        }

        smockinUserDAO.delete(smockinUser);
    }

    @Override
    public void resetPassword(final PasswordDTO dto, final String token) throws ValidationException, RecordNotFoundException, AuthException {

        final SmockinUser currentUser = loadCurrentUser(token);

        if (!encryptionService.verify(dto.getCurrentPassword(), currentUser.getPassword())) {
            throw new AuthException();
        }

        currentUser.setPassword(validateAndEncryptPassword(dto.getNewPassword()));

        smockinUserDAO.save(currentUser);
    }

    @Override
    public void resetToken(final String token) throws RecordNotFoundException {

        final SmockinUser currentUser = loadCurrentUser(token);

        currentUser.setSessionToken(GeneralUtils.generateUUID()); // using UUID simply to void this with a unique value.

        smockinUserDAO.save(currentUser);
    }

    @Override
    public void lookUpToken(final String sessionToken) throws AuthException {

        if (smockinUserDAO.findBySessionToken(sessionToken) == null) {
            throw new AuthException();
        }
    }

    @Override
    public UserModeEnum getUserMode() {
        return (multiUserMode) ? UserModeEnum.ACTIVE : UserModeEnum.INACTIVE;
    }

    @Override
    public SmockinUser loadCurrentUser(final String sessionToken) throws RecordNotFoundException {

        final SmockinUser smockinUser = smockinUserDAO.findBySessionToken(sessionToken);

        if (smockinUser == null) {
            throw new RecordNotFoundException();
        }

        return smockinUser;
    }

    @Override
    public Optional<SmockinUser> loadDefaultUser() {
        return smockinUserDAO.findAllByRole(SmockinUserRoleEnum.SYS_ADMIN).stream().findFirst();
    }

    void validateDTO(final SmockinUserDTO dto) throws ValidationException {

        if (StringUtils.isBlank(dto.getFullName())) {
            throw new ValidationException("fullname is required");
        }
        if (StringUtils.isBlank(dto.getUsername())) {
            throw new ValidationException("username is required");
        }
        if (!dto.getUsername().matches(USERNAME_REGEX)) {
            throw new ValidationException("username can only contain alphanumeric characters and no spaces");
        }
    }

    String validateAndEncryptPassword(final String passwordPlain) throws ValidationException {

        if (StringUtils.isBlank(passwordPlain)) {
            throw new ValidationException("password is required");
        }
        if (!passwordPlain.matches(PASSWORD_REGEX)) {
            throw new ValidationException("Invalid password (min 8 chars, 1 digit, 1 upper case & 1 lower case)");
        }

        return encryptionService.encrypt(passwordPlain);
    }

    void assertCurrentUserIsAdmin(final String token) throws RecordNotFoundException, AuthException {

        final SmockinUserRoleEnum userRole = loadCurrentUser(token).getRole();

        if (!SmockinUserRoleEnum.ADMIN.equals(userRole)
                && !SmockinUserRoleEnum.SYS_ADMIN.equals(userRole)) {
            throw new AuthException();
        }

    }

    SmockinUser loadUserByExtId(final String externalId) throws RecordNotFoundException {

        final SmockinUser smockinUser = smockinUserDAO.findByExtId(externalId);

        if (smockinUser == null) {
            throw new RecordNotFoundException();
        }

        return smockinUser;
    }

}
