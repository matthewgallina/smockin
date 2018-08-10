package com.smockin.admin.persistence.migration.version;

import com.smockin.admin.persistence.dao.MigrationDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by gallina on 15/09/2017.
 */
@Component
public class MigrationPatch_121 implements MigrationPatch {

    @Autowired
    private MigrationDAO migrationDAO;

    @Override
    public String versionNo() {
        return "1.2.1-SNAPSHOT";
    }

    @Override
    public void execute() {

        migrationDAO.buildNativeQuery("ALTER TABLE REST_MOCK_RULE ALTER COLUMN RESPONSE_BODY VARCHAR2(2147483647) NULL;").executeUpdate();
        migrationDAO.buildNativeQuery("ALTER TABLE REST_MOCK_DEF ALTER COLUMN RESPONSE_BODY VARCHAR2(2147483647) NULL;").executeUpdate();
    }

}
