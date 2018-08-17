package com.smockin.admin.persistence.dao;

import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

/**
 * Created by mgallina.
 */
@Repository
public class RestfulMockDAOImpl implements RestfulMockDAOCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void detach(final RestfulMock restfulMock) {
        entityManager.detach(restfulMock);
    }

    @Override
    public List<RestfulMock> findAllByStatus(final RecordStatusEnum status) {
        return entityManager.createQuery("FROM RestfulMock rm WHERE rm.status = :status ORDER BY rm.initializationOrder ASC")
                .setParameter("status", status)
                .getResultList();
    }

    @Override
    public List<RestfulMock> findAll() {
        return entityManager.createQuery("FROM RestfulMock rm ORDER BY rm.initializationOrder ASC")
                .getResultList();
    }

    @Override
    public List<RestfulMock> findAllByUser(final long userId) {
        return entityManager.createQuery("FROM RestfulMock rm WHERE rm.createdBy.id = :userId ORDER BY rm.initializationOrder ASC")
                .setParameter("userId", userId)
                .getResultList();
    }

    @Override
    public RestfulMock findByPathAndMethodAndUser(final String path, final RestMethodEnum method, final SmockinUser user) {
        try {
            return entityManager.createQuery("FROM RestfulMock rm WHERE rm.path = :path AND rm.method = :method AND rm.createdBy.id = :userId", RestfulMock.class)
                    .setParameter("path", path)
                    .setParameter("method", method)
                    .setParameter("userId", user.getId())
                    .getSingleResult();
        } catch (Throwable ex) {
            return null;
        }
    }

}
