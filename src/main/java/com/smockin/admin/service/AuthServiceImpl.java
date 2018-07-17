package com.smockin.admin.service;

import com.smockin.admin.dto.AuthDTO;
import com.smockin.admin.exception.AuthException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.SmockinUserDAO;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.utils.GeneralUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    @Autowired
    private SmockinUserDAO smockinUserDAO;

    @Autowired
    private EncryptionService encryptionService;

    public String authenticate(final AuthDTO dto) throws ValidationException, AuthException {

        if (StringUtils.isBlank(dto.getUsername())) {
            throw new ValidationException("username is required");
        }

        if (StringUtils.isBlank(dto.getPassword())) {
            throw new ValidationException("password is required");
        }

        final SmockinUser user = smockinUserDAO.findByUsername(dto.getUsername());

        if (user == null
                || !encryptionService.verify(dto.getPassword(), user.getPassword())) {
            throw new AuthException();
        }

        final String sessionToken = GeneralUtils.generateUUID();

        user.setSessionToken(sessionToken);
        smockinUserDAO.save(user);

        return sessionToken;
    }

}
