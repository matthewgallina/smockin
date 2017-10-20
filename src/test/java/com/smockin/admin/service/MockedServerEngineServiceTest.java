package com.smockin.admin.service;

import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.RestfulMockDAO;
import com.smockin.admin.persistence.dao.ServerConfigDAO;
import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.persistence.entity.ServerConfig;
import com.smockin.admin.persistence.enums.ServerTypeEnum;
import com.smockin.mockserver.dto.MockServerState;
import com.smockin.mockserver.dto.MockedServerConfigDTO;
import com.smockin.mockserver.engine.MockedRestServerEngine;
import com.smockin.mockserver.exception.MockServerException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Created by mgallina on 21/07/17.
 */
@RunWith(MockitoJUnitRunner.class)
public class MockedServerEngineServiceTest {

    @Mock
    private MockedRestServerEngine mockedRestServerEngine;

    @Mock
    private RestfulMockDAO restfulMockDefinitionDAO;

    @Mock
    private ServerConfigDAO serverConfigDAO;

    @Spy
    @InjectMocks
    private MockedServerEngineService mockedServerEngineService = new MockedServerEngineServiceImpl();

    @Spy
    @InjectMocks
    private MockedServerEngineServiceImpl mockedServerEngineServiceImpl = new MockedServerEngineServiceImpl();

    @Rule
    public ExpectedException thrown = ExpectedException.none();


    @Test(expected = RecordNotFoundException.class)
    public void loadServerConfig_NotFound_Test() throws RecordNotFoundException {

        mockedServerEngineService.loadServerConfig(ServerTypeEnum.RESTFUL);

    }

    @Test
    public void loadServerConfig_Found_Test() throws RecordNotFoundException {

        // Setup
        final ServerConfig serverConfig = new ServerConfig(ServerTypeEnum.RESTFUL);
        serverConfig.setPort(8001);
        serverConfig.setMaxThreads(10);
        serverConfig.setMinThreads(5);
        serverConfig.setTimeOutMillis(30000);
        serverConfig.setAutoStart(true);
        serverConfig.setAutoRefresh(true);
        serverConfig.getNativeProperties().put("serverName", "foo");

        Mockito.when(serverConfigDAO.findByServerType(Matchers.any(ServerTypeEnum.class))).thenReturn(serverConfig);

        // Test
        final MockedServerConfigDTO dto = mockedServerEngineService.loadServerConfig(ServerTypeEnum.RESTFUL);

        // Assertions
        Assert.assertNotNull(dto);

        Assert.assertEquals(serverConfig.getPort(), dto.getPort());
        Assert.assertEquals(serverConfig.getMaxThreads(), dto.getMaxThreads());
        Assert.assertEquals(serverConfig.getMinThreads(), dto.getMinThreads());
        Assert.assertEquals(serverConfig.getTimeOutMillis(), dto.getTimeOutMillis());
        Assert.assertEquals(serverConfig.isAutoStart(), dto.isAutoStart());
        Assert.assertEquals(serverConfig.isAutoRefresh(), dto.isAutoRefresh());

        Assert.assertEquals(1, dto.getNativeProperties().size());
        Assert.assertEquals("foo", dto.getNativeProperties().get("serverName"));

    }

    @Test
    public void saveServerConfig_NotFoundCreateNew_Test() throws ValidationException {

        // Setup
        final MockedServerConfigDTO dto = new MockedServerConfigDTO();
        dto.setPort(8001);
        dto.setMaxThreads(10);
        dto.setMinThreads(1);
        dto.setTimeOutMillis(30000);
        dto.setAutoStart(true);
        dto.setAutoRefresh(true);
        dto.getNativeProperties().put("serverName", "foo");

        // Test
        mockedServerEngineService.saveServerConfig(ServerTypeEnum.RESTFUL, dto);

        // Assertions
        final ArgumentCaptor<ServerConfig> argument = ArgumentCaptor.forClass(ServerConfig.class);
        Mockito.verify(serverConfigDAO).saveAndFlush(argument.capture());

        Assert.assertEquals(dto.getPort(), argument.getValue().getPort());
        Assert.assertEquals(dto.getMaxThreads(), argument.getValue().getMaxThreads());
        Assert.assertEquals(dto.getMinThreads(), argument.getValue().getMinThreads());
        Assert.assertEquals(dto.getTimeOutMillis(), argument.getValue().getTimeOutMillis());
        Assert.assertEquals(dto.isAutoStart(), argument.getValue().isAutoStart());
        Assert.assertEquals(dto.isAutoRefresh(), argument.getValue().isAutoRefresh());
        Assert.assertEquals(dto.isAutoStart(), argument.getValue().isAutoStart());

        Assert.assertNotNull(argument.getValue().getNativeProperties());
        Assert.assertEquals(1, argument.getValue().getNativeProperties().size());
        Assert.assertEquals(dto.getNativeProperties().get("serverName"), argument.getValue().getNativeProperties().get("serverName"));

    }

