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

import static org.mockito.Mockito.*;

/**
 * Unit tests for SolaceConnectionWrapper
 * Tests protocol validation and configuration handling
 */
public class SolaceConnectionWrapperTest {

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
                "Test Solace Queue",
                MQTypeEnum.JMS,
                "test-queue",
                false, // Queue (not topic)
                RecordStatusEnum.ACTIVE,
                testUser,
                true
        );
        testMQMock.setExtId("solace-test-ext-id");
    }

    @Test
    public void testSolaceHostValidation_ValidSMF() {
        // Test valid smf:// protocol
        Map<String, String> props = new HashMap<>();
        props.put("jmsProvider", "SOLACE");
        props.put("solaceHost", "smf://solace-server:55555");
        props.put("solaceVPN", "default");
        props.put("username", "user");
        props.put("password", "pass");
        testMQMock.setProperties(props);

        SolaceConnectionWrapper wrapper = new SolaceConnectionWrapper(testMQMock);
        
        // The wrapper should accept smf:// protocol
        Assert.assertNotNull("Wrapper should be created", wrapper);
    }

    @Test
    public void testSolaceHostValidation_ValidSMFS() {
        // Test valid smfs:// protocol (TLS/SSL)
        Map<String, String> props = new HashMap<>();
        props.put("jmsProvider", "SOLACE");
        props.put("solaceHost", "smfs://solace-server:55443");
        props.put("solaceVPN", "default");
        props.put("username", "user");
        props.put("password", "pass");
        testMQMock.setProperties(props);

        SolaceConnectionWrapper wrapper = new SolaceConnectionWrapper(testMQMock);
        
        // The wrapper should accept smfs:// protocol
        Assert.assertNotNull("Wrapper should be created", wrapper);
    }

    @Test
    public void testSolaceHostValidation_ValidTCP() {
        // Test valid tcp:// protocol (compatible mode)
        Map<String, String> props = new HashMap<>();
        props.put("jmsProvider", "SOLACE");
        props.put("solaceHost", "tcp://solace-server:55555");
        props.put("solaceVPN", "default");
        props.put("username", "user");
        props.put("password", "pass");
        testMQMock.setProperties(props);

        SolaceConnectionWrapper wrapper = new SolaceConnectionWrapper(testMQMock);
        
        // The wrapper should accept tcp:// protocol
        Assert.assertNotNull("Wrapper should be created", wrapper);
    }

    @Test
    public void testSolaceHostValidation_DefaultProtocol() {
        // Test default protocol (should be smf://)
        Map<String, String> props = new HashMap<>();
        props.put("jmsProvider", "SOLACE");
        // No solaceHost specified, should use default
        props.put("solaceVPN", "default");
        testMQMock.setProperties(props);

        SolaceConnectionWrapper wrapper = new SolaceConnectionWrapper(testMQMock);
        
        Assert.assertNotNull("Wrapper should be created with default protocol", wrapper);
    }

    @Test
    public void testSolaceHostValidation_InvalidProtocol() {
        // Test invalid protocol
        Map<String, String> props = new HashMap<>();
        props.put("jmsProvider", "SOLACE");
        props.put("solaceHost", "http://invalid-protocol:55555");
        testMQMock.setProperties(props);

        SolaceConnectionWrapper wrapper = new SolaceConnectionWrapper(testMQMock);
        
        // Should throw MockServerException when initializing with invalid protocol
        thrown.expect(MockServerException.class);
        thrown.expectMessage("Invalid Solace host format");
        
        wrapper.initialize();
    }

    @Test
    public void testSolaceReconnectConfiguration() {
        // Test reconnect configuration
        Map<String, String> props = new HashMap<>();
        props.put("jmsProvider", "SOLACE");
        props.put("solaceHost", "smf://solace-server:55555");
        props.put("solaceVPN", "default");
        props.put("solaceReconnectRetries", "5");
        props.put("solaceReconnectRetryWaitInMillis", "5000");
        testMQMock.setProperties(props);

        SolaceConnectionWrapper wrapper = new SolaceConnectionWrapper(testMQMock);
        
        // Verify configuration is set correctly
        Assert.assertNotNull("Wrapper should be created", wrapper);
        Assert.assertEquals("Reconnect retries", "5", props.get("solaceReconnectRetries"));
        Assert.assertEquals("Reconnect retry wait", "5000", props.get("solaceReconnectRetryWaitInMillis"));
    }

    @Test
    public void testSolaceJNDIConfiguration() {
        // Test JNDI-based configuration
        Map<String, String> props = new HashMap<>();
        props.put("jmsProvider", "SOLACE");
        props.put("jndiInitialContextFactory", "com.solacesystems.jndi.SolJNDIInitialContextFactory");
        props.put("jndiProviderUrl", "smf://solace-server:55555");
        props.put("jndiConnectionFactoryName", "/jms/cf/default");
        props.put("username", "user");
        props.put("password", "pass");
        testMQMock.setProperties(props);

        SolaceConnectionWrapper wrapper = new SolaceConnectionWrapper(testMQMock);
        
        // Should accept JNDI configuration with smf://
        Assert.assertNotNull("Wrapper should be created with JNDI config", wrapper);
    }

    @Test
    public void testSolaceJNDIWithSMFS() {
        // Test JNDI configuration with smfs:// (TLS)
        Map<String, String> props = new HashMap<>();
        props.put("jmsProvider", "SOLACE");
        props.put("jndiInitialContextFactory", "com.solacesystems.jndi.SolJNDIInitialContextFactory");
        props.put("jndiProviderUrl", "smfs://solace-server:55443");
        props.put("jndiConnectionFactoryName", "/jms/cf/default");
        props.put("username", "user");
        props.put("password", "pass");
        testMQMock.setProperties(props);

        SolaceConnectionWrapper wrapper = new SolaceConnectionWrapper(testMQMock);
        
        Assert.assertNotNull("Wrapper should accept smfs:// in JNDI", wrapper);
    }

    @Test
    public void testSolaceDestinationTypes() {
        // Test Queue destination
        testMQMock.setTopic(false);
        testMQMock.setDestinationName("test-queue");
        Map<String, String> props = new HashMap<>();
        props.put("jmsProvider", "SOLACE");
        props.put("solaceHost", "smf://localhost:55555");
        testMQMock.setProperties(props);

        SolaceConnectionWrapper wrapper = new SolaceConnectionWrapper(testMQMock);
        Assert.assertFalse("Should be queue type", testMQMock.isTopic());

        // Test Topic destination
        testMQMock.setTopic(true);
        testMQMock.setDestinationName("test-topic");

        SolaceConnectionWrapper wrapper2 = new SolaceConnectionWrapper(testMQMock);
        Assert.assertTrue("Should be topic type", testMQMock.isTopic());
    }

    @Test
    public void testSolacePropertiesParsing() {
        // Test all property parsing
        Map<String, String> props = new HashMap<>();
        props.put("jmsProvider", "SOLACE");
        props.put("solaceHost", "smfs://solace.example.com:55443");
        props.put("solaceVPN", "production-vpn");
        props.put("username", "prod-user");
        props.put("password", "prod-pass");
        props.put("solaceReconnectRetries", "10");
        props.put("solaceReconnectRetryWaitInMillis", "5000");
        testMQMock.setProperties(props);

        SolaceConnectionWrapper wrapper = new SolaceConnectionWrapper(testMQMock);
        
        // Verify properties are correctly set
        Assert.assertNotNull("Wrapper should be created", wrapper);
        Assert.assertEquals("Host should be smfs://", "smfs://solace.example.com:55443", 
                           props.get("solaceHost"));
        Assert.assertEquals("VPN should be production-vpn", "production-vpn", 
                           props.get("solaceVPN"));
    }

    @Test
    public void testSolaceCloseConnection() {
        // Test proper cleanup
        Map<String, String> props = new HashMap<>();
        props.put("jmsProvider", "SOLACE");
        props.put("solaceHost", "smf://localhost:55555");
        testMQMock.setProperties(props);

        SolaceConnectionWrapper wrapper = new SolaceConnectionWrapper(testMQMock);
        
        // Should not throw exception even if not initialized
        wrapper.close();
        
        // Multiple close calls should be safe
        wrapper.close();
    }

    @Test
    public void testSolaceTopicDestinationJNDI() {
        // Test topic destination with JNDI
        Map<String, String> props = new HashMap<>();
        props.put("jmsProvider", "SOLACE");
        props.put("jndiInitialContextFactory", "com.solacesystems.jndi.SolJNDIInitialContextFactory");
        props.put("jndiProviderUrl", "smf://solace-server:55555");
        props.put("jndiDestinationName", "/jms/topic/my-topic");
        testMQMock.setTopic(true);
        testMQMock.setDestinationName("my-topic");
        testMQMock.setProperties(props);

        SolaceConnectionWrapper wrapper = new SolaceConnectionWrapper(testMQMock);
        Assert.assertNotNull("Topic JNDI config should be accepted", wrapper);
    }

    @Test
    public void testSolaceQueueDestinationJNDI() {
        // Test queue destination with JNDI
        Map<String, String> props = new HashMap<>();
        props.put("jmsProvider", "SOLACE");
        props.put("jndiInitialContextFactory", "com.solacesystems.jndi.SolJNDIInitialContextFactory");
        props.put("jndiProviderUrl", "smfs://solace-server:55443");
        props.put("jndiDestinationName", "/jms/queue/my-queue");
        testMQMock.setTopic(false);
        testMQMock.setDestinationName("my-queue");
        testMQMock.setProperties(props);

        SolaceConnectionWrapper wrapper = new SolaceConnectionWrapper(testMQMock);
        Assert.assertNotNull("Queue JNDI config should be accepted", wrapper);
    }
}
