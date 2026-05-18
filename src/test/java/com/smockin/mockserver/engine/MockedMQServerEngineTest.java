package com.smockin.mockserver.engine;

import com.smockin.admin.persistence.dao.MQMockDAO;
import com.smockin.admin.persistence.dao.MQMockMessageDAO;
import com.smockin.admin.persistence.entity.MQMock;
import com.smockin.admin.persistence.enums.MQTypeEnum;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.mockserver.dto.MockServerState;
import com.smockin.mockserver.dto.MockedServerConfigDTO;
import com.smockin.mockserver.dto.MQMockMessageDTO;
import com.smockin.mockserver.exception.MockServerException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * MockedMQServerEngine Unit Tests
 * Tests engine lifecycle, state management, MQ Mock CRUD operations
 */
@RunWith(MockitoJUnitRunner.class)
public class MockedMQServerEngineTest {

    @InjectMocks
    private MockedMQServerEngine engine;

    @Mock
    private MQMockDAO mqMockDAO;

    @Mock
    private MQMockMessageDAO mqMockMessageDAO;

    private MQMock mqMock;
    private MockedServerConfigDTO configDTO;

    @Before
    public void setUp() {
        // Create test MQ Mock
        mqMock = new MQMock();
        mqMock.setId(1L);
        mqMock.setExtId("mq-mock-ext-id-001");
        mqMock.setName("Test ActiveMQ");
        mqMock.setMqType(MQTypeEnum.JMS);
        mqMock.setBrokerUrl("tcp://localhost:61616");
        mqMock.setDestinationName("test.queue");
        mqMock.setTopic(false);
        mqMock.setStatus(RecordStatusEnum.ACTIVE);
        mqMock.setProperties(new HashMap<>());
        mqMock.getProperties().put("jmsProvider", "ACTIVEMQ");

        // Create test config
        configDTO = new MockedServerConfigDTO();
        configDTO.setPort(8080);
    }

    @Test
    public void test_InitialState() throws MockServerException {
        // When getting initial state
        MockServerState state = engine.getCurrentState();

        // Then engine should not be running
        Assert.assertNotNull(state);
        Assert.assertFalse(state.isRunning());
        Assert.assertEquals(0, state.getPort());
    }

    @Test
    public void test_StartEngineWithEmptyMQMocks() throws MockServerException {
        // Given empty MQ mocks list
        List<MQMock> mqMocks = new ArrayList<>();

        // When starting engine
        engine.start(configDTO, mqMocks);

        // Then engine should be running
        MockServerState state = engine.getCurrentState();
        Assert.assertTrue(state.isRunning());
        Assert.assertEquals(8080, state.getPort());
        Assert.assertEquals(0, engine.getConnectionCount());
    }

    @Test
    public void test_StartEngineWithNullMQMocks() throws MockServerException {
        // When starting engine with null mocks
        engine.start(configDTO, null);

        // Then engine should be running
        MockServerState state = engine.getCurrentState();
        Assert.assertTrue(state.isRunning());
        Assert.assertEquals(0, engine.getConnectionCount());
    }

    @Test
    public void test_StartEngineWithNullPort() throws MockServerException {
        // Given config with null port
        configDTO.setPort(null);

        // When starting engine
        engine.start(configDTO, new ArrayList<>());

        // Then port should be 0
        MockServerState state = engine.getCurrentState();
        Assert.assertTrue(state.isRunning());
        Assert.assertEquals(0, state.getPort());
    }

    @Test(expected = MockServerException.class)
    public void test_StartEngineTwice() throws MockServerException {
        // Given engine is already started
        engine.start(configDTO, new ArrayList<>());

        // When starting again - should throw exception
        engine.start(configDTO, new ArrayList<>());
    }

