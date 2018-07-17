package com.smockin.admin.persistence.dao;

import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.persistence.enums.SmockinUserRoleEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Created by mgallina.
 */
public interface SmockinUserDAO extends JpaRepository<SmockinUser, Long> {

    SmockinUser findByUsername(final String username);
    SmockinUser findByExtId(final String extId);
    SmockinUser findBySessionToken(final String sessionToken);
    List<SmockinUser> findAllByRole(final SmockinUserRoleEnum role);

}
