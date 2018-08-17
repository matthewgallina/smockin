package com.smockin.admin.persistence.migration.version;

import com.smockin.admin.exception.MigrationException;

/**
 * Created by gallina on 15/09/2017.
 */
public interface MigrationPatch {

    String versionNo();
    void execute() throws MigrationException;

}
