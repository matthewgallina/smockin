package com.smockin.admin.persistence.migration;

import com.smockin.admin.exception.MigrationException;
import com.smockin.admin.persistence.migration.version.*;
import com.smockin.utils.GeneralUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
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
    private MigrationPatch_121 migrationPatch_121;

    @Autowired
    private MigrationPatch_130 migrationPatch_130;

    @Autowired
    private MigrationPatch_150 migrationPatch_150;

    @Autowired
    private MigrationPatch_151 migrationPatch_151;

    @Autowired
    private MigrationPatch_167 migrationPatch_167;

    @Autowired
    private MigrationPatch_170 migrationPatch_170;

    private final Set<MigrationPatch> patches = new HashSet<>();

    @Transactional
    public void applyVersionChanges(final String currentVersion, final String latestVersion) throws MigrationException {

        if (currentVersion == null) {
            // new app, no need to migrate
            return;
        }

        final int currentVersionNo = GeneralUtils.exactVersionNo(currentVersion);
        final int latestVersionNo = GeneralUtils.exactVersionNo(latestVersion);

        if (logger.isInfoEnabled()) {
            logger.info("Current Version No: " + currentVersionNo);
            logger.info("Latest Version No: " + latestVersionNo);
        }

        if (latestVersionNo == currentVersionNo) {
            // no app version change found, so nothing to migrate.
            return;
        }

        // Apply all patches for versions later then the previous version.
        for (MigrationPatch p : patches) {
            if (GeneralUtils.exactVersionNo(p.versionNo()) > currentVersionNo) {
                logger.info("Running data migration patch for app version " + p.versionNo());
                p.execute();
            }
        }

    }

    @PostConstruct
    public void after() {

        patches.add(migrationPatch_121);
        patches.add(migrationPatch_130);
        patches.add(migrationPatch_150);
        patches.add(migrationPatch_151);
        patches.add(migrationPatch_167);
        patches.add(migrationPatch_170);

    }

}
