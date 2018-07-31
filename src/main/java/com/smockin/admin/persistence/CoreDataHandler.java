package com.smockin.admin.persistence;

import com.smockin.admin.persistence.dao.AppConfigDAO;
import com.smockin.admin.persistence.dao.ServerConfigDAO;
import com.smockin.admin.persistence.dao.SmockinUserDAO;
import com.smockin.admin.persistence.entity.AppConfig;
import com.smockin.admin.persistence.entity.ServerConfig;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.persistence.enums.ServerTypeEnum;
import com.smockin.admin.persistence.enums.SmockinUserRoleEnum;
import com.smockin.admin.persistence.migration.DataMigrationService;
import com.smockin.admin.service.EncryptionService;
import com.smockin.utils.GeneralUtils;
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

    @Autowired
    private SmockinUserDAO smockinUserDAO;

    @Autowired
    private EncryptionService authService;

    @Transactional
    public void exec() {

        applyServerConfigDefaults();

        applyAppVersioning();

        applyCoreAdminUser();

    }

    void applyServerConfigDefaults() {

        // Check if Server Config DB Defaults have already been installed

        if (serverConfigDAO.findByServerType(ServerTypeEnum.RESTFUL) == null) {

            logger.info("Installing REST Server Config DB Defaults...");

            final ServerConfig restServerConfig = new ServerConfig();
            restServerConfig.setServerType(ServerTypeEnum.RESTFUL);
            restServerConfig.setPort(8001);
            restServerConfig.setMaxThreads(100);
            restServerConfig.setMinThreads(10);
            restServerConfig.setTimeOutMillis(30000);
            restServerConfig.setAutoStart(false);
            restServerConfig.setAutoRefresh(false);
            restServerConfig.getNativeProperties().put("ENABLE_CORS", "false");

            serverConfigDAO.save(restServerConfig);
        }

        if (serverConfigDAO.findByServerType(ServerTypeEnum.JMS) == null) {

            logger.info("Installing JMS Server Config DB Defaults...");

            final ServerConfig jmsServerConfig = new ServerConfig();
            jmsServerConfig.setServerType(ServerTypeEnum.JMS);
            jmsServerConfig.setPort(8002); // 61616
            jmsServerConfig.setMaxThreads(10);
            jmsServerConfig.setMinThreads(0);
            jmsServerConfig.setTimeOutMillis(0);
            jmsServerConfig.setAutoStart(false);
            jmsServerConfig.setAutoRefresh(false);
            jmsServerConfig.getNativeProperties().put("BROKER_URL", "tcp://localhost:");

            serverConfigDAO.save(jmsServerConfig);

        }

        if (serverConfigDAO.findByServerType(ServerTypeEnum.FTP) == null) {

            logger.info("Installing FTP Server Config DB Defaults...");

            final ServerConfig ftpServerConfig = new ServerConfig();
            ftpServerConfig.setServerType(ServerTypeEnum.FTP);
            ftpServerConfig.setPort(8003);
            ftpServerConfig.setMaxThreads(0);
            ftpServerConfig.setMinThreads(0);
            ftpServerConfig.setTimeOutMillis(0);
            ftpServerConfig.setAutoStart(false);
            ftpServerConfig.setAutoRefresh(false);

            serverConfigDAO.save(ftpServerConfig);
        }

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

    void applyCoreAdminUser() {

        if (!smockinUserDAO.findAllByRole(SmockinUserRoleEnum.SYS_ADMIN).isEmpty()) {
            return;
        }

        smockinUserDAO.save(new SmockinUser(
                "admin",
                authService.encrypt("admin"),
                "System Admin",
                "",
                SmockinUserRoleEnum.SYS_ADMIN,
                RecordStatusEnum.ACTIVE,
                GeneralUtils.generateUUID()));

    }

}
