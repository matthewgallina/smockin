package com.smockin.admin.persistence.migration;

import com.smockin.admin.persistence.dao.MigrationDAO;
import com.smockin.admin.persistence.migration.version.MigrationPatch;
import com.smockin.admin.persistence.migration.version.MigrationPatch_121;
import com.smockin.admin.persistence.migration.version.MigrationPatch_130;
import com.smockin.admin.persistence.migration.version.MigrationPatch_141;
import com.smockin.utils.GeneralUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Created by gallina.
 */
@Service
public class DataMigrationService {

    private final Logger logger = LoggerFactory.getLogger(DataMigrationService.class);

    // NOTE
    // Need to consider that some users may be using a different DB to H2 and factor that in when writing native SQL patches.

    @Autowired
    private MigrationDAO migrationDAO;

    private final Set<MigrationPatch> patches = Collections.unmodifiableSet(new HashSet<MigrationPatch>() {
        {
            add(new MigrationPatch_121());
            add(new MigrationPatch_130());
            add(new MigrationPatch_141());
        }
    });

    @Transactional
    public void applyVersionChanges(final String currentVersion, final String latestVersion) {

        if (currentVersion == null) {
            // new app, no need to migrate
            return;
        }

        final int currentVersionNo = GeneralUtils.exactVersionNo(currentVersion);
        final int latestVersionNo = GeneralUtils.exactVersionNo(latestVersion);

        if (latestVersionNo == currentVersionNo) {
            // no app version change found, so nothing to migrate.
            return;
        }

        // Apply all patches for versions later then the previous version.
        for (MigrationPatch p : patches) {
            if (GeneralUtils.exactVersionNo(p.versionNo()) > currentVersionNo) {
                logger.info("Running data migration patch for app version " + p.versionNo());
                p.execute(migrationDAO);
            }
        }

    }

}
