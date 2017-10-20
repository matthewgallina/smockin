package com.smockin.admin.persistence.dao;

import com.smockin.admin.persistence.entity.JmsQueueMock;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Created by mgallina.
 */
public interface JmsQueueMockDAO extends JpaRepository<JmsQueueMock, Long>, JmsQueueMockDAOCustom {

}