    @Test
    public void saveServerConfig_UpdateExisting_Test() throws ValidationException {

        // Setup
        final MockedServerConfigDTO dto = new MockedServerConfigDTO();
        dto.setPort(8001);
        dto.setMaxThreads(10);
        dto.setMinThreads(1);
        dto.setTimeOutMillis(30000);
        dto.setAutoStart(true);
        dto.setAutoRefresh(true);
        dto.getNativeProperties().put("serverName", "foo");

        final ServerConfig serverConfig = new ServerConfig();
        Mockito.when(serverConfigDAO.findByServerType(Matchers.any(ServerTypeEnum.class))).thenReturn(serverConfig);

        // Test
        mockedServerEngineService.saveServerConfig(ServerTypeEnum.RESTFUL, dto);

        // Assertions
        Assert.assertEquals(dto.getPort(), serverConfig.getPort());
        Assert.assertEquals(dto.getMaxThreads(), serverConfig.getMaxThreads());
        Assert.assertEquals(dto.getMinThreads(), serverConfig.getMinThreads());
        Assert.assertEquals(dto.getTimeOutMillis(), serverConfig.getTimeOutMillis());
        Assert.assertEquals(dto.isAutoStart(), serverConfig.isAutoStart());
        Assert.assertEquals(dto.isAutoRefresh(), serverConfig.isAutoRefresh());
        Assert.assertEquals(dto.isAutoStart(), serverConfig.isAutoStart());

        Assert.assertNotNull(serverConfig.getNativeProperties());
        Assert.assertEquals(1, serverConfig.getNativeProperties().size());
        Assert.assertEquals(dto.getNativeProperties().get("serverName"), serverConfig.getNativeProperties().get("serverName"));

    }

    @Test
    public void saveServerConfig_ValidationFailure_Test() throws ValidationException {

        // Assertions
        thrown.expect(ValidationException.class);
        thrown.expectMessage("config is required");

        // Test
        mockedServerEngineService.saveServerConfig(ServerTypeEnum.RESTFUL, null);

    }

    @Test
    public void validateServerConfig_Null_Test() throws ValidationException {

        // Assertions
        thrown.expect(ValidationException.class);
        thrown.expectMessage("config is required");

        // Test
        mockedServerEngineServiceImpl.validateServerConfig(null);

    }

    @Test
    public void validateServerConfig_MissingPort_Test() throws ValidationException {

        // Assertions
        thrown.expect(ValidationException.class);
        thrown.expectMessage("'port' config value is required");

        // Test
        mockedServerEngineServiceImpl.validateServerConfig(new MockedServerConfigDTO());

    }

    @Test
    public void validateServerConfig_MissingMaxThreads_Test() throws ValidationException {

        // Assertions
        thrown.expect(ValidationException.class);
        thrown.expectMessage("'maxThreads' config value is required");

        // Setup
        final MockedServerConfigDTO dto = new MockedServerConfigDTO();
        dto.setPort(8001);

        // Test
        mockedServerEngineServiceImpl.validateServerConfig(dto);

    }

    @Test
    public void validateServerConfig_MissingMinThreads_Test() throws ValidationException {

        // Assertions
        thrown.expect(ValidationException.class);
        thrown.expectMessage("'minThreads' config value is required");

        // Setup
        final MockedServerConfigDTO dto = new MockedServerConfigDTO();
        dto.setPort(8001);
        dto.setMaxThreads(10);

        // Test
        mockedServerEngineServiceImpl.validateServerConfig(dto);

    }

    @Test
    public void validateServerConfig_MissingTimeOutMillis_Test() throws ValidationException {

        // Assertions
        thrown.expect(ValidationException.class);
        thrown.expectMessage("'timeOutMillis' config value is required");

        // Setup
        final MockedServerConfigDTO dto = new MockedServerConfigDTO();
        dto.setPort(8001);
        dto.setMaxThreads(10);
        dto.setMinThreads(1);

        // Test
        mockedServerEngineServiceImpl.validateServerConfig(dto);

    }

    @Test
    public void validateServerConfigTest() throws ValidationException {

        // Setup
        final MockedServerConfigDTO dto = new MockedServerConfigDTO();
        dto.setPort(8001);
        dto.setMaxThreads(10);
        dto.setMinThreads(1);
        dto.setTimeOutMillis(30000);

        // Test
        mockedServerEngineServiceImpl.validateServerConfig(dto);

    }

