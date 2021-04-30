package com.smockin.admin.persistence.dao;

import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.persistence.enums.SmockinUserRoleEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Created by mgallina.
 */
public interface SmockinUserDAO extends JpaRepository<SmockinUser, Long> {

    SmockinUser findByUsername(final String username);
    SmockinUser findByExtId(final String extId);
    SmockinUser findBySessionToken(final String sessionToken);
    List<SmockinUser> findAllByRole(final SmockinUserRoleEnum role);
    @Query("FROM SmockinUser su WHERE su.passwordResetToken = :passwordResetToken AND su.passwordResetTokenExpiry != NULL AND su.passwordResetTokenExpiry > CURRENT_DATE")
    SmockinUser findByValidPasswordResetToken(@Param("passwordResetToken") final String passwordResetToken);
    boolean existsSmockinUserByUsername(final String username);
    @Query("SELECT count(su) > 0 FROM SmockinUser su WHERE su.ctxPath = :ctxPath")
    boolean doesUserExistWithCtxPath(final String ctxPath);

}
