package com.smockin.admin.persistence.migration.version;

import com.smockin.admin.persistence.dao.MigrationDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by gallina.
 */
@Component
public class MigrationPatch_290 implements MigrationPatch {

    @Autowired
    private MigrationDAO migrationDAO;

    @Override
    public String versionNo() {
        return "2.9.0";
    }

    @Override
    public void execute() {

        migrationDAO.buildNativeQuery("UPDATE SERVER_CONFIG SET PROXY_MODE_TYPE = 'ACTIVE' WHERE SERVER_TYPE = 'RESTFUL';").executeUpdate();

    }

}
