package com.smockin.admin.persistence.migration.version;

import com.smockin.admin.persistence.dao.MigrationDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by gallina.
 */
@Component
public class MigrationPatch_170 implements MigrationPatch {

    @Autowired
    private MigrationDAO migrationDAO;

    @Override
    public String versionNo() {
        return "1.7.0";
    }

    @Override
    public void execute() {

        migrationDAO.buildNativeQuery("ALTER TABLE REST_MOCK DROP COLUMN REST_CATGY_ID;")
                .executeUpdate();

        migrationDAO.buildNativeQuery("DROP TABLE REST_CATGY;")
                .executeUpdate();

    }

}
