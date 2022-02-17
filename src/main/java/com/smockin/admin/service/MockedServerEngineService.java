package com.smockin.admin.service;

import com.smockin.admin.enums.StoreTypeEnum;
import com.smockin.admin.exception.AuthException;
import com.smockin.admin.exception.MockImportException;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.admin.persistence.enums.ServerTypeEnum;
import com.smockin.mockserver.dto.MockServerState;
import com.smockin.mockserver.dto.MockedServerConfigDTO;
import com.smockin.mockserver.dto.ProxyForwardConfigDTO;
import com.smockin.mockserver.dto.ProxyForwardConfigResponseDTO;
import com.smockin.mockserver.exception.MockServerException;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

/**
 * Created by mgallina.
 */
public interface MockedServerEngineService {

    // Rest
    MockedServerConfigDTO startRest(final String token) throws MockServerException, RecordNotFoundException, AuthException;
    MockServerState getRestServerState() throws MockServerException;
    void shutdownRest(final String token) throws MockServerException, RecordNotFoundException, AuthException;
    MockedServerConfigDTO restartRest(final String token) throws MockServerException, RecordNotFoundException, AuthException;

    // S3
    MockedServerConfigDTO startS3(final String token) throws MockServerException, RecordNotFoundException, AuthException;
    MockServerState getS3ServerState() throws MockServerException;
    void shutdownS3(final String token) throws MockServerException, RecordNotFoundException, AuthException;
    MockedServerConfigDTO restartS3(final String token) throws MockServerException, RecordNotFoundException, AuthException;

    // Mail
    MockedServerConfigDTO startMail(final String token) throws MockServerException, RecordNotFoundException, AuthException;
    MockServerState getMailServerState() throws MockServerException;
    void shutdownMail(final String token) throws MockServerException, RecordNotFoundException, AuthException;
    MockedServerConfigDTO restartMail(final String token) throws MockServerException, RecordNotFoundException, AuthException;
    void clearAllMailMessages(final StoreTypeEnum storeType, final String token) throws AuthException;

    // Config
    MockedServerConfigDTO loadServerConfig(final ServerTypeEnum serverType) throws RecordNotFoundException;
    void saveServerConfig(final ServerTypeEnum serverType, final MockedServerConfigDTO config, final String token) throws RecordNotFoundException, AuthException, ValidationException;
    void handleServerAutoStart();

    void addLiveLoggingPathToBlock(final RestMethodEnum method,
                                   final String path,
                                   final String token) throws ValidationException;

    void removeLiveLoggingPathToBlock(final RestMethodEnum method,
                                      final String path,
                                      final String token) throws ValidationException;

    Optional<String> exportProxyMappings(final String token);

    String importProxyMappingsFile(final MultipartFile file, final boolean keepExisting, final String token)
            throws MockImportException, ValidationException;

    void updateProxyMode(final boolean enableProxyMode, final String token) throws AuthException;

    ProxyForwardConfigResponseDTO loadProxyForwardMappingsForUser(final String token);
    void saveProxyForwardMappingsForUser(final ProxyForwardConfigDTO proxyForwardConfigDTO,
                                          final String token) throws AuthException, ValidationException, RecordNotFoundException;

}
