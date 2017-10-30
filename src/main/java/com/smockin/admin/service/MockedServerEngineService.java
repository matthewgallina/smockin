package com.smockin.admin.service;

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

    MockedServerConfigDTO startRest() throws MockServerException;
    MockServerState getRestServerState() throws MockServerException;
    void shutdownRest() throws MockServerException;
    MockedServerConfigDTO restartRest() throws MockServerException;

    MockedServerConfigDTO startJms() throws MockServerException;
    MockServerState getJmsServerState() throws MockServerException;
    void shutdownJms() throws MockServerException;
    MockedServerConfigDTO restartJms() throws MockServerException;

    MockedServerConfigDTO loadServerConfig(final ServerTypeEnum serverType) throws RecordNotFoundException;
    void saveServerConfig(final ServerTypeEnum serverType, final MockedServerConfigDTO config) throws ValidationException;
    void handleServerAutoStart();

}
