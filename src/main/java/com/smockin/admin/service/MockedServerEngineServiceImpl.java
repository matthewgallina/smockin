package com.smockin.admin.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.smockin.admin.enums.UserModeEnum;
import com.smockin.admin.exception.*;
import com.smockin.admin.persistence.dao.ProxyForwardMappingDAO;
import com.smockin.admin.persistence.dao.RestfulMockDAO;
import com.smockin.admin.persistence.dao.ServerConfigDAO;
import com.smockin.admin.persistence.entity.ProxyForwardMapping;
import com.smockin.admin.persistence.entity.ServerConfig;
import com.smockin.admin.persistence.entity.SmockinUser;
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
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
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

        if (proxyForwardConfigDTO.isProxyMode()
                && !proxyForwardConfigDTO.getProxyForwardMappings().isEmpty()) {

            //
            // Validation
            validateProxyMappings(proxyForwardConfigDTO.getProxyForwardMappings());

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
            saveProxyMappings(proxyForwardConfigDTO.getProxyForwardMappings());

        }

    }

    @Override
    public void addLiveLoggingPathToBlock(final RestMethodEnum method,
                                          final String path,
                                          final String token) {

        mockedRestServerEngine.addPathToLiveBlocking(method, amendMultiUserCtxPath(path, token));
    }

    @Override
    public void removeLiveLoggingPathToBlock(final RestMethodEnum method,
                                             final String path,
                                             final String token) {

        mockedRestServerEngine.removePathFromLiveBlocking(method, amendMultiUserCtxPath(path, token));
    }

    String amendMultiUserCtxPath(final String path, final String token) {

        if (UserModeEnum.ACTIVE.equals(smockinUserService.getUserMode())) {

            final SmockinUser user = userTokenServiceUtils.loadCurrentUser(token);
            final String userCtxPath = GeneralUtils.URL_PATH_SEPARATOR + user.getCtxPath();

            // Check if path already contains userCtxPath
            return (StringUtils.startsWith(path, userCtxPath))
                    ? path
                    : userCtxPath + path;
        }

        return path;
    }

    @Override
    public Optional<String> exportProxyMappings(final String token)
            throws AuthException {

        smockinUserService.assertCurrentUserIsAdmin(userTokenServiceUtils.loadCurrentUser(token));

        final List<ProxyForwardMappingDTO> dtos = proxyForwardMappingDAO.findAll()
                .stream()
                .map(m ->
                        new ProxyForwardMappingDTO(m.getPath(), m.getProxyForwardUrl(), m.isDisabled()))
                .collect(Collectors.toList());

        if (dtos.isEmpty()) {
            return Optional.empty();
        }

        final String exportContent =
            GeneralUtils.serialiseJson(dtos);

        final byte[] exportBytes = exportContent.getBytes();

        return Optional.of(Base64.getEncoder().encodeToString(exportBytes));
    }

    @Override
    public String importProxyMappingsFile(final MultipartFile file, final boolean keepExisting, final String token)
            throws MockImportException, AuthException, ValidationException {

        smockinUserService.assertCurrentUserIsAdmin(userTokenServiceUtils.loadCurrentUser(token));

        try {

            final ByteArrayInputStream stream = new ByteArrayInputStream(file.getBytes());
            final String content = IOUtils.toString(stream, Charset.defaultCharset().displayName());
            List<ProxyForwardMappingDTO> proxyForwardMappingDTOs = GeneralUtils.deserialiseJson(content, new TypeReference<List<ProxyForwardMappingDTO>>() {});

            if (proxyForwardMappingDTOs == null) {
                throw new ValidationException("Error reading import file: invalid json structure");
            }

            validateProxyMappings(proxyForwardMappingDTOs);

            if (!keepExisting) {

                // Delete all existing mappings
                proxyForwardMappingDAO.deleteAll();
                proxyForwardMappingDAO.flush();

            } else {

                final List<String> paths = proxyForwardMappingDAO.findAll()
                        .stream()
                        .map(p ->
                                p.getPath().toLowerCase())
                        .collect(Collectors.toList());

                // Remove entries from import that duplicate existing mappings
                proxyForwardMappingDTOs = proxyForwardMappingDTOs
                        .stream()
                        .filter(p ->
                            !paths.contains(p.getPath().toLowerCase()))
                        .collect(Collectors.toList());
            }

            // Save latest mappings
            saveProxyMappings(proxyForwardMappingDTOs);

        } catch (IOException ex) {
            throw new MockExportException("Error importing proxy mappings file");
        }

        return null;
    }

    void saveProxyMappings(final List<ProxyForwardMappingDTO> proxyForwardMappings) {

        if (proxyForwardMappings.isEmpty()) {
            return;
        }

        proxyForwardMappingDAO.saveAll(
            proxyForwardMappings
            .stream()
            .map(dto -> {

                final ProxyForwardMapping proxyForwardMapping = new ProxyForwardMapping();
                proxyForwardMapping.setPath(
                        (!StringUtils.startsWith(dto.getPath(), GeneralUtils.URL_PATH_SEPARATOR)
                                && !StringUtils.equals(dto.getPath(), GeneralUtils.PATH_WILDCARD)) ? GeneralUtils.URL_PATH_SEPARATOR : ""
                                + dto.getPath());
                proxyForwardMapping.setProxyForwardUrl(dto.getProxyForwardUrl());
                proxyForwardMapping.setDisabled(dto.isDisabled());

                return proxyForwardMapping;

            }).collect(Collectors.toList())
        );

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

    void validateProxyMappings(final List<ProxyForwardMappingDTO> proxyForwardMappings)
            throws ValidationException {

        for (ProxyForwardMappingDTO dto : proxyForwardMappings) {

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

}
