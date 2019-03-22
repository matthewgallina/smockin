package com.smockin.admin.persistence.migration.version;

import com.smockin.admin.persistence.dao.MigrationDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by gallina.
 */
@Component
public class MigrationPatch_164 implements MigrationPatch {

    @Autowired
    private MigrationDAO migrationDAO;

    @Override
    public String versionNo() {
        return "1.6.4-SNAPSHOT";
    }

    @Override
    public void execute() {

        migrationDAO.buildNativeQuery("ALTER TABLE REST_MOCK "
                + " ADD COLUMN RANDOM_LAT BOOLEAN NOT NULL DEFAULT FALSE")
            .executeUpdate();

        migrationDAO.buildNativeQuery("ALTER TABLE REST_MOCK "
                + " ADD COLUMN RDM_LAT_RANGE_MIN BIGINT NOT NULL")
            .executeUpdate();

        migrationDAO.buildNativeQuery("ALTER TABLE REST_MOCK "
                + " ADD COLUMN RDM_LAT_RANGE_MAX BIGINT NOT NULL")
            .executeUpdate();

    }

}
