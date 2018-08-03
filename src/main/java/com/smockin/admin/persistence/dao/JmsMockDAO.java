package com.smockin.admin.persistence.dao;

import com.smockin.admin.persistence.entity.JmsMock;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Created by mgallina.
 */
public interface JmsMockDAO extends JpaRepository<JmsMock, Long>, JmsMockDAOCustom {

    JmsMock findByExtId(final String extId);


}
