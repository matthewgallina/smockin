package com.smockin.admin.persistence.migration.version;

import com.smockin.admin.persistence.dao.MigrationDAO;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.persistence.enums.SmockinUserRoleEnum;
import com.smockin.utils.GeneralUtils;

/**
 * Created by gallina.
 */
public class MigrationPatch_150 implements MigrationPatch {

    @Override
    public String versionNo() {
        return "1.5.0-SNAPSHOT";
    }

    @Override
    public void execute(final MigrationDAO migrationDAO) {

        migrationDAO.persist(new SmockinUser(
                "admin",
                "OL93piQZrHhrlK5YZn+BDQ5zypWEpzYTMr7v73tY9Teu8qGdm2JiZ/VIUpQVRk5J", // admin
                "Admin",
                "admin",
                SmockinUserRoleEnum.SYS_ADMIN,
                RecordStatusEnum.ACTIVE,
                GeneralUtils.generateUUID()));
    }

}
