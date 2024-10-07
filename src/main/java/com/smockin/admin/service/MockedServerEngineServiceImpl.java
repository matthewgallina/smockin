package com.smockin.admin.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.smockin.admin.enums.StoreTypeEnum;
import com.smockin.admin.enums.UserModeEnum;
import com.smockin.admin.exception.*;
import com.smockin.admin.persistence.dao.*;
import com.smockin.admin.persistence.entity.*;
import com.smockin.admin.persistence.enums.ProxyModeTypeEnum;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.admin.persistence.enums.ServerTypeEnum;
import com.smockin.admin.persistence.enums.SmockinUserRoleEnum;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import com.smockin.mockserver.dto.*;
import com.smockin.mockserver.engine.*;
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
import java.util.Arrays;
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
    private MockedRestServerEngineUtils mockedRestServerEngineUtils;

    @Autowired
    private RestfulMockDAO restfulMockDefinitionDAO;

    @Autowired
    private MockedS3ServerEngine mockedS3ServerEngine;

    @Autowired
    private ServerConfigDAO serverConfigDAO;

    @Autowired
    private SmockinUserService smockinUserService;

    @Autowired
    private UserTokenServiceUtils userTokenServiceUtils;

    @Autowired
    private ProxyForwardUserConfigDAO proxyForwardUserConfigDAO;

    @Autowired
    private ProxyMappingCache proxyMappingCache;

    @Autowired
    private S3MockDAO s3MockDAO;

    @Autowired
    private MockedMailServerEngine mockedMailServerEngine;

    @Autowired
    private MailMockDAO mailMockDAO;

    @Autowired
    private MailMockMessageDAO mailMockMessageDAO;


    //
    // Rest
    @Override
    public MockedServerConfigDTO startRest(final String token) throws MockServerException, RecordNotFoundException, AuthException {

        smockinUserService.assertCurrentUserIsAdmin(userTokenServiceUtils.loadCurrentActiveUser(token));

        return startRest();
    }

    private MockedServerConfigDTO startRest() throws MockServerException {

        try {

            final MockedServerConfigDTO configDTO = loadServerConfig(ServerTypeEnum.RESTFUL);
            final List<ProxyForwardConfigCacheDTO> allProxyForwardConfig = loadAllUserProxyForwardMappings();

            mockedRestServerEngine.start(configDTO, allProxyForwardConfig);

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

        smockinUserService.assertCurrentUserIsAdmin(userTokenServiceUtils.loadCurrentActiveUser(token));

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

        smockinUserService.assertCurrentUserIsAdmin(userTokenServiceUtils.loadCurrentActiveUser(token));

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
    // S3
    @Override
    public MockedServerConfigDTO startS3(final String token) throws MockServerException, RecordNotFoundException, AuthException {

        smockinUserService.assertCurrentUserIsAdmin(userTokenServiceUtils.loadCurrentActiveUser(token));

        return startS3();
    }

    private MockedServerConfigDTO startS3() throws MockServerException {

        try {

            final MockedServerConfigDTO configDTO = loadServerConfig(ServerTypeEnum.S3);

            final List<S3Mock> activeMocks = s3MockDAO.findAllActiveBuckets();

            mockedS3ServerEngine.start(configDTO, activeMocks);

            return configDTO;
        } catch (IllegalArgumentException ex) {
            logger.error("Starting S3 Mocking Engine", ex);
            mockedS3ServerEngine.shutdown();
            throw ex;
        } catch (RecordNotFoundException ex) {
            logger.error("Starting S3 Mocking Engine, due to missing mock server config", ex);
            throw new MockServerException("Missing mock S3 server config");
        } catch (MockServerException ex) {
            logger.error("Starting S3 Mocking Engine", ex);
            throw ex;
        }

    }

    @Override
    public MockedServerConfigDTO restartS3(final String token) throws MockServerException, RecordNotFoundException, AuthException {

        smockinUserService.assertCurrentUserIsAdmin(userTokenServiceUtils.loadCurrentActiveUser(token));

        if (getS3ServerState().isRunning()) {
            shutdownS3();
        }

        return startS3();
    }

    @Override
    public MockServerState getS3ServerState() throws MockServerException {
        return mockedS3ServerEngine.getCurrentState();
    }

    @Override
    public void shutdownS3(final String token) throws MockServerException, RecordNotFoundException, AuthException {

        smockinUserService.assertCurrentUserIsAdmin(userTokenServiceUtils.loadCurrentActiveUser(token));

        shutdownS3();
    }

    private void shutdownS3() throws MockServerException {

        try {
            mockedS3ServerEngine.shutdown();
        } catch (MockServerException ex) {
            logger.error("Stopping S3 Mocking Engine", ex);
            throw ex;
        }

    }


    //
    // Mail
    @Override
    public MockedServerConfigDTO startMail(final String token) throws MockServerException, RecordNotFoundException, AuthException {

        smockinUserService.assertCurrentUserIsAdmin(userTokenServiceUtils.loadCurrentActiveUser(token));

        return startMail();
    }

    private MockedServerConfigDTO startMail() throws MockServerException {

        try {

            final MockedServerConfigDTO configDTO = loadServerConfig(ServerTypeEnum.MAIL);

            mockedMailServerEngine.start(configDTO, mailMockDAO.findAllActive());

            return configDTO;
        } catch (IllegalArgumentException ex) {
            logger.error("Starting Mail Mocking Engine", ex);
            mockedMailServerEngine.shutdown();
            throw ex;
        } catch (RecordNotFoundException ex) {
            logger.error("Starting Mail Mocking Engine, due to missing mock server config", ex);
            throw new MockServerException("Missing mock Mail server config");
        } catch (MockServerException ex) {
            logger.error("Starting Mail Mocking Engine", ex);
            throw ex;
        }

    }

    @Override
    public MockedServerConfigDTO restartMail(final String token) throws MockServerException, RecordNotFoundException, AuthException {

        smockinUserService.assertCurrentUserIsAdmin(userTokenServiceUtils.loadCurrentActiveUser(token));

        if (getS3ServerState().isRunning()) {
            shutdownMail();
        }

        return startMail();
    }

    @Override
    public MockServerState getMailServerState() throws MockServerException {
        return mockedMailServerEngine.getCurrentState();
    }

    @Override
    public void shutdownMail(final String token) throws MockServerException, RecordNotFoundException, AuthException {

        smockinUserService.assertCurrentUserIsAdmin(userTokenServiceUtils.loadCurrentActiveUser(token));

        shutdownMail();
    }

    private void shutdownMail() throws MockServerException {

        try {
            mockedMailServerEngine.shutdown();
        } catch (MockServerException ex) {
            logger.error("Stopping Mail Mocking Engine", ex);
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
                serverConfig.isProxyMode(),
                serverConfig.getNativeProperties()
        );

    }

    @Override
    public void saveServerConfig(final ServerTypeEnum serverType, final MockedServerConfigDTO config, final String token)
            throws RecordNotFoundException, AuthException, ValidationException {

        smockinUserService.assertCurrentUserIsAdmin(userTokenServiceUtils.loadCurrentActiveUser(token));

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

        serverConfigDAO.findAll()
                .stream()
                .forEach(sc -> {
                    if (sc.isAutoStart()) {
                        try {
                            autoStartManager(sc.getServerType());
                        } catch (MockServerException ex) {
                            logger.error("Error auto starting server type : " + sc.getServerType(), ex);
                        }
                    }
                });

    }

    boolean isProxyModeEnabled() {

        final ServerConfig serverConfig = serverConfigDAO.findByServerType(ServerTypeEnum.RESTFUL);

        if (serverConfig == null) {
            throw new RecordNotFoundException();
        }

        return serverConfig.isProxyMode();
    }

    @Override
    public void updateProxyMode(final boolean enableProxyMode, final String token) throws AuthException {

        // only admin is allowed to enable / disable this.
        smockinUserService.assertCurrentUserIsAdmin(userTokenServiceUtils.loadCurrentActiveUser(token));

        final ServerConfig serverConfig = serverConfigDAO.findByServerType(ServerTypeEnum.RESTFUL);

        if (serverConfig == null) {
            throw new RecordNotFoundException();
        }

        if (serverConfig.isProxyMode() == enableProxyMode) {
            // no change
            return;
        }

        serverConfig.setProxyMode(enableProxyMode);

        serverConfigDAO.save(serverConfig);

        // Update mock server
        mockedRestServerEngine.updateProxyMode(enableProxyMode);

        if (enableProxyMode) {
            // Update cache with all proxy mappings
            proxyMappingCache.init(loadAllUserProxyForwardMappings());
        }

    }

    List<ProxyForwardConfigCacheDTO> loadAllUserProxyForwardMappings() {

        return proxyForwardUserConfigDAO.findAll().stream()
                .map(pm ->
                        toProxyForwardConfigCacheDTO(pm)
                ).collect(Collectors.toList());
    }

    ProxyForwardConfigCacheDTO toProxyForwardConfigCacheDTO(final ProxyForwardUserConfig pm) {

        final ProxyForwardConfigCacheDTO dto = new ProxyForwardConfigCacheDTO(
                pm.getCreatedBy().getExtId(),
                pm.getCreatedBy().getCtxPath());

        dto.setProxyModeType(pm.getProxyModeType());
        dto.setDoNotForwardWhen404Mock(pm.isDoNotForwardWhen404Mock());
        dto.setProxyForwardMappings(
                pm.getProxyForwardMappings()
                        .stream()
                        .map(m ->
                                new ProxyForwardMappingDTO(
                                        m.getPath(),
                                        m.getProxyForwardUrl(),
                                        m.isDisabled()))
                        .collect(Collectors.toList()));

        return dto;
    }

    @Override
    public ProxyForwardConfigResponseDTO loadProxyForwardMappingsForUser(final String token) {

        final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);

        ProxyForwardUserConfig proxyForwardUserConfig
                = proxyForwardUserConfigDAO.findByUser(smockinUser.getId());

        // Create new record for user if this does not exist
        if (proxyForwardUserConfig == null) {
            proxyForwardUserConfig = buildNewProxyForwardUserConfig(smockinUser);
            proxyForwardUserConfig.setProxyModeType(ProxyModeTypeEnum.ACTIVE);
            proxyForwardUserConfig.setDoNotForwardWhen404Mock(false);

            saveUserProxyMappings(proxyForwardUserConfig, Arrays.asList());
        }

        return new ProxyForwardConfigResponseDTO(
                isProxyModeEnabled(),
                proxyForwardUserConfig.getProxyModeType(),
                proxyForwardUserConfig.isDoNotForwardWhen404Mock(),
                proxyForwardUserConfig.getProxyForwardMappings()
                        .stream()
                        .map(m ->
                                new ProxyForwardMappingDTO(
                                        m.getPath(),
                                        m.getProxyForwardUrl(),
                                        m.isDisabled()))
                        .collect(Collectors.toList()));
    }

    @Override
    public void saveProxyForwardMappingsForUser(
            final ProxyForwardConfigDTO proxyForwardConfigDTO,
            final String token)
            throws ValidationException, RecordNotFoundException {

        if (!proxyForwardConfigDTO.getProxyForwardMappings().isEmpty()) {

            //
            // Validation
            if (proxyForwardConfigDTO.getProxyModeType() == null) {
                throw new ValidationException("Proxy Mode Type is required");
            }

            validateProxyMappings(proxyForwardConfigDTO.getProxyForwardMappings());

        }

        final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);

        ProxyForwardUserConfig proxyForwardUserConfig
                = proxyForwardUserConfigDAO.findByUser(smockinUser.getId());

        if (proxyForwardUserConfig == null) {
            proxyForwardUserConfig = buildNewProxyForwardUserConfig(smockinUser);
        }

        proxyForwardUserConfig.setProxyModeType(proxyForwardConfigDTO.getProxyModeType());
        proxyForwardUserConfig.setDoNotForwardWhen404Mock(proxyForwardConfigDTO.isDoNotForwardWhen404Mock());

        // Save latest mappings
        saveUserProxyMappings(proxyForwardUserConfig, proxyForwardConfigDTO.getProxyForwardMappings());

        // Save mapping changes to the cache
        final ProxyForwardConfigCacheDTO cacheDTO = new ProxyForwardConfigCacheDTO(smockinUser.getExtId(), smockinUser.getCtxPath());
        cacheDTO.setProxyModeType(proxyForwardConfigDTO.getProxyModeType());
        cacheDTO.setDoNotForwardWhen404Mock(proxyForwardConfigDTO.isDoNotForwardWhen404Mock());
        cacheDTO.setProxyForwardMappings(proxyForwardConfigDTO.getProxyForwardMappings());

        if (isProxyModeEnabled()) {
            proxyMappingCache.update(cacheDTO);
        }

    }

    ProxyForwardUserConfig buildNewProxyForwardUserConfig(final SmockinUser smockinUser) {

        final ServerConfig serverConfig = serverConfigDAO.findByServerType(ServerTypeEnum.RESTFUL);

        if (serverConfig == null) {
            throw new RecordNotFoundException();
        }

        final ProxyForwardUserConfig proxyForwardUserConfig = new ProxyForwardUserConfig();
        proxyForwardUserConfig.setCreatedBy(smockinUser);
        proxyForwardUserConfig.setServerConfig(serverConfig);

        return proxyForwardUserConfig;
    }

    @Override
    public void addLiveLoggingPathToBlock(final RestMethodEnum method,
                                          final String path,
                                          final String token) throws ValidationException {

        final SmockinUser user = userTokenServiceUtils.loadCurrentActiveUser(token);
        mockedRestServerEngine.addPathToLiveBlocking(method, amendMultiUserCtxPath(path, user), user.getExtId());
    }

    @Override
    public void removeLiveLoggingPathToBlock(final RestMethodEnum method,
                                             final String path,
                                             final String token) throws ValidationException {

        final SmockinUser user = userTokenServiceUtils.loadCurrentActiveUser(token);
        final String amendedPath = amendMultiUserCtxPath(path, user);

        mockedRestServerEngine.removePathFromLiveBlocking(method, amendedPath, user.getExtId());

        if (mockedRestServerEngine.countLiveBlockingPathsForUser(method, amendedPath, user.getExtId()) == 0) {
            mockedRestServerEngine.notifyBlockedLiveLoggingCalls(Optional.of(method), amendedPath);
        }

    }

    String amendMultiUserCtxPath(final String path, final SmockinUser user) throws ValidationException {

        if (UserModeEnum.ACTIVE.equals(smockinUserService.getUserMode())) {

            if (SmockinUserRoleEnum.SYS_ADMIN.equals(user.getRole())) {
                // Can enter and block any path they wish
                return path;
            }

            // Prevent non admin users from entering another user's mock path.
            final String userCtxPathSegment = mockedRestServerEngineUtils.extractMultiUserCtxPathSegment(path);

            if (!StringUtils.equalsIgnoreCase(user.getCtxPath(), userCtxPathSegment)
                    && mockedRestServerEngineUtils.isInboundPathMultiUserPath(userCtxPathSegment)) {
                throw new ValidationException("You cannot block another user's mock");
            }

            final String userCtxPath = GeneralUtils.URL_PATH_SEPARATOR + user.getCtxPath();

            // Check if path already contains userCtxPath
            return (StringUtils.startsWith(path, userCtxPath))
                    ? path
                    : userCtxPath + path;
        }

        return path;
    }

    @Override
    public Optional<String> exportProxyMappings(final String token) {

        final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);
        final ProxyForwardUserConfig proxyForwardUserConfig = proxyForwardUserConfigDAO.findByUser(smockinUser.getId());

        final List<ProxyForwardMappingDTO> dtos = proxyForwardUserConfig.getProxyForwardMappings()
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

        return Optional.of(GeneralUtils.base64Encode(exportBytes));
    }

    @Override
    public String importProxyMappingsFile(final MultipartFile file, final boolean keepExisting, final String token)
            throws MockImportException, ValidationException {

        final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);
        final ProxyForwardUserConfig proxyForwardUserConfig = proxyForwardUserConfigDAO.findByUser(smockinUser.getId());

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
                proxyForwardUserConfig.getProxyForwardMappings().clear();
                proxyForwardUserConfigDAO.saveAndFlush(proxyForwardUserConfig);

            } else {

                final List<String> paths = proxyForwardUserConfig.getProxyForwardMappings()
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
            saveUserProxyMappings(proxyForwardUserConfig, proxyForwardMappingDTOs);

        } catch (IOException ex) {
            throw new MockExportException("Error importing proxy mappings file");
        }

        return null;
    }

    public void clearAllMailMessages(final StoreTypeEnum storeType,
                                     final String token) throws AuthException {

        smockinUserService.assertCurrentUserIsAdmin(userTokenServiceUtils.loadCurrentActiveUser(token));

        if (StoreTypeEnum.DB.equals(storeType)) {
            mailMockMessageDAO.deleteAll();
        } else if (StoreTypeEnum.CACHE.equals(storeType)) {
            mockedMailServerEngine.purgeAllMailMessagesForAllInboxes();
        }
    }

    void saveUserProxyMappings(final ProxyForwardUserConfig proxyForwardUserConfig,
                               final List<ProxyForwardMappingDTO> proxyForwardMappings) {

        proxyForwardUserConfig.getProxyForwardMappings().clear();

        proxyForwardUserConfigDAO.saveAndFlush(proxyForwardUserConfig);

        if (!proxyForwardMappings.isEmpty()) {

            proxyForwardUserConfig.getProxyForwardMappings().addAll(
                    proxyForwardMappings
                            .stream()
                            .map(dto ->
                                toProxyForwardMapping(dto, proxyForwardUserConfig)
                            ).collect(Collectors.toList()));

        }

        proxyForwardUserConfigDAO.save(proxyForwardUserConfig);
    }

    ProxyForwardMapping toProxyForwardMapping(final ProxyForwardMappingDTO proxyForwardMappingDTO,
                                              final ProxyForwardUserConfig proxyForwardUserConfig) {

        final ProxyForwardMapping proxyForwardMapping = new ProxyForwardMapping();

        proxyForwardMapping.setProxyForwardUserConfig(proxyForwardUserConfig);
        proxyForwardMapping.setPath(
                (!StringUtils.startsWith(proxyForwardMappingDTO.getPath(), GeneralUtils.URL_PATH_SEPARATOR)
                        && !StringUtils.equals(proxyForwardMappingDTO.getPath(), GeneralUtils.PATH_WILDCARD)) ? GeneralUtils.URL_PATH_SEPARATOR : ""
                        + proxyForwardMappingDTO.getPath());
        proxyForwardMapping.setProxyForwardUrl(proxyForwardMappingDTO.getProxyForwardUrl());
        proxyForwardMapping.setDisabled(proxyForwardMappingDTO.isDisabled());

        return proxyForwardMapping;

    }

    void autoStartManager(final ServerTypeEnum serverType) throws MockServerException {

        if (serverType == null) {
            return;
        }

        switch (serverType) {
            case RESTFUL:
                startRest();
                break;
            case S3:
                startS3();
                break;
            case MAIL:
                startMail();
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
