package com.smockin.admin.service;

import com.smockin.admin.exception.AuthException;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.enums.ServerTypeEnum;
import com.smockin.mockserver.dto.MockServerState;
import com.smockin.mockserver.dto.MockedServerConfigDTO;
import com.smockin.mockserver.exception.MockServerException;

/**
 * Created by mgallina.
 */
public interface MockedServerEngineService {

    MockedServerConfigDTO startRest(final String token) throws MockServerException, RecordNotFoundException, AuthException;
    MockServerState getRestServerState() throws MockServerException;
    void shutdownRest(final String token) throws MockServerException, RecordNotFoundException, AuthException;
    MockedServerConfigDTO restartRest(final String token) throws MockServerException, RecordNotFoundException, AuthException;

    MockedServerConfigDTO startJms(final String token) throws MockServerException, RecordNotFoundException, AuthException;
    MockServerState getJmsServerState() throws MockServerException;
    void shutdownJms(final String token) throws MockServerException, RecordNotFoundException, AuthException;
    MockedServerConfigDTO restartJms(final String token) throws MockServerException, RecordNotFoundException, AuthException;

    MockedServerConfigDTO startFtp(final String token) throws MockServerException, RecordNotFoundException, AuthException;
    MockServerState getFtpServerState() throws MockServerException;
    void shutdownFtp(final String token) throws MockServerException, RecordNotFoundException, AuthException;
    MockedServerConfigDTO restartFtp(final String token) throws MockServerException, RecordNotFoundException, AuthException;

    MockedServerConfigDTO loadServerConfig(final ServerTypeEnum serverType) throws RecordNotFoundException;
    void saveServerConfig(final ServerTypeEnum serverType, final MockedServerConfigDTO config, final String token) throws RecordNotFoundException, AuthException, ValidationException;
    void handleServerAutoStart();

}
