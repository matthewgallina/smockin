package com.smockin.mockserver.engine;

import com.smockin.admin.persistence.entity.MQMock;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.persistence.enums.MQTypeEnum;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.persistence.enums.SmockinUserRoleEnum;
import com.smockin.mockserver.exception.MockServerException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.test.rule.EmbeddedKafkaRule;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Integration tests for Kafka connection using Embedded Kafka
 * Tests complete message flow: Producer send -> Consumer receive
 */
public class KafkaConnectionWrapperIntegrationTest {

    private final Logger logger = LoggerFactory.getLogger(KafkaConnectionWrapperIntegrationTest.class);

    @ClassRule
    public static EmbeddedKafkaRule embeddedKafka = new EmbeddedKafkaRule(1, false, 
        "test-topic-1", "test-topic-2", "test-topic-3", "test-topic-4", "test-topic-5");

    private KafkaConnectionWrapper kafkaWrapper;
    private MQMock mqMock;
    private SmockinUser testUser;

    @Before
    public void setUp() {
        testUser = new SmockinUser();
        testUser.setRole(SmockinUserRoleEnum.ADMIN);
        testUser.setExtId("test-user-ext-id");

        // Create MQ Mock for Kafka with embedded Kafka bootstrap servers
        String bootstrapServers = embeddedKafka.getEmbeddedKafka().getBrokersAsString();
        logger.info("Embedded Kafka bootstrap servers: {}", bootstrapServers);

        mqMock = new MQMock(
                "Test Kafka Topic",
                MQTypeEnum.KAFKA,
                "test-topic",
                true, // Topic
                RecordStatusEnum.ACTIVE,
                testUser,
                true
        );
        mqMock.setExtId("kafka-test-ext-id");
        mqMock.setProperties(new HashMap<>());
        mqMock.getProperties().put("bootstrapServers", bootstrapServers);
        mqMock.getProperties().put("groupId", "smockin-test-group-" + System.currentTimeMillis());
    }

    @After
    public void tearDown() {
        if (kafkaWrapper != null) {
            kafkaWrapper.close();
        }
    }

    @Test
    public void testKafkaProducerSendAndConsumerReceive() throws Exception {
        logger.info("Testing Kafka producer send and consumer receive with embedded Kafka");

        // Use unique topic for this test
        mqMock.setDestinationName("test-topic-1");
        
        // Create wrapper and initialize
        kafkaWrapper = new KafkaConnectionWrapper(mqMock);
        
        // Setup message receiver latch
        CountDownLatch messageLatch = new CountDownLatch(1);
        List<ConsumerRecord<String, String>> receivedMessages = new ArrayList<>();

        // Initialize wrapper
        kafkaWrapper.initialize();

        // Start message listener
        kafkaWrapper.startMessageListener(record -> {
            logger.info("Received message: key={}, value={}", record.key(), record.value());
            receivedMessages.add(record);
            messageLatch.countDown();
        });

        // Give consumer time to start and subscribe
        Thread.sleep(1000);

        // Send message using wrapper
        String messageKey = "test-key-1";
        String messageBody = "{\"test\": \"message body\"}";
        String contentType = "application/json";
        Map<String, String> headers = new HashMap<>();
        headers.put("custom-header", "custom-value");

        CompletableFuture<RecordMetadata> sendFuture = kafkaWrapper.sendMessage(
            messageKey, messageBody, contentType, headers
        );

        // Wait for send to complete
        RecordMetadata metadata = sendFuture.get(5, TimeUnit.SECONDS);
        Assert.assertNotNull("Send metadata should not be null", metadata);
        Assert.assertEquals("Topic should match", "test-topic-1", metadata.topic());
        logger.info("Message sent to topic-partition: {}-{} at offset {}", 
            metadata.topic(), metadata.partition(), metadata.offset());

        // Wait for message to be received
        boolean received = messageLatch.await(5, TimeUnit.SECONDS);
        Assert.assertTrue("Message should be received within timeout", received);

        // Verify received message
        Assert.assertEquals("Should receive 1 message", 1, receivedMessages.size());
        ConsumerRecord<String, String> receivedRecord = receivedMessages.get(0);
        Assert.assertEquals("Message key should match", messageKey, receivedRecord.key());
        Assert.assertEquals("Message body should match", messageBody, receivedRecord.value());
        
        // Verify headers
        org.apache.kafka.common.header.Header customHeader = receivedRecord.headers().lastHeader("custom-header");
        Assert.assertNotNull("Custom header should exist", customHeader);
        Assert.assertEquals("Custom header value should match", 
            "custom-value", new String(customHeader.value(), StandardCharsets.UTF_8));
        
        org.apache.kafka.common.header.Header contentTypeHeader = receivedRecord.headers().lastHeader("content-type");
        Assert.assertNotNull("Content-type header should exist", contentTypeHeader);
        Assert.assertEquals("Content-type should match", 
            contentType, new String(contentTypeHeader.value(), StandardCharsets.UTF_8));

        logger.info("Kafka producer and consumer test passed");
    }

