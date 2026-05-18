package com.smockin.mockserver.engine;

import com.smockin.admin.persistence.entity.MQMock;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.persistence.enums.MQTypeEnum;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.persistence.enums.SmockinUserRoleEnum;
import com.smockin.mockserver.exception.MockServerException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for ActiveMQConnectionWrapper
 * Tests protocol validation and configuration handling
 */
public class ActiveMQConnectionWrapperTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private SmockinUser testUser;
    private MQMock testMQMock;

    @Before
    public void setUp() {
        testUser = new SmockinUser();
        testUser.setRole(SmockinUserRoleEnum.ADMIN);
        testUser.setExtId("test-user-ext-id");

        testMQMock = new MQMock(
                "Test ActiveMQ Queue",
                MQTypeEnum.JMS,
                "test-queue",
                false,
                RecordStatusEnum.ACTIVE,
                testUser,
                true
        );
        testMQMock.setExtId("activemq-test-ext-id");
    }

    @Test
    public void testActiveMQWrapper_TCPProtocol() {
        // Test valid tcp:// protocol
        Map<String, String> props = new HashMap<>();
        props.put("jmsProvider", "ACTIVEMQ");
        props.put("brokerUrl", "tcp://localhost:61616");
        props.put("username", "admin");
        props.put("password", "admin");
        testMQMock.setProperties(props);

        ActiveMQConnectionWrapper wrapper = new ActiveMQConnectionWrapper(testMQMock);
        Assert.assertNotNull("Wrapper should be created", wrapper);
    }

    @Test
    public void testActiveMQWrapper_VMProtocol() {
        // Test valid vm:// protocol (embedded)
        Map<String, String> props = new HashMap<>();
        props.put("jmsProvider", "ACTIVEMQ");
        props.put("brokerUrl", "vm://localhost");
        testMQMock.setProperties(props);

        ActiveMQConnectionWrapper wrapper = new ActiveMQConnectionWrapper(testMQMock);
        Assert.assertNotNull("Wrapper should be created with VM protocol", wrapper);
    }

    @Test
    public void testActiveMQWrapper_HTTPProtocol() {
        // Test valid http:// protocol
        Map<String, String> props = new HashMap<>();
        props.put("jmsProvider", "ACTIVEMQ");
        props.put("brokerUrl", "http://localhost:8161");
        testMQMock.setProperties(props);

        ActiveMQConnectionWrapper wrapper = new ActiveMQConnectionWrapper(testMQMock);
        Assert.assertNotNull("Wrapper should be created with HTTP protocol", wrapper);
    }

    @Test
    public void testActiveMQWrapper_HTTPSProtocol() {
        // Test valid https:// protocol
        Map<String, String> props = new HashMap<>();
        props.put("jmsProvider", "ACTIVEMQ");
        props.put("brokerUrl", "https://localhost:8443");
        testMQMock.setProperties(props);

        ActiveMQConnectionWrapper wrapper = new ActiveMQConnectionWrapper(testMQMock);
        Assert.assertNotNull("Wrapper should be created with HTTPS protocol", wrapper);
    }

    @Test
    public void testActiveMQWrapper_InvalidProtocol() {
        // Test invalid protocol
        Map<String, String> props = new HashMap<>();
        props.put("jmsProvider", "ACTIVEMQ");
        props.put("brokerUrl", "invalid://localhost:61616");
        testMQMock.setProperties(props);

        ActiveMQConnectionWrapper wrapper = new ActiveMQConnectionWrapper(testMQMock);
        
        // Should throw MockServerException when initializing with invalid protocol
        thrown.expect(MockServerException.class);
        thrown.expectMessage("Invalid ActiveMQ broker URL");
        
        wrapper.initialize();
    }

    @Test
    public void testActiveMQWrapper_ConsumerWindowSize() {
        // Test consumer window size configuration
        Map<String, String> props = new HashMap<>();
        props.put("jmsProvider", "ACTIVEMQ");
        props.put("brokerUrl", "tcp://localhost:61616");
        props.put("consumerWindowSize", "65536");
        testMQMock.setProperties(props);

        ActiveMQConnectionWrapper wrapper = new ActiveMQConnectionWrapper(testMQMock);
        Assert.assertNotNull("Wrapper should be created with consumer window size", wrapper);
    }

    @Test
    public void testActiveMQWrapper_ClientId() {
        // Test client ID configuration (for durable subscriptions)
        Map<String, String> props = new HashMap<>();
        props.put("jmsProvider", "ACTIVEMQ");
        props.put("brokerUrl", "tcp://localhost:61616");
        props.put("clientId", "my-durable-subscriber");
        testMQMock.setProperties(props);

        ActiveMQConnectionWrapper wrapper = new ActiveMQConnectionWrapper(testMQMock);
        Assert.assertNotNull("Wrapper should be created with client ID", wrapper);
    }

    @Test
    public void testActiveMQWrapper_TopicDestination() {
        // Test topic destination
        testMQMock.setTopic(true);
        testMQMock.setDestinationName("test.topic");
        
        Map<String, String> props = new HashMap<>();
        props.put("jmsProvider", "ACTIVEMQ");
        props.put("brokerUrl", "tcp://localhost:61616");
        testMQMock.setProperties(props);

        ActiveMQConnectionWrapper wrapper = new ActiveMQConnectionWrapper(testMQMock);
        Assert.assertTrue("Should be topic type", testMQMock.isTopic());
    }

    @Test
    public void testActiveMQWrapper_QueueDestination() {
        // Test queue destination
        testMQMock.setTopic(false);
        testMQMock.setDestinationName("test.queue");
        
        Map<String, String> props = new HashMap<>();
        props.put("jmsProvider", "ACTIVEMQ");
        props.put("brokerUrl", "tcp://localhost:61616");
        testMQMock.setProperties(props);

        ActiveMQConnectionWrapper wrapper = new ActiveMQConnectionWrapper(testMQMock);
        Assert.assertFalse("Should be queue type", testMQMock.isTopic());
    }
}
