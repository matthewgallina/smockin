package com.smockin.admin.persistence;

import com.smockin.admin.persistence.dao.AppConfigDAO;
import com.smockin.admin.persistence.dao.ServerConfigDAO;
import com.smockin.admin.persistence.entity.AppConfig;
import com.smockin.admin.persistence.entity.ServerConfig;
import com.smockin.admin.persistence.enums.ServerTypeEnum;
import com.smockin.admin.persistence.migration.DataMigrationService;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Created by mgallina.
 */
@Component
public class CoreDataHandler {

    private final Logger logger = LoggerFactory.getLogger(CoreDataHandler.class);

    @Autowired
    private ServerConfigDAO serverConfigDAO;

    @Autowired
    private AppConfigDAO appConfigDAO;

    @Autowired
    private DataMigrationService dataMigrationService;

    @Transactional
    public void exec() {

        applyServerConfigDefaults();

        applyAppVersioning();

    }

    void applyServerConfigDefaults() {

        // Check if Server Config DB Defaults have already been installed
        if (!serverConfigDAO.findAll().isEmpty()) {
            return;
        }

        logger.info("Server Config DB Defaults being executed...");

        final ServerConfig restServerConfig = new ServerConfig();
        restServerConfig.setServerType(ServerTypeEnum.RESTFUL);
        restServerConfig.setPort(8001);
        restServerConfig.setMaxThreads(100);
        restServerConfig.setMinThreads(10);
        restServerConfig.setTimeOutMillis(30000);
        restServerConfig.setAutoStart(false);
        restServerConfig.setAutoRefresh(false);
        restServerConfig.setEnableCors(false);

        serverConfigDAO.save(restServerConfig);

        logger.info("Server Config DB Defaults    DONE");
    }

    void applyAppVersioning() {

        final String appVersionArg = System.getProperty("app.version");

        if (appVersionArg == null) {
            logger.error("Invalid application version arg (-Dapp.version): " + appVersionArg);
            return;
        }

        final List<AppConfig> allAppConfig = appConfigDAO.findAll();

        final AppConfig appConfig = ( !allAppConfig.isEmpty() ) ? allAppConfig.get(0) : new AppConfig(appVersionArg);

        final String currentVersion = appConfig.getAppCurrentVersion();

        // Save if new install or version has changed
        if (!appVersionArg.equals(currentVersion)) {

            appConfig.setAppCurrentVersion(appVersionArg);
            appConfigDAO.save(appConfig);

            dataMigrationService.applyVersionChanges(currentVersion, appVersionArg);
        }

    }

}
