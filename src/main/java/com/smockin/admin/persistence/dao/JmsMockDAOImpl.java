package com.smockin.admin.persistence.dao;

import com.smockin.admin.persistence.entity.JmsMock;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

/**
 * Created by mgallina.
 */
@Repository
public class JmsMockDAOImpl implements JmsMockDAOCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void detach(final JmsMock jmsQueueMock) {
        entityManager.detach(jmsQueueMock);
    }

    @Override
    public List<JmsMock> findAllByStatus(final RecordStatusEnum status) {
        return entityManager.createQuery("FROM JmsMock jqm WHERE jqm.status = :status ORDER BY jqm.name ASC")
                .setParameter("status", status)
                .getResultList();
    }

    @Override
    public List<JmsMock> findAll() {
        return entityManager.createQuery("FROM JmsMock jqm ORDER BY jqm.name ASC")
                .getResultList();
    }

}
