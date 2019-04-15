package com.smockin.admin.persistence.migration.version;

import com.smockin.admin.persistence.dao.MigrationDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by gallina.
 */
@Component
public class MigrationPatch_167 implements MigrationPatch {

    @Autowired
    private MigrationDAO migrationDAO;

    @Override
    public String versionNo() {
        return "1.6.7-SNAPSHOT";
    }

    @Override
    public void execute() {

        migrationDAO.buildNativeQuery("ALTER TABLE REST_MOCK DROP COLUMN PROXY_PRTY;")
                .executeUpdate();

    }

}
