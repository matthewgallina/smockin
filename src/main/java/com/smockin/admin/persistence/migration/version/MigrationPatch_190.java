package com.smockin.admin.persistence.migration.version;

import com.smockin.admin.persistence.dao.MigrationDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by gallina.
 */
@Component
public class MigrationPatch_190 implements MigrationPatch {

    @Autowired
    private MigrationDAO migrationDAO;

    @Override
    public String versionNo() {
        return "1.9.0";
    }

    @Override
    public void execute() {

        migrationDAO.buildNativeQuery("ALTER TABLE SERVER_CONFIG "
                + " DROP COLUMN AUTO_REFRESH;")
                .executeUpdate();

    }

}
