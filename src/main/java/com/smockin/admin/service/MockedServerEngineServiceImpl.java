package com.smockin.admin.service;

import com.smockin.admin.enums.UserModeEnum;
import com.smockin.admin.exception.AuthException;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.FtpMockDAO;
import com.smockin.admin.persistence.dao.JmsMockDAO;
import com.smockin.admin.persistence.dao.RestfulMockDAO;
import com.smockin.admin.persistence.dao.ServerConfigDAO;
import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.persistence.entity.ServerConfig;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.persistence.enums.ServerTypeEnum;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import com.smockin.mockserver.dto.MockServerState;
import com.smockin.mockserver.dto.MockedServerConfigDTO;
import com.smockin.mockserver.engine.MockedFtpServerEngine;
import com.smockin.mockserver.engine.MockedJmsServerEngine;
import com.smockin.mockserver.engine.MockedRestServerEngine;
import com.smockin.mockserver.exception.MockServerException;
import com.smockin.utils.GeneralUtils;
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
    private MockedJmsServerEngine mockedJmsServerEngine;

    @Autowired
    private MockedFtpServerEngine mockedFtpServerEngine;

    @Autowired
    private RestfulMockDAO restfulMockDefinitionDAO;

    @Autowired
    private JmsMockDAO jmsQueueMockDAO;

    @Autowired
    private FtpMockDAO ftpMockDAO;

    @Autowired
    private ServerConfigDAO serverConfigDAO;

    @Autowired
    private SmockinUserService smockinUserService;

    @Autowired
    private UserTokenServiceUtils userTokenServiceUtils;


    //
    // Rest
    @Override
    public MockedServerConfigDTO startRest(final String token) throws MockServerException, RecordNotFoundException, AuthException {

        smockinUserService.assertCurrentUserIsAdmin(userTokenServiceUtils.loadCurrentUser(token));

        return startRest();
    }

    private MockedServerConfigDTO startRest() throws MockServerException {

        try {

            final MockedServerConfigDTO configDTO = loadServerConfig(ServerTypeEnum.RESTFUL);

            checkForUnresolvedProxyUserPathMatchConflicts(configDTO);

            final List<RestfulMock> activeMocks =
                    (UserModeEnum.INACTIVE.equals(smockinUserService.getUserMode()))
                        ? restfulMockDefinitionDAO.findAllByStatusAndUser(RecordStatusEnum.ACTIVE, smockinUserService.loadDefaultUser().get().getId())
                        : restfulMockDefinitionDAO.findAllByStatus(RecordStatusEnum.ACTIVE);

            mockedRestServerEngine.start(configDTO, activeMocks);

            return configDTO;
        } catch (IllegalArgumentException ex) {
            mockedRestServerEngine.shutdown();
            throw ex;
        } catch (RecordNotFoundException ex) {
            logger.error("Starting REST Mocking Engine, due to missing mock server config", ex);
            throw new MockServerException("Missing mock REST server config");
        } catch (MockServerException ex) {
            logger.error("Starting REST Mocking Engine", ex);
            throw ex;
        }

    }

    @Override
    public MockedServerConfigDTO restartRest(final String token) throws MockServerException, RecordNotFoundException, AuthException {

        smockinUserService.assertCurrentUserIsAdmin(userTokenServiceUtils.loadCurrentUser(token));

        if (getRestServerState().isRunning()) {
            shutdownRest();
        }

        return startRest();
    }

    @Override
    public MockServerState getRestServerState() throws MockServerException {
        return mockedRestServerEngine.getCurrentState();
    }

    @Override
    public void shutdownRest(final String token) throws MockServerException, RecordNotFoundException, AuthException {

        smockinUserService.assertCurrentUserIsAdmin(userTokenServiceUtils.loadCurrentUser(token));

        shutdownRest();
    }

    private void shutdownRest() throws MockServerException {

        try {
            mockedRestServerEngine.shutdown();
        } catch (MockServerException ex) {
            logger.error("Stopping REST Mocking Engine", ex);
            throw ex;
        }

    }


    //
    // JMS
    @Override
    public MockedServerConfigDTO startJms(final String token) throws MockServerException, RecordNotFoundException, AuthException {

        smockinUserService.assertCurrentUserIsAdmin(userTokenServiceUtils.loadCurrentUser(token));

        return startJms();
    }

    private MockedServerConfigDTO startJms() throws MockServerException {

        try {

            final MockedServerConfigDTO dto = loadServerConfig(ServerTypeEnum.JMS);

            if (getJmsServerState().isRunning()) {
                logger.warn("Cannot start JMS server as it is already running");
                return dto;
            }

            mockedJmsServerEngine.start(dto, jmsQueueMockDAO.findAllByStatus(RecordStatusEnum.ACTIVE));

            return dto;
        } catch (RecordNotFoundException ex) {
            logger.error("Starting JMS Mocking Engine, due to missing mock server config", ex);
            throw new MockServerException("Missing mock JMS server config");
        } catch (MockServerException ex) {
            logger.error("Starting JMS Mocking Engine", ex);
            throw ex;
        }

    }

    @Override
    public void shutdownJms(final String token) throws MockServerException, RecordNotFoundException, AuthException {

        smockinUserService.assertCurrentUserIsAdmin(userTokenServiceUtils.loadCurrentUser(token));

        shutdownJms();
    }

    private void shutdownJms() throws MockServerException {

        try {
            mockedJmsServerEngine.shutdown();
        } catch (MockServerException ex) {
            logger.error("Stopping JMS Mocking Engine", ex);
            throw ex;
        }

    }

    @Override
    public MockedServerConfigDTO restartJms(final String token) throws MockServerException, RecordNotFoundException, AuthException {

        smockinUserService.assertCurrentUserIsAdmin(userTokenServiceUtils.loadCurrentUser(token));

        if (getJmsServerState().isRunning()) {
            shutdownJms();
        }

        return startJms();
    }

    @Override
    public MockServerState getJmsServerState() throws MockServerException {
        return mockedJmsServerEngine.getCurrentState();
    }


    //
    // FTP
    @Override
    public MockedServerConfigDTO startFtp(final String token) throws MockServerException, RecordNotFoundException, AuthException {

        smockinUserService.assertCurrentUserIsAdmin(userTokenServiceUtils.loadCurrentUser(token));

        return startFtp();
    }

    private MockedServerConfigDTO startFtp() throws MockServerException {

        try {

            final MockedServerConfigDTO dto = loadServerConfig(ServerTypeEnum.FTP);

            if (getFtpServerState().isRunning()) {
                logger.warn("Cannot start FTP server as it is already running");
                return dto;
            }

            mockedFtpServerEngine.start(dto, ftpMockDAO.findAllByStatus(RecordStatusEnum.ACTIVE));

            return dto;
        } catch (RecordNotFoundException ex) {
            logger.error("Starting FTP Mocking Engine, due to missing mock server config", ex);
            throw new MockServerException("Missing mock FTP server config");
        } catch (MockServerException ex) {
            logger.error("Starting FTP Mocking Engine", ex);
            throw ex;
        }

    }

    @Override
    public void shutdownFtp(final String token) throws MockServerException, RecordNotFoundException, AuthException {

        smockinUserService.assertCurrentUserIsAdmin(userTokenServiceUtils.loadCurrentUser(token));

        shutdownFtp();
    }

    private void shutdownFtp() throws MockServerException {

        try {
            mockedFtpServerEngine.shutdown();
        } catch (MockServerException ex) {
            logger.error("Stopping FTP Mocking Engine", ex);
            throw ex;
        }

    }

    @Override
    public MockedServerConfigDTO restartFtp(final String token) throws MockServerException, RecordNotFoundException, AuthException {

        smockinUserService.assertCurrentUserIsAdmin(userTokenServiceUtils.loadCurrentUser(token));

        if (getFtpServerState().isRunning()) {
            shutdownFtp();
        }

        return startFtp();

    }

    @Override
    public MockServerState getFtpServerState() throws MockServerException {
        return mockedFtpServerEngine.getCurrentState();
    }


    //
    // Config
    @Override
    public MockedServerConfigDTO loadServerConfig(final ServerTypeEnum serverType) throws RecordNotFoundException {

        final ServerConfig serverConfig = serverConfigDAO.findByServerType(serverType);

        if (serverConfig == null) {
            throw new RecordNotFoundException();
        }

        return new MockedServerConfigDTO(
                serverConfig.getServerType(),
                serverConfig.getPort(),
                serverConfig.getMaxThreads(),
                serverConfig.getMinThreads(),
                serverConfig.getTimeOutMillis(),
                serverConfig.isAutoStart(),
                serverConfig.isAutoRefresh(),
                serverConfig.getNativeProperties()
        );

    }

    @Override
    public void saveServerConfig(final ServerTypeEnum serverType, final MockedServerConfigDTO config, final String token)
            throws RecordNotFoundException, AuthException, ValidationException {

        smockinUserService.assertCurrentUserIsAdmin(userTokenServiceUtils.loadCurrentUser(token));

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

        serverConfig.getNativeProperties().clear();
        serverConfig.getNativeProperties().putAll(config.getNativeProperties());

        serverConfigDAO.saveAndFlush(serverConfig);
    }

    @Override
    public void handleServerAutoStart() {

        serverConfigDAO.findAll().stream().forEach(sc -> {
            if (sc.isAutoStart()) {
                try {
                    autoStartManager(sc.getServerType());
                } catch (MockServerException ex) {
                    logger.error("Error auto starting server type : " + sc.getServerType(), ex);
                }
            }
        });

    }

    void autoStartManager(final ServerTypeEnum serverType) throws MockServerException {

        if (serverType == null) {
            return;
        }

        switch (serverType) {
            case RESTFUL:
                startRest();
                break;
            case JMS:
                startJms();
                break;
            case FTP:
                startFtp();
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

    void checkForUnresolvedProxyUserPathMatchConflicts(final MockedServerConfigDTO config) {

        if (!mockedRestServerEngine.isProxyServerModeEnabled(config)
                || UserModeEnum.INACTIVE.equals(smockinUserService.getUserMode())) {
            return;
        }

        restfulMockDefinitionDAO.findAllActivePathDuplicates()
                .entrySet()
                .stream()
                .forEach(d -> {

                    if (!d.getValue()
                            .stream()
                            .anyMatch(RestfulMock::isProxyPriority)) {
                        throw new IllegalArgumentException(GeneralUtils.PROXY_PATH_CONFLICT);
                    }
                });

    }

}