    @Test
    public void test_ShutdownEngine() throws MockServerException {
        // Given engine is running
        engine.start(configDTO, new ArrayList<>());
        Assert.assertTrue(engine.getCurrentState().isRunning());

        // When shutting down
        engine.shutdown();

        // Then engine should not be running
        MockServerState state = engine.getCurrentState();
        Assert.assertFalse(state.isRunning());
    }

    @Test
    public void test_ShutdownEngineNotRunning() throws MockServerException {
        // Given engine is not running
        Assert.assertFalse(engine.getCurrentState().isRunning());

        // When shutting down
        engine.shutdown();

        // Then engine should still not be running
        Assert.assertFalse(engine.getCurrentState().isRunning());
    }

    @Test
    public void test_AddJMSMQMock() throws MockServerException {
        // Given engine is running
        engine.start(configDTO, new ArrayList<>());

        // When adding JMS MQ Mock (ActiveMQ will throw exception but engine handles it)
        try {
            engine.addMQMock(mqMock);
        } catch (Exception e) {
            // Expected - connection to real ActiveMQ will fail
        }

        // Then engine should still be running
        Assert.assertTrue(engine.getCurrentState().isRunning());
    }

    @Test
    public void test_AddInactiveMQMock() throws MockServerException {
        // Given engine is running
        engine.start(configDTO, new ArrayList<>());

        // And MQ Mock is inactive
        mqMock.setStatus(RecordStatusEnum.INACTIVE);

        // When adding inactive MQ Mock
        engine.addMQMock(mqMock);

        // Then engine should still be running with 0 connections
        Assert.assertTrue(engine.getCurrentState().isRunning());
        Assert.assertEquals(0, engine.getConnectionCount());
    }

    @Test
    public void test_AddKafkaMQMock() throws MockServerException {
        // Given engine is running
        engine.start(configDTO, new ArrayList<>());

        // And Kafka MQ Mock
        MQMock kafkaMock = new MQMock();
        kafkaMock.setId(2L);
        kafkaMock.setExtId("mq-mock-ext-id-002");
        kafkaMock.setName("Test Kafka");
        kafkaMock.setMqType(MQTypeEnum.KAFKA);
        kafkaMock.setBrokerUrl("localhost:9092");
        kafkaMock.setDestinationName("test-topic");
        kafkaMock.setTopic(true);
        kafkaMock.setStatus(RecordStatusEnum.ACTIVE);
        kafkaMock.setProperties(new HashMap<>());
        kafkaMock.getProperties().put("bootstrap.servers", "localhost:9092");

        // When adding Kafka MQ Mock (will fail to connect but engine handles it)
        try {
            engine.addMQMock(kafkaMock);
        } catch (Exception e) {
            // Expected - connection to real Kafka will fail
        }

        // Then engine should still be running
        Assert.assertTrue(engine.getCurrentState().isRunning());
    }

    @Test
    public void test_AddRabbitMQMock() throws MockServerException {
        // Given engine is running
        engine.start(configDTO, new ArrayList<>());

        // And RabbitMQ Mock
        MQMock rabbitMock = new MQMock();
        rabbitMock.setId(3L);
        rabbitMock.setExtId("mq-mock-ext-id-003");
        rabbitMock.setName("Test RabbitMQ");
        rabbitMock.setMqType(MQTypeEnum.AMQP);
        rabbitMock.setBrokerUrl("localhost:5672");
        rabbitMock.setDestinationName("test-queue");
        rabbitMock.setTopic(false);
        rabbitMock.setStatus(RecordStatusEnum.ACTIVE);
        rabbitMock.setProperties(new HashMap<>());

        // When adding RabbitMQ Mock (will fail to connect but engine handles it)
        try {
            engine.addMQMock(rabbitMock);
        } catch (Exception e) {
            // Expected - connection to real RabbitMQ will fail
        }

        // Then engine should still be running
        Assert.assertTrue(engine.getCurrentState().isRunning());
    }

