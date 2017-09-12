package com.smockin.admin.service;

import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.RestfulMockDAO;
import com.smockin.admin.persistence.dao.ServerConfigDAO;
import com.smockin.admin.persistence.entity.ServerConfig;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.persistence.enums.ServerTypeEnum;
import com.smockin.mockserver.dto.MockServerState;
import com.smockin.mockserver.dto.MockedServerConfigDTO;
import com.smockin.mockserver.engine.MockedRestServerEngine;
import com.smockin.mockserver.exception.MockServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Created by mgallina.
 */
@Service
@Transactional
public class MockedServerEngineServiceImpl implements MockedServerEngineService {

    private final Logger logger = LoggerFactory.getLogger(MockedServerEngineServiceImpl.class);

    @Autowired
    private MockedRestServerEngine mockedRestServerEngine;

    @Autowired
    private RestfulMockDAO restfulMockDefinitionDAO;

    @Autowired
    private ServerConfigDAO serverConfigDAO;


    public MockedServerConfigDTO startRest() throws MockServerException {

        try {
            final MockedServerConfigDTO dto = loadServerConfig(ServerTypeEnum.RESTFUL);
            mockedRestServerEngine.start(dto, restfulMockDefinitionDAO.findAllByStatus(RecordStatusEnum.ACTIVE));
            return dto;
        } catch (RecordNotFoundException ex) {
            logger.error("Starting REST Mocking Engine, due to missing mock server config", ex);
            throw new MockServerException("Missing mock server config");
        } catch (MockServerException ex) {
            logger.error("Starting REST Mocking Engine", ex);
            throw ex;
        }

    }

    public MockedServerConfigDTO restartRest() throws MockServerException {

        if (getRestServerState().isRunning()) {
            shutdownRest();
        }

        return startRest();
    }

    public MockServerState getRestServerState() throws MockServerException {
        return mockedRestServerEngine.getCurrentState();
    }

    public void shutdownRest() throws MockServerException {

        try {
            mockedRestServerEngine.shutdown();
        } catch (MockServerException ex) {
            logger.error("Stopping REST Mocking Engine", ex);
            throw ex;
        }

    }

    public MockedServerConfigDTO loadServerConfig(final ServerTypeEnum serverType) throws RecordNotFoundException {

        final ServerConfig serverConfig = serverConfigDAO.findByServerType(serverType);

        if (serverConfig == null) {
            throw new RecordNotFoundException();
        }

        return new MockedServerConfigDTO(
                serverConfig.getPort(),
                serverConfig.getMaxThreads(),
                serverConfig.getMinThreads(),
                serverConfig.getTimeOutMillis(),
                serverConfig.isAutoStart(),
                serverConfig.isAutoRefresh(),
                serverConfig.isEnableCors(),
                serverConfig.getNativeProperties()
        );

    }

    public void saveServerConfig(final ServerTypeEnum serverType, final MockedServerConfigDTO config) throws ValidationException {

        validateServerConfig(config);

        ServerConfig serverConfig = serverConfigDAO.findByServerType(serverType);

        if (serverConfig == null) {
            serverConfig = new ServerConfig(serverType);
        }

        serverConfig.setPort(config.getPort());
        serverConfig.setMaxThreads(config.getMaxThreads());
        serverConfig.setMinThreads(config.getMinThreads());
        serverConfig.setTimeOutMillis(config.getTimeOutMillis());
        serverConfig.setAutoStart(config.isAutoStart());
        serverConfig.setAutoRefresh(config.isAutoRefresh());
        serverConfig.setEnableCors(config.isEnableCors());

        serverConfig.getNativeProperties().clear();
        serverConfig.getNativeProperties().putAll(config.getNativeProperties());

        serverConfigDAO.saveAndFlush(serverConfig);
    }

    public void handleServerAutoStart() {

        final List<ServerConfig> configList = serverConfigDAO.findAll();

        for (ServerConfig sc : configList) {
            if (sc.isAutoStart()) {
                try {
                    autoStartManager(sc.getServerType());
                } catch (MockServerException ex) {
                    logger.error("Error auto starting server type : " + sc.getServerType(), ex);
                }
            }
        }

    }

    void autoStartManager(final ServerTypeEnum serverType) throws MockServerException {

        if (serverType == null) {
            return;
        }

        switch (serverType) {
            case RESTFUL:
                startRest();
                break;
        }

    }

    void validateServerConfig(final MockedServerConfigDTO dto) throws ValidationException {

        if (dto == null) {
            throw new ValidationException("config is required");
        }
        if (dto.getPort() == null) {
            throw new ValidationException("'port' config value is required");
        }
        if (dto.getMaxThreads() == null) {
            throw new ValidationException("'maxThreads' config value is required");
        }
        if (dto.getMinThreads() == null) {
            throw new ValidationException("'minThreads' config value is required");
        }
        if (dto.getTimeOutMillis() == null) {
            throw new ValidationException("'timeOutMillis' config value is required");
        }

    }

}
