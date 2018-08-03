package com.smockin.admin.persistence.dao;

import com.smockin.admin.persistence.entity.FtpMock;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

/**
 * Created by mgallina on 02/02/18.
 */
@Repository
public class FtpMockDAOImpl implements FtpMockDAOCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void detach(final FtpMock ftpMock) {
        entityManager.detach(ftpMock);
    }

    @Override
    public List<FtpMock> findAllByStatus(final RecordStatusEnum status) {
        return entityManager.createQuery("FROM FtpMock fm WHERE fm.status = :status ORDER BY fm.name ASC")
                .setParameter("status", status)
                .getResultList();
    }

    @Override
    public List<FtpMock> findAllByUser(final long userId) {
        return entityManager.createQuery("FROM FtpMock fm WHERE fm.createdBy.id = :userId ORDER BY fm.name ASC")
                .setParameter("userId", userId)
                .getResultList();
    }

}
