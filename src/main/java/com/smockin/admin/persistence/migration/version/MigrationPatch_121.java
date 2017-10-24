package com.smockin.admin.persistence.migration.version;

import com.smockin.admin.persistence.dao.MigrationDAO;

/**
 * Created by gallina on 15/09/2017.
 */
public class MigrationPatch_121 implements MigrationPatch {

    @Override
    public String versionNo() {
        return "1.2.1-SNAPSHOT";
    }

    @Override
    public void execute(final MigrationDAO migrationDAO) {

        migrationDAO.buildNativeQuery("ALTER TABLE REST_MOCK_RULE ALTER COLUMN RESPONSE_BODY VARCHAR2(2147483647) NULL;").executeUpdate();
        migrationDAO.buildNativeQuery("ALTER TABLE REST_MOCK_DEF ALTER COLUMN RESPONSE_BODY VARCHAR2(2147483647) NULL;").executeUpdate();
    }

}
