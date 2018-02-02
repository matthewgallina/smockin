package com.smockin.admin.persistence.dao;

import com.smockin.admin.persistence.entity.FtpMock;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

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

}