    @Test
    public void testKafkaSendMessageWithoutKey() throws Exception {
        logger.info("Testing Kafka send message without key");

        // Use unique topic for this test
        mqMock.setDestinationName("test-topic-2");
        
        // Create wrapper
        kafkaWrapper = new KafkaConnectionWrapper(mqMock);
        kafkaWrapper.initialize();

        CountDownLatch messageLatch = new CountDownLatch(1);
        List<ConsumerRecord<String, String>> receivedMessages = new ArrayList<>();

        kafkaWrapper.startMessageListener(record -> {
            receivedMessages.add(record);
            messageLatch.countDown();
        });

        Thread.sleep(1000);

        // Send message without key
        String messageBody = "message-without-key-" + System.currentTimeMillis();
        CompletableFuture<RecordMetadata> sendFuture = kafkaWrapper.sendMessage(
            null, messageBody, "text/plain", null
        );

        RecordMetadata metadata = sendFuture.get(5, TimeUnit.SECONDS);
        Assert.assertNotNull("Send metadata should not be null", metadata);

        boolean received = messageLatch.await(5, TimeUnit.SECONDS);
        Assert.assertTrue("Message should be received", received);

        Assert.assertTrue("Should receive at least 1 message", receivedMessages.size() >= 1);
        // Check that our message is in the received messages
        boolean found = receivedMessages.stream().anyMatch(r -> r.value().equals(messageBody));
        Assert.assertTrue("Received messages should contain our message", found);

        logger.info("Kafka send without key test passed");
    }

    @Test
    public void testKafkaSendMultipleMessages() throws Exception {
        logger.info("Testing Kafka send multiple messages");

        // Use unique topic for this test
        mqMock.setDestinationName("test-topic-3");
        
        // Create wrapper
        kafkaWrapper = new KafkaConnectionWrapper(mqMock);
        kafkaWrapper.initialize();

        int messageCount = 3;
        CountDownLatch messageLatch = new CountDownLatch(messageCount);
        List<ConsumerRecord<String, String>> receivedMessages = new ArrayList<>();

        kafkaWrapper.startMessageListener(record -> {
            receivedMessages.add(record);
            messageLatch.countDown();
        });

        Thread.sleep(1000);

        // Send multiple messages with unique content
        String prefix = "multi-" + System.currentTimeMillis();
        for (int i = 0; i < messageCount; i++) {
            String messageKey = "key-" + i;
            String messageBody = prefix + "-message-" + i;
            kafkaWrapper.sendMessage(messageKey, messageBody, "text/plain", null);
        }

        // Wait for all messages to be received
        boolean received = messageLatch.await(10, TimeUnit.SECONDS);
        Assert.assertTrue("All messages should be received", received);

        Assert.assertTrue("Should receive at least " + messageCount + " messages", receivedMessages.size() >= messageCount);
        // Verify our messages are present
        for (int i = 0; i < messageCount; i++) {
            String expectedBody = prefix + "-message-" + i;
            boolean found = receivedMessages.stream().anyMatch(r -> r.value().equals(expectedBody));
            Assert.assertTrue("Message " + i + " should be present", found);
        }

        logger.info("Kafka send multiple messages test passed");
    }

    @Test
    public void testKafkaFlushProducer() throws Exception {
        logger.info("Testing Kafka producer flush");

        // Use unique topic for this test
        mqMock.setDestinationName("test-topic-4");
        
        // Create wrapper
        kafkaWrapper = new KafkaConnectionWrapper(mqMock);
        kafkaWrapper.initialize();

        CountDownLatch messageLatch = new CountDownLatch(1);
        List<ConsumerRecord<String, String>> receivedMessages = new ArrayList<>();

        kafkaWrapper.startMessageListener(record -> {
            receivedMessages.add(record);
            messageLatch.countDown();
        });

        Thread.sleep(1000);

        // Send message and flush
        kafkaWrapper.sendMessage("flush-key", "flush-message", "text/plain", null);
        kafkaWrapper.flush();

        boolean received = messageLatch.await(5, TimeUnit.SECONDS);
        Assert.assertTrue("Flushed message should be received", received);
        Assert.assertEquals("Should receive 1 message", 1, receivedMessages.size());

        logger.info("Kafka producer flush test passed");
    }

    @Test
    public void testKafkaWrapperClose() throws Exception {
        logger.info("Testing Kafka wrapper close");

        // Use unique topic for this test
        mqMock.setDestinationName("test-topic-5");
        
        // Create wrapper
        kafkaWrapper = new KafkaConnectionWrapper(mqMock);
        kafkaWrapper.initialize();

        // Verify connections are initialized
        Assert.assertNotNull("Producer should be initialized", kafkaWrapper.getProducer());
        Assert.assertNotNull("Consumer should be initialized", kafkaWrapper.getConsumer());
        Assert.assertNotNull("MQ Mock should be accessible", kafkaWrapper.getMqMock());

        // Close wrapper
        kafkaWrapper.close();

        // After close, producer and consumer should be closed (not necessarily null)
        logger.info("Kafka wrapper close test passed");
    }
}