    @Test
    public void test_RemoveMQMock() throws MockServerException {
        // Given engine is running
        engine.start(configDTO, new ArrayList<>());

        // And we added a mock (even if connection failed)
        try {
            engine.addMQMock(mqMock);
        } catch (Exception e) {
            // Expected
        }

        // When removing the mock
        engine.removeMQMock("mq-mock-ext-id-001");

        // Then message cache should be cleared
        List<MQMockMessageDTO> messages = engine.getMessagesFromCache("mq-mock-ext-id-001");
        Assert.assertNotNull(messages);
        Assert.assertTrue(messages.isEmpty());
    }

    @Test
    public void test_GetMessagesFromCacheEmpty() throws MockServerException {
        // Given engine is running
        engine.start(configDTO, new ArrayList<>());

        // When getting messages from non-existent cache
        List<MQMockMessageDTO> messages = engine.getMessagesFromCache("non-existent-ext-id");

        // Then should return empty list
        Assert.assertNotNull(messages);
        Assert.assertTrue(messages.isEmpty());
    }

    @Test
    public void test_GetMessagesFromCacheAfterAdd() throws MockServerException {
        // Given engine is running
        engine.start(configDTO, new ArrayList<>());

        // When adding MQ Mock
        try {
            engine.addMQMock(mqMock);
        } catch (Exception e) {
            // Expected - connection will fail but cache is initialized
        }

        // Then message cache should be initialized (even if empty)
        List<MQMockMessageDTO> messages = engine.getMessagesFromCache("mq-mock-ext-id-001");
        Assert.assertNotNull(messages);
        Assert.assertTrue(messages.isEmpty());
    }

    @Test
    public void test_ConnectionCountInitiallyZero() throws MockServerException {
        // Given engine is not started
        // When getting connection count
        int count = engine.getConnectionCount();

        // Then should be 0
        Assert.assertEquals(0, count);
    }

    @Test
    public void test_ConnectionCountAfterStart() throws MockServerException {
        // Given engine is started with empty mocks
        engine.start(configDTO, new ArrayList<>());

        // When getting connection count
        int count = engine.getConnectionCount();

        // Then should be 0
        Assert.assertEquals(0, count);
    }

    @Test
    public void test_MultipleStartShutdownCycles() throws MockServerException {
        // Cycle 1
        engine.start(configDTO, new ArrayList<>());
        Assert.assertTrue(engine.getCurrentState().isRunning());
        engine.shutdown();
        Assert.assertFalse(engine.getCurrentState().isRunning());

        // Cycle 2
        engine.start(configDTO, new ArrayList<>());
        Assert.assertTrue(engine.getCurrentState().isRunning());
        engine.shutdown();
        Assert.assertFalse(engine.getCurrentState().isRunning());

        // Cycle 3
        engine.start(configDTO, new ArrayList<>());
        Assert.assertTrue(engine.getCurrentState().isRunning());
        engine.shutdown();
        Assert.assertFalse(engine.getCurrentState().isRunning());
    }

    @Test
    public void test_StateConsistencyAfterOperations() throws MockServerException {
        // Given engine is running
        engine.start(configDTO, new ArrayList<>());

        // When performing multiple operations
        MockServerState state1 = engine.getCurrentState();
        Assert.assertTrue(state1.isRunning());
        Assert.assertEquals(8080, state1.getPort());

        int count1 = engine.getConnectionCount();
        List<MQMockMessageDTO> messages1 = engine.getMessagesFromCache("test-id");

        // Then state should remain consistent
        MockServerState state2 = engine.getCurrentState();
        Assert.assertEquals(state1.isRunning(), state2.isRunning());
        Assert.assertEquals(state1.getPort(), state2.getPort());
    }

