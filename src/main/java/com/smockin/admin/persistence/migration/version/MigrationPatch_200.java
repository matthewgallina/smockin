package com.smockin.admin.persistence.migration.version;

import com.smockin.admin.persistence.dao.MigrationDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by gallina.
 */
@Component
public class MigrationPatch_200 implements MigrationPatch {

    @Autowired
    private MigrationDAO migrationDAO;

    @Override
    public String versionNo() {
        return "2.0.0";
    }

    @Override
    public void execute() {

        migrationDAO.buildNativeQuery("DROP TABLE JMS_MOCK;")
                .executeUpdate();

        migrationDAO.buildNativeQuery("DROP TABLE FTP_MOCK;")
                .executeUpdate();

        migrationDAO.buildNativeQuery("DELETE FROM SERVER_CONFIG WHERE SERVER_TYPE IN ('JMS', 'FTP');")
                .executeUpdate();

    }

}
