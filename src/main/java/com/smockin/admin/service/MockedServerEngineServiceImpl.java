package com.smockin.admin.service;

import com.smockin.admin.dto.LiveLoggingBlockingEndpointDTO;
import com.smockin.admin.exception.AuthException;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.ProxyForwardMappingDAO;
import com.smockin.admin.persistence.dao.RestfulMockDAO;
import com.smockin.admin.persistence.dao.ServerConfigDAO;
import com.smockin.admin.persistence.entity.ProxyForwardMapping;
import com.smockin.admin.persistence.entity.ServerConfig;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.admin.persistence.enums.ServerTypeEnum;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import com.smockin.mockserver.dto.MockServerState;
import com.smockin.mockserver.dto.MockedServerConfigDTO;
import com.smockin.mockserver.dto.ProxyForwardConfigDTO;
import com.smockin.mockserver.dto.ProxyForwardMappingDTO;
import com.smockin.mockserver.engine.MockedRestServerEngine;
import com.smockin.mockserver.exception.MockServerException;
import com.smockin.utils.GeneralUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

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

    @Autowired
    private SmockinUserService smockinUserService;

    @Autowired
    private UserTokenServiceUtils userTokenServiceUtils;

    @Autowired
    private ProxyForwardMappingDAO proxyForwardMappingDAO;


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
            final ProxyForwardConfigDTO proxyConfig = loadProxyForwardConfig(ServerTypeEnum.RESTFUL);

            mockedRestServerEngine.start(configDTO, proxyConfig);

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

    @Override
    public ProxyForwardConfigDTO loadProxyForwardConfig(final ServerTypeEnum type) {

        final ServerConfig serverConfig = serverConfigDAO.findByServerType(type);

        if (serverConfig == null) {
            throw new RecordNotFoundException();
        }

        final ProxyForwardConfigDTO dto = new ProxyForwardConfigDTO();

        dto.setProxyMode(serverConfig.isProxyMode());
        dto.setProxyModeType(serverConfig.getProxyModeType());
        dto.setDoNotForwardWhen404Mock(serverConfig.isDoNotForwardWhen404Mock());

        dto.setProxyForwardMappings(proxyForwardMappingDAO.findAll()
            .stream()
            .map(m -> new ProxyForwardMappingDTO(m.getPath(), m.getProxyForwardUrl(), m.isDisabled()))
            .collect(Collectors.toList()));

        return dto;
    }

    @Override
    public void saveProxyForwardMappings(
            final ServerTypeEnum serverType,
            final ProxyForwardConfigDTO proxyForwardConfigDTO,
            final String token)
                throws AuthException, ValidationException, RecordNotFoundException {

        smockinUserService.assertCurrentUserIsAdmin(userTokenServiceUtils.loadCurrentUser(token));

        if (proxyForwardConfigDTO.isProxyMode()) {

            //
            // Validation
            if (proxyForwardConfigDTO.getProxyForwardMappings().isEmpty()) {
                throw new ValidationException("No proxy mappings have been defined");
            }

            for (ProxyForwardMappingDTO dto : proxyForwardConfigDTO.getProxyForwardMappings()) {

                if (StringUtils.isBlank(dto.getPath())) {
                    throw new ValidationException("A 'Path' value is missing");
                }

                if (StringUtils.isBlank(dto.getProxyForwardUrl())) {
                    throw new ValidationException("A 'Proxy Forward Url' value is missing");
                }

                if (!dto.getProxyForwardUrl().startsWith(HttpClientService.HTTPS_PROTOCOL)
                        && !dto.getProxyForwardUrl().startsWith(HttpClientService.HTTP_PROTOCOL)) {
                    throw new ValidationException("The 'Proxy Forward Url' value '" + dto.getProxyForwardUrl() + "' is invalid");
                }

            }

        }

        //
        // Save proxy related server config
        final ServerConfig serverConfig = serverConfigDAO.findByServerType(serverType);

        if (serverConfig == null) {
            throw new RecordNotFoundException();
        }

        serverConfig.setProxyMode(proxyForwardConfigDTO.isProxyMode());

        // Only update if proxy mode is enabled so as to preserve previous values
        if (proxyForwardConfigDTO.isProxyMode()) {
            serverConfig.setProxyModeType(proxyForwardConfigDTO.getProxyModeType());
            serverConfig.setDoNotForwardWhen404Mock(proxyForwardConfigDTO.isDoNotForwardWhen404Mock());
        }

        serverConfigDAO.save(serverConfig);


        // Only update if proxy mode is enabled so as to preserve previous values
        if (proxyForwardConfigDTO.isProxyMode()) {

            // Delete all existing mappings
            proxyForwardMappingDAO.deleteAll();
            proxyForwardMappingDAO.flush();

            // Save latest mappings
            proxyForwardMappingDAO.saveAll(
                proxyForwardConfigDTO.getProxyForwardMappings()
                    .stream()
                    .map(dto -> {

                        final ProxyForwardMapping proxyForwardMapping = new ProxyForwardMapping();
                        proxyForwardMapping.setPath(
                                (!StringUtils.startsWith(dto.getPath(), "/")
                                    && !StringUtils.equals(dto.getPath(), GeneralUtils.PATH_WILDCARD)) ? "/" : ""
                                        + dto.getPath());
                        proxyForwardMapping.setProxyForwardUrl(dto.getProxyForwardUrl());
                        proxyForwardMapping.setDisabled(dto.isDisabled());

                        return proxyForwardMapping;

                    }).collect(Collectors.toList())
            );

        }

    }

    @Override
    public void addLiveLoggingPathToBlock(final LiveLoggingBlockingEndpointDTO liveLoggingBlockingEndpoint,
                                          final String token) throws AuthException {

        mockedRestServerEngine.addPathToLiveBlocking(liveLoggingBlockingEndpoint.getMethod(), liveLoggingBlockingEndpoint.getPath());
    }

    @Override
    public void removeLiveLoggingPathToBlock(final String method,
                                             final String path,
                                             final String token) throws AuthException {

        mockedRestServerEngine.removePathFromLiveBlocking(RestMethodEnum.findByName(method), path);
    }

    void autoStartManager(final ServerTypeEnum serverType) throws MockServerException {

        if (serverType == null) {
            return;
        }

        switch (serverType) {
            case RESTFUL:
                startRest();
                break;
            default:
                logger.warn("Found auto start instruction for discontinued server type: " + serverType);
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