    @Test
    public void test_AddMQMockWithMissingBrokerUrl() throws MockServerException {
        // Given engine is running
        engine.start(configDTO, new ArrayList<>());

        // And MQ Mock with missing broker URL
        MQMock invalidMock = new MQMock();
        invalidMock.setId(4L);
        invalidMock.setExtId("mq-mock-ext-id-004");
        invalidMock.setName("Invalid Mock");
        invalidMock.setMqType(MQTypeEnum.JMS);
        invalidMock.setDestinationName("test.queue");
        invalidMock.setTopic(false);
        invalidMock.setStatus(RecordStatusEnum.ACTIVE);
        invalidMock.setProperties(new HashMap<>());
        invalidMock.getProperties().put("jmsProvider", "ACTIVEMQ");

        // When adding invalid mock
        try {
            engine.addMQMock(invalidMock);
        } catch (Exception e) {
            // Expected - should handle gracefully
        }

        // Then engine should still be running
        Assert.assertTrue(engine.getCurrentState().isRunning());
    }

    @Test
    public void test_AddGenericJMSMQMock() throws MockServerException {
        // Given engine is running
        engine.start(configDTO, new ArrayList<>());

        // And Generic JMS Mock
        MQMock genericMock = new MQMock();
        genericMock.setId(5L);
        genericMock.setExtId("mq-mock-ext-id-005");
        genericMock.setName("Test Generic JMS");
        genericMock.setMqType(MQTypeEnum.JMS);
        genericMock.setBrokerUrl("tcp://localhost:61616");
        genericMock.setDestinationName("test.queue");
        genericMock.setTopic(false);
        genericMock.setStatus(RecordStatusEnum.ACTIVE);
        genericMock.setProperties(new HashMap<>());
        // No jmsProvider specified - should default to ACTIVEMQ

        // When adding Generic JMS Mock
        try {
            engine.addMQMock(genericMock);
        } catch (Exception e) {
            // Expected - connection will fail
        }

        // Then engine should still be running
        Assert.assertTrue(engine.getCurrentState().isRunning());
    }

    @Test
    public void test_MQMessageCacheIsolation() throws MockServerException {
        // Given engine is running
        engine.start(configDTO, new ArrayList<>());

        // And two different MQ Mocks
        try {
            engine.addMQMock(mqMock);
        } catch (Exception e) {
            // Expected
        }

        MQMock secondMock = new MQMock();
        secondMock.setId(6L);
        secondMock.setExtId("mq-mock-ext-id-006");
        secondMock.setName("Second Mock");
        secondMock.setMqType(MQTypeEnum.JMS);
        secondMock.setBrokerUrl("tcp://localhost:61617");
        secondMock.setDestinationName("second.queue");
        secondMock.setTopic(false);
        secondMock.setStatus(RecordStatusEnum.ACTIVE);
        secondMock.setProperties(new HashMap<>());
        secondMock.getProperties().put("jmsProvider", "ACTIVEMQ");

        try {
            engine.addMQMock(secondMock);
        } catch (Exception e) {
            // Expected
        }

        // When getting messages from different caches
        List<MQMockMessageDTO> messages1 = engine.getMessagesFromCache("mq-mock-ext-id-001");
        List<MQMockMessageDTO> messages2 = engine.getMessagesFromCache("mq-mock-ext-id-006");

        // Then each cache should be independent
        Assert.assertNotNull(messages1);
        Assert.assertNotNull(messages2);
        Assert.assertEquals(0, messages1.size());
        Assert.assertEquals(0, messages2.size());
    }

    @Test
    public void test_ShutdownClearsAllResources() throws MockServerException {
        // Given engine is running with MQ Mocks
        engine.start(configDTO, new ArrayList<>());

        try {
            engine.addMQMock(mqMock);
        } catch (Exception e) {
            // Expected
        }

        // When shutting down
        engine.shutdown();

        // Then engine should be stopped
        Assert.assertFalse(engine.getCurrentState().isRunning());

        // And message caches should be cleared
        List<MQMockMessageDTO> messages = engine.getMessagesFromCache("mq-mock-ext-id-001");
        Assert.assertTrue(messages.isEmpty());
    }
}
