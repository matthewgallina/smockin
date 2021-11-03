package com.smockin.admin.persistence.migration.version;

import com.smockin.admin.persistence.dao.ServerConfigDAO;
import com.smockin.admin.persistence.entity.ServerConfig;
import com.smockin.admin.persistence.enums.ServerTypeEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by gallina.
 */
@Component
public class MigrationPatch_2170 implements MigrationPatch {

    @Autowired
    private ServerConfigDAO serverConfigDAO;

    @Override
    public String versionNo() {
        return "2.17.0";
    }

    @Override
    public void execute() {

        // Add S3 default config
        final ServerConfig s3ServerConfig = new ServerConfig();

        s3ServerConfig.setPort(8002);
        s3ServerConfig.setMinThreads(10);
        s3ServerConfig.setMaxThreads(10);
        s3ServerConfig.setTimeOutMillis(20000);
        s3ServerConfig.setServerType(ServerTypeEnum.S3);

        serverConfigDAO.save(s3ServerConfig);

    }

}
