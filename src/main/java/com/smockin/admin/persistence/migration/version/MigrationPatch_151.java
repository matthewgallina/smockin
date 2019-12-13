package com.smockin.admin.persistence.migration.version;

import com.smockin.admin.persistence.dao.MigrationDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by gallina.
 */
@Component
public class MigrationPatch_151 implements MigrationPatch {

    @Autowired
    private MigrationDAO migrationDAO;

    @Override
    public String versionNo() {
        return "1.5.1-SNAPSHOT";
    }

    @Override
    public void execute() {

        migrationDAO.buildNativeQuery("INSERT INTO SERVER_CONFIG_NATIVE_PROPERTIES "
                + " (SERVER_CONFIG_ID, NATIVE_PROPERTIES_KEY, NATIVE_PROPERTIES) "
                + " VALUES "
                + " ((select ID from SERVER_CONFIG where SERVER_TYPE = 'RESTFUL'), 'PROXY_SERVER_ENABLED', 'FALSE');")
            .executeUpdate();

        migrationDAO.buildNativeQuery("INSERT INTO SERVER_CONFIG_NATIVE_PROPERTIES "
                + " (SERVER_CONFIG_ID, NATIVE_PROPERTIES_KEY, NATIVE_PROPERTIES) "
                + " VALUES "
                + " ((select ID from SERVER_CONFIG where SERVER_TYPE = 'RESTFUL'), 'PROXY_SERVER_PORT', '8010');")
                .executeUpdate();

    }

}
