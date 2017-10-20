package com.smockin.admin.persistence.dao;

import com.smockin.admin.persistence.entity.JmsQueueMock;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

/**
 * Created by mgallina.
 */
@Repository
public class JmsQueueMockDAOImpl implements JmsQueueMockDAOCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void detach(final JmsQueueMock jmsQueueMock) {
        entityManager.detach(jmsQueueMock);
    }

    @Override
    public List<JmsQueueMock> findAllByStatus(final RecordStatusEnum status) {
        return entityManager.createQuery("FROM JmsQueueMock jqm WHERE jqm.status = :status ORDER BY jqm.name ASC")
                .setParameter("status", status)
                .getResultList();
    }

    @Override
    public List<JmsQueueMock> findAll() {
        return entityManager.createQuery("FROM JmsQueueMock jqm ORDER BY jqm.name ASC")
                .getResultList();
    }

}
