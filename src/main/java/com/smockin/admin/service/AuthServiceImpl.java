package com.smockin.admin.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.smockin.admin.dto.AuthDTO;
import com.smockin.admin.exception.AuthException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.SmockinUserDAO;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.persistence.enums.SmockinUserRoleEnum;
import com.smockin.utils.GeneralUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Stream;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    @Autowired
    private SmockinUserDAO smockinUserDAO;

    @Autowired
    private EncryptionService encryptionService;

    private final String jwtRoleKey = "role";
    private final String jwtSubjectKey = "smockin-access";
    private final String jwtIssuer = "smockin";
    private final String jwtSecret = "somesobsecuresecretkey";

    private final Algorithm jwtAlgorithm = Algorithm.HMAC256(jwtSecret);
    private final JWTVerifier jwtVerifier = JWT.require(jwtAlgorithm)
            .withIssuer(jwtIssuer)
            .build();

    public String authenticate(final AuthDTO dto) throws ValidationException, AuthException {
        logger.debug("authenticate called");

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

        final String token = generateJWT(user.getRole());

        user.setSessionToken(token);
        smockinUserDAO.save(user);

        return token;
    }

    String generateJWT(final SmockinUserRoleEnum role) {
        return JWT.create()
                .withIssuer(jwtIssuer)
                .withClaim(jwtRoleKey, role.name())
                .withSubject(jwtSubjectKey)
                .withIssuedAt(GeneralUtils.getCurrentDate())
                .withExpiresAt(GeneralUtils.toDate(GeneralUtils.getCurrentDateTime().plusDays(99)))
                .sign(jwtAlgorithm);
    }

    public void checkTokenRoles(final String jwt, SmockinUserRoleEnum... roles) throws AuthException {

        final DecodedJWT decodedJWT = jwtVerifier.verify(jwt);
        final Claim roleClaim = decodedJWT.getClaim(jwtRoleKey);

        if (roleClaim == null || !Stream.of(roles).anyMatch(r -> r.name().equals(roleClaim.asString()))) {
            throw new AuthException();
        }
    }

    public void verifyToken(final String jwt) throws AuthException {

        try {
            jwtVerifier.verify(jwt);
        } catch (JWTVerificationException ex) {
            logger.debug("JWT authorization failed", ex);
            throw new AuthException();
        }
    }

}
