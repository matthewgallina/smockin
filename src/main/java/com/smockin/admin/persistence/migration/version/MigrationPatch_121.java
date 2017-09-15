package com.smockin.admin.persistence.migration.version;

import com.smockin.admin.persistence.dao.MigrationDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by gallina on 15/09/2017.
 */
public class MigrationPatch_121 implements MigrationPatch {

    private final Logger logger = LoggerFactory.getLogger(MigrationPatch_121.class);

    @Override
    public String versionNo() {
        return "1.2.1-SNAPSHOT";
    }

    @Override
    public void execute(final MigrationDAO migrationDAO) {
        logger.info("Running data migration patch for app version " + versionNo());

        migrationDAO.buildNativeQuery("ALTER TABLE REST_MOCK_RULE ALTER COLUMN RESPONSE_BODY VARCHAR2(2147483647);").executeUpdate();
        migrationDAO.buildNativeQuery("ALTER TABLE REST_MOCK_DEF ALTER COLUMN RESPONSE_BODY VARCHAR2(2147483647);").executeUpdate();

    }

}
