package com.smockin.admin.persistence.dao;

import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.persistence.entity.SmockinUser;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Created by mgallina.
 */
public interface SmockinUserDAO extends JpaRepository<SmockinUser, Long> {

}
