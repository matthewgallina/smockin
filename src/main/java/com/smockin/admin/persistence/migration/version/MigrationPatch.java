package com.smockin.admin.persistence.migration.version;

import com.smockin.admin.persistence.dao.MigrationDAO;

/**
 * Created by gallina on 15/09/2017.
 */
public interface MigrationPatch {

    String versionNo();
    void execute(final MigrationDAO migrationDAO);

}
