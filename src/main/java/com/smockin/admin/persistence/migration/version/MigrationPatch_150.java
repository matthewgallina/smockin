package com.smockin.admin.persistence.migration.version;

import com.smockin.admin.exception.MigrationException;
import com.smockin.admin.persistence.dao.MigrationDAO;
import com.smockin.admin.persistence.dao.SmockinUserDAO;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.persistence.enums.SmockinUserRoleEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by gallina.
 */
@Component
public class MigrationPatch_150 implements MigrationPatch {

    @Autowired
    private MigrationDAO migrationDAO;

    @Autowired
    private SmockinUserDAO smockinUserDAO;

    @Override
    public String versionNo() {
        return "1.5.0-SNAPSHOT";
    }

    @Override
    public void execute() {

        final SmockinUser sysAdmin = smockinUserDAO.findAllByRole(SmockinUserRoleEnum.SYS_ADMIN)
                .stream()
                .findFirst()
                .orElseThrow(() -> new MigrationException("Error retrieving SYS_ADMIN user"));

        // REST_MOCK
        migrationDAO.buildNativeQuery("UPDATE REST_MOCK SET CREATED_BY = :userId")
                .setParameter("userId", sysAdmin.getId())
                .executeUpdate();

        // JMS_MOCK
        migrationDAO.buildNativeQuery("UPDATE JMS_MOCK SET CREATED_BY = :userId")
                .setParameter("userId", sysAdmin.getId())
                .executeUpdate();

        // FTP_MOCK
        migrationDAO.buildNativeQuery("UPDATE FTP_MOCK SET CREATED_BY = :userId")
                .setParameter("userId", sysAdmin.getId())
                .executeUpdate();

    }

}