    @Test
    public void startRestTest() throws MockServerException {

        // Setup
        final ServerConfig serverConfig = new ServerConfig(ServerTypeEnum.RESTFUL);
        serverConfig.setPort(8001);
        serverConfig.setMaxThreads(10);
        serverConfig.setMinThreads(5);
        serverConfig.setTimeOutMillis(30000);
        serverConfig.setAutoStart(true);
        serverConfig.setAutoRefresh(true);
        serverConfig.getNativeProperties().put("serverName", "foo");

        Mockito.when(serverConfigDAO.findByServerType(Matchers.any(ServerTypeEnum.class))).thenReturn(serverConfig);

        // Test
        final MockedServerConfigDTO dto = mockedServerEngineService.startRest();

        // Assertions
        Assert.assertNotNull(dto);

        Assert.assertEquals(serverConfig.getPort(), dto.getPort());
        Assert.assertEquals(serverConfig.getMaxThreads(), dto.getMaxThreads());
        Assert.assertEquals(serverConfig.getMinThreads(), dto.getMinThreads());
        Assert.assertEquals(serverConfig.getTimeOutMillis(), dto.getTimeOutMillis());
        Assert.assertEquals(serverConfig.isAutoStart(), dto.isAutoStart());
        Assert.assertEquals(serverConfig.isAutoRefresh(), dto.isAutoRefresh());
        Assert.assertEquals(serverConfig.getNativeProperties().size(), dto.getNativeProperties().size());
        Assert.assertEquals(serverConfig.getNativeProperties().get("serverName"), dto.getNativeProperties().get("serverName"));

    }

    @Test
    public void startRest_ConfigNotFound_Test() throws MockServerException {

        // Assertions
        thrown.expect(MockServerException.class);
        thrown.expectMessage("Missing mock REST server config");

        // Test
        mockedServerEngineService.startRest();

    }

    @Test
    public void startRest_GeneralFailure_Test() throws MockServerException {

        // Assertions
        thrown.expect(MockServerException.class);
        thrown.expectMessage("Startup Boom");

        // Setup
        final ServerConfig serverConfig = Mockito.mock(ServerConfig.class);
        Mockito.when(serverConfigDAO.findByServerType(Matchers.any(ServerTypeEnum.class))).thenReturn(serverConfig);
        Mockito.doThrow(new MockServerException("Startup Boom")).when(mockedRestServerEngine).start(Matchers.any(MockedServerConfigDTO.class), Matchers.anyListOf(RestfulMock.class));

        // Test
        mockedServerEngineService.startRest();

    }

    @Test
    public void shutdownRestTest() throws MockServerException {

        mockedServerEngineService.shutdownRest();

    }

    @Test
    public void shutdownRest_GeneralFailure_Test() throws MockServerException {

        // Assertions
        thrown.expect(MockServerException.class);
        thrown.expectMessage("Shutdown Boom");

        // Setup
        Mockito.doThrow(new MockServerException("Shutdown Boom")).when(mockedRestServerEngine).shutdown();

        // Test
        mockedServerEngineService.shutdownRest();

    }

    @Test
    public void autoStartManagerTest() throws MockServerException {

        // Setup
        final ServerConfig serverConfig = Mockito.mock(ServerConfig.class);
        Mockito.when(serverConfigDAO.findByServerType(Matchers.any(ServerTypeEnum.class))).thenReturn(serverConfig);

        // Test
        mockedServerEngineServiceImpl.autoStartManager(ServerTypeEnum.RESTFUL);

        // Assertions
        Mockito.verify(mockedRestServerEngine, Mockito.times(1)).start(Matchers.any(MockedServerConfigDTO.class), Matchers.anyListOf(RestfulMock.class));

    }

    @Test
    public void autoStartManager_Null_Test() throws MockServerException {

        // Test
        mockedServerEngineServiceImpl.autoStartManager(null);

        // Assertions
        Mockito.verify(mockedRestServerEngine, Mockito.never()).start(Matchers.any(MockedServerConfigDTO.class), Matchers.anyListOf(RestfulMock.class));
    }

    @Test
    public void getRestServerStateTest() throws MockServerException {

        // Setup
        Mockito.when(mockedRestServerEngine.getCurrentState()).thenReturn(new MockServerState(true, 8001));

        // Test
        final MockServerState mockServerState = mockedServerEngineService.getRestServerState();

        // Assertions
        Assert.assertNotNull(mockServerState);
        Assert.assertTrue(mockServerState.isRunning());
        Assert.assertEquals(8001, mockServerState.getPort());

    }

    @Test
    public void handleServerAutoStartTest() {

        // TODO

    }

    @Test
    public void restartRestTest() {

        // TODO

    }

}
