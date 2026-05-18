package com.smockin.mockserver.engine;

import com.smockin.admin.persistence.entity.MQMock;
import com.smockin.mockserver.exception.MockServerException;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.*;

/**
 * Kafka Connection Wrapper
 * Manages Kafka Producer and Consumer lifecycle
 */
public class KafkaConnectionWrapper implements AutoCloseable {

    private final Logger logger = LoggerFactory.getLogger(KafkaConnectionWrapper.class);

    private final MQMock mqMock;
    private Producer<String, String> producer;
    private Consumer<String, String> consumer;
    private ExecutorService consumerExecutor;
    private volatile boolean running = false;

    public KafkaConnectionWrapper(final MQMock mqMock) {
        this.mqMock = mqMock;
    }

    /**
     * Initialize Kafka Producer and Consumer
     */
    public void initialize() throws MockServerException {
        try {
            Map<String, String> props = mqMock.getProperties();
            
            String bootstrapServers = props.getOrDefault("bootstrapServers", "localhost:9092");
            String groupId = props.getOrDefault("groupId", "smockin-consumer-group");
            String username = props.get("username");
            String password = props.get("password");

            // Initialize Producer
            producer = createProducer(bootstrapServers, username, password);

            // Initialize Consumer
            consumer = createConsumer(bootstrapServers, groupId, username, password);

            // Subscribe to topic
            consumer.subscribe(Collections.singletonList(mqMock.getDestinationName()));

            running = true;
            logger.info("Kafka connection initialized for topic: {}", mqMock.getDestinationName());

        } catch (Exception e) {
            throw new MockServerException("Failed to initialize Kafka connection", e);
        }
    }

    /**
     * Create Kafka Producer
     */
    private Producer<String, String> createProducer(String bootstrapServers, 
                                                     String username, 
                                                     String password) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");

        // SASL authentication if credentials provided
        if (username != null && password != null) {
            props.put("security.protocol", "SASL_PLAINTEXT");
            props.put("sasl.mechanism", "PLAIN");
            String jaasConfig = String.format(
                "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";",
                username, password
            );
            props.put("sasl.jaas.config", jaasConfig);
        }

        return new KafkaProducer<>(props);
    }

    /**
     * Create Kafka Consumer
     */
    private Consumer<String, String> createConsumer(String bootstrapServers, 
                                                     String groupId, 
                                                     String username, 
                                                     String password) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");

        // SASL authentication if credentials provided
        if (username != null && password != null) {
            props.put("security.protocol", "SASL_PLAINTEXT");
            props.put("sasl.mechanism", "PLAIN");
            String jaasConfig = String.format(
                "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";",
                username, password
            );
            props.put("sasl.jaas.config", jaasConfig);
        }

        return new KafkaConsumer<>(props);
    }

    /**
     * Start message listener in background thread
     */
    public void startMessageListener(java.util.function.Consumer<ConsumerRecord<String, String>> messageHandler) {
        if (consumerExecutor != null) {
            logger.warn("Consumer listener already started for: {}", mqMock.getDestinationName());
            return;
        }

        consumerExecutor = Executors.newSingleThreadExecutor();
        consumerExecutor.submit(() -> {
            logger.info("Starting Kafka consumer listener for topic: {}", mqMock.getDestinationName());
            
            while (running) {
                try {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                    for (ConsumerRecord<String, String> record : records) {
                        try {
                            messageHandler.accept(record);
                        } catch (Exception e) {
                            logger.error("Error handling Kafka message", e);
                        }
                    }
                } catch (Exception e) {
                    if (running) {
                        logger.error("Error polling Kafka messages", e);
                    }
                }
            }
            
            logger.info("Kafka consumer listener stopped for topic: {}", mqMock.getDestinationName());
        });
    }

    /**
     * Send message to Kafka topic
     */
    public CompletableFuture<RecordMetadata> sendMessage(String messageKey, 
                                                          String messageBody, 
                                                          String contentType,
                                                          Map<String, String> headers) {
        ProducerRecord<String, String> record;
        
        if (messageKey != null) {
            record = new ProducerRecord<>(mqMock.getDestinationName(), messageKey, messageBody);
        } else {
            record = new ProducerRecord<>(mqMock.getDestinationName(), messageBody);
        }

        // Add headers
        if (headers != null && !headers.isEmpty()) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                record.headers().add(entry.getKey(), entry.getValue().getBytes());
            }
        }
        
        if (contentType != null) {
            record.headers().add("content-type", contentType.getBytes());
        }

        CompletableFuture<RecordMetadata> future = new CompletableFuture<>();
        
        producer.send(record, (metadata, exception) -> {
            if (exception != null) {
                future.completeExceptionally(exception);
            } else {
                future.complete(metadata);
            }
        });

        return future;
    }

    /**
     * Flush producer
     */
    public void flush() {
        if (producer != null) {
            producer.flush();
        }
    }

    /**
     * Get consumer
     */
    public Consumer<String, String> getConsumer() {
        return consumer;
    }

    /**
     * Get producer
     */
    public Producer<String, String> getProducer() {
        return producer;
    }

    /**
     * Get MQ Mock configuration
     */
    public MQMock getMqMock() {
        return mqMock;
    }

    /**
     * Close Kafka connections
     */
    @Override
    public void close() {
        logger.info("Closing Kafka connection for topic: {}", mqMock.getDestinationName());
        
        running = false;
        
        if (consumerExecutor != null) {
            consumerExecutor.shutdown();
            try {
                if (!consumerExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    consumerExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                consumerExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        if (consumer != null) {
            try {
                consumer.unsubscribe();
                consumer.close();
            } catch (Exception e) {
                logger.error("Error closing Kafka consumer", e);
            }
        }
        
        if (producer != null) {
            try {
                producer.flush();
                producer.close();
            } catch (Exception e) {
                logger.error("Error closing Kafka producer", e);
            }
        }
        
        logger.info("Kafka connection closed for topic: {}", mqMock.getDestinationName());
    }
}
