package com.smockin.admin.persistence.migration.version;

import com.smockin.admin.persistence.dao.MigrationDAO;

/**
 * Created by gallina.
 */
public class MigrationPatch_150 implements MigrationPatch {

    @Override
    public String versionNo() {
        return "1.5.0-SNAPSHOT";
    }

    @Override
    public void execute(final MigrationDAO migrationDAO) {

        // REST_MOCK
        migrationDAO.buildNativeQuery("ALTER TABLE REST_MOCK ADD COLUMN CREATED_BY INT;").executeUpdate();
        migrationDAO.buildNativeQuery("ALTER TABLE REST_MOCK ADD FOREIGN KEY CREATED_BY REFERENCES SMKN_USER(ID);").executeUpdate();

        // JMS_MOCK
        migrationDAO.buildNativeQuery("ALTER TABLE JMS_MOCK ADD COLUMN CREATED_BY INT;").executeUpdate();
        migrationDAO.buildNativeQuery("ALTER TABLE JMS_MOCK ADD FOREIGN KEY CREATED_BY REFERENCES SMKN_USER(ID);").executeUpdate();

        // FTP_MOCK
        migrationDAO.buildNativeQuery("ALTER TABLE FTP_MOCK ADD COLUMN CREATED_BY INT;").executeUpdate();
        migrationDAO.buildNativeQuery("ALTER TABLE FTP_MOCK ADD FOREIGN KEY CREATED_BY REFERENCES SMKN_USER(ID);").executeUpdate();

    }

}
