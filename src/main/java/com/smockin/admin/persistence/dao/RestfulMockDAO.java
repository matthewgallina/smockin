package com.smockin.admin.persistence.dao;

import com.smockin.admin.persistence.entity.RestfulMock;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Created by mgallina.
 */
public interface RestfulMockDAO extends JpaRepository<RestfulMock, Long>, RestfulMockDAOCustom {

    RestfulMock findByExtId(final String extId);

}
