package com.smockin.admin.persistence.migration.version;

import com.smockin.admin.persistence.dao.MigrationDAO;
import com.smockin.admin.persistence.enums.ServerTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by gallina.
 */
public class MigrationPatch_130 implements MigrationPatch {

    private final Logger logger = LoggerFactory.getLogger(MigrationPatch_130.class);

    @Override
    public String versionNo() {
        return "1.3.0-SNAPSHOT";
    }

    @Override
    public void execute(final MigrationDAO migrationDAO) {
        logger.info("Running data migration patch for app version " + versionNo());

        migrationDAO.buildNativeQuery("INSERT INTO SERVER_CONFIG_NATIVE_PROPERTIES (SERVER_CONFIG_ID, NATIVE_PROPERTIES_KEY, NATIVE_PROPERTIES) VALUES ( (select ID from SERVER_CONFIG where SERVER_TYPE = 'RESTFUL'), 'ENABLE_CORS', (select USE_CORS from SERVER_CONFIG where SERVER_TYPE = 'RESTFUL'));").executeUpdate();
        migrationDAO.buildNativeQuery("ALTER TABLE SERVER_CONFIG DROP COLUMN USE_CORS;").executeUpdate();

    }

}
