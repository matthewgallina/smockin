package com.smockin.mockserver.engine;

import com.smockin.admin.persistence.entity.MQMock;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.persistence.enums.MQTypeEnum;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.persistence.enums.SmockinUserRoleEnum;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for RabbitMQConnectionWrapper
 * Tests AMQP protocol configuration and routing
 */
public class RabbitMQConnectionWrapperTest {

    private SmockinUser testUser;
    private MQMock testMQMock;

    @Before
    public void setUp() {
        testUser = new SmockinUser();
        testUser.setRole(SmockinUserRoleEnum.ADMIN);
        testUser.setExtId("test-user-ext-id");

        testMQMock = new MQMock(
                "Test RabbitMQ Queue",
                MQTypeEnum.AMQP,
                "test-queue",
                false,
                RecordStatusEnum.ACTIVE,
                testUser,
                true
        );
        testMQMock.setExtId("rabbitmq-test-ext-id");
    }

    @Test
    public void testRabbitMQWrapper_DefaultConfiguration() {
        // Test default configuration (localhost:5672)
        Map<String, String> props = new HashMap<>();
        props.put("host", "localhost");
        props.put("port", "5672");
        props.put("username", "guest");
        props.put("password", "guest");
        testMQMock.setProperties(props);

        RabbitMQConnectionWrapper wrapper = new RabbitMQConnectionWrapper(testMQMock);
        Assert.assertNotNull("Wrapper should be created", wrapper);
        Assert.assertEquals("RABBITMQ", wrapper.getConnectionType());
    }

    @Test
    public void testRabbitMQWrapper_DirectExchange() {
        // Test direct exchange configuration
        Map<String, String> props = new HashMap<>();
        props.put("host", "localhost");
        props.put("port", "5672");
        props.put("exchangeName", "my-direct-exchange");
        props.put("exchangeType", "direct");
        props.put("routingKey", "my-routing-key");
        testMQMock.setProperties(props);

        RabbitMQConnectionWrapper wrapper = new RabbitMQConnectionWrapper(testMQMock);
        Assert.assertNotNull("Wrapper should be created with direct exchange", wrapper);
    }

    @Test
    public void testRabbitMQWrapper_FanoutExchange() {
        // Test fanout exchange configuration
        Map<String, String> props = new HashMap<>();
        props.put("host", "localhost");
        props.put("port", "5672");
        props.put("exchangeName", "my-fanout-exchange");
        props.put("exchangeType", "fanout");
        testMQMock.setProperties(props);

        RabbitMQConnectionWrapper wrapper = new RabbitMQConnectionWrapper(testMQMock);
        Assert.assertNotNull("Wrapper should be created with fanout exchange", wrapper);
    }

    @Test
    public void testRabbitMQWrapper_TopicExchange() {
        // Test topic exchange configuration
        Map<String, String> props = new HashMap<>();
        props.put("host", "localhost");
        props.put("port", "5672");
        props.put("exchangeName", "my-topic-exchange");
        props.put("exchangeType", "topic");
        props.put("routingKey", "*.important.*");
        testMQMock.setProperties(props);

        RabbitMQConnectionWrapper wrapper = new RabbitMQConnectionWrapper(testMQMock);
        Assert.assertNotNull("Wrapper should be created with topic exchange", wrapper);
    }

    @Test
    public void testRabbitMQWrapper_DurableQueue() {
        // Test durable queue configuration
        Map<String, String> props = new HashMap<>();
        props.put("host", "localhost");
        props.put("port", "5672");
        props.put("durable", "true");
        testMQMock.setProperties(props);

        RabbitMQConnectionWrapper wrapper = new RabbitMQConnectionWrapper(testMQMock);
        Assert.assertNotNull("Wrapper should be created with durable queue", wrapper);
    }

    @Test
    public void testRabbitMQWrapper_NonDurableQueue() {
        // Test non-durable queue configuration
        Map<String, String> props = new HashMap<>();
        props.put("host", "localhost");
        props.put("port", "5672");
        props.put("durable", "false");
        testMQMock.setProperties(props);

        RabbitMQConnectionWrapper wrapper = new RabbitMQConnectionWrapper(testMQMock);
        Assert.assertNotNull("Wrapper should be created with non-durable queue", wrapper);
    }

    @Test
    public void testRabbitMQWrapper_PrefetchCount() {
        // Test prefetch count configuration
        Map<String, String> props = new HashMap<>();
        props.put("host", "localhost");
        props.put("port", "5672");
        props.put("prefetchCount", "10");
        testMQMock.setProperties(props);

        RabbitMQConnectionWrapper wrapper = new RabbitMQConnectionWrapper(testMQMock);
        Assert.assertNotNull("Wrapper should be created with prefetch count", wrapper);
    }

    @Test
    public void testRabbitMQWrapper_VirtualHost() {
        // Test virtual host configuration
        Map<String, String> props = new HashMap<>();
        props.put("host", "localhost");
        props.put("port", "5672");
        props.put("virtualHost", "/my-vhost");
        props.put("username", "my-user");
        props.put("password", "my-password");
        testMQMock.setProperties(props);

        RabbitMQConnectionWrapper wrapper = new RabbitMQConnectionWrapper(testMQMock);
        Assert.assertNotNull("Wrapper should be created with virtual host", wrapper);
    }

    @Test
    public void testRabbitMQWrapper_CustomPort() {
        // Test custom port configuration
        Map<String, String> props = new HashMap<>();
        props.put("host", "rabbitmq-server");
        props.put("port", "5673");
        testMQMock.setProperties(props);

        RabbitMQConnectionWrapper wrapper = new RabbitMQConnectionWrapper(testMQMock);
        Assert.assertNotNull("Wrapper should be created with custom port", wrapper);
    }

    @Test
    public void testRabbitMQWrapper_AutoDeleteQueue() {
        // Test auto-delete queue configuration
        Map<String, String> props = new HashMap<>();
        props.put("host", "localhost");
        props.put("port", "5672");
        props.put("autoDelete", "true");
        testMQMock.setProperties(props);

        RabbitMQConnectionWrapper wrapper = new RabbitMQConnectionWrapper(testMQMock);
        Assert.assertNotNull("Wrapper should be created with auto-delete queue", wrapper);
    }
}
