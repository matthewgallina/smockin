package com.smockin.admin.persistence;

import com.smockin.admin.persistence.dao.ServerConfigDAO;
import com.smockin.admin.persistence.entity.ServerConfig;
import com.smockin.admin.persistence.enums.ServerTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by mgallina.
 */
@Component
public class CoreDataSet {

    private final Logger logger = LoggerFactory.getLogger(CoreDataSet.class);

    @Autowired
    private ServerConfigDAO serverConfigDAO;

    @Transactional
    public void exec() {

        applyServerConfigDefaults();

    }

    void applyServerConfigDefaults() {

        // Check if Server Config DB Defaults have already been installed
        if (!serverConfigDAO.findAll().isEmpty()) {
            return;
        }

        logger.debug("Server Config DB Defaults being executed...");

        ServerConfig restServerConfig = new ServerConfig();
        restServerConfig.setServerType(ServerTypeEnum.RESTFUL);
        restServerConfig.setPort(8001);
        restServerConfig.setMaxThreads(100);
        restServerConfig.setMinThreads(10);
        restServerConfig.setTimeOutMillis(30000);
        restServerConfig.setAutoStart(false);
        restServerConfig.setAutoRefresh(false);

        serverConfigDAO.save(restServerConfig);

        logger.debug("Server Config DB Defaults    DONE");
    }

}
