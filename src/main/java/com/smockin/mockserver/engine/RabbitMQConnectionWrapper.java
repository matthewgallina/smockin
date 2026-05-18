package com.smockin.mockserver.engine;

import com.rabbitmq.client.*;
import com.smockin.admin.persistence.entity.MQMock;
import com.smockin.mockserver.exception.MockServerException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * RabbitMQ AMQP Connection Wrapper
 * 
 * Supports RabbitMQ via AMQP 0-9-1 protocol.
 * RabbitMQ is the most popular open-source message broker.
 * 
 * Configuration Options:
 * - host: localhost (default)
 * - port: 5672 (default)
 * - virtualHost: / (default)
 * - username: guest (default)
 * - password: guest (default)
 * - exchangeName: optional (default: "")
 * - exchangeType: direct|topic|fanout|headers (default: direct)
 * - routingKey: optional (default: destinationName)
 * - durable: true|false (default: true)
 * - autoDelete: true|false (default: false)
 * - prefetchCount: optional (default: 0, unlimited)
 * 
 * Protocol Support:
 * - AMQP 0-9-1 (RabbitMQ default protocol)
 */
public class RabbitMQConnectionWrapper implements IMQConnectionWrapper {

    private final Logger logger = LoggerFactory.getLogger(RabbitMQConnectionWrapper.class);

    private final MQMock mqMock;
    private Connection connection;
    private Channel channel;
    private String exchangeName;
    private String routingKey;
    private String consumerTag;

    public RabbitMQConnectionWrapper(final MQMock mqMock) {
        this.mqMock = mqMock;
    }

    /**
     * Initialize RabbitMQ connection
     */
    public void initialize() throws MockServerException {
        logger.debug("Initializing RabbitMQ connection for: {}", mqMock.getDestinationName());

        try {
            Map<String, String> props = mqMock.getProperties();

            // Connection configuration
            String host = props.getOrDefault("host", "localhost");
            int port = Integer.parseInt(props.getOrDefault("port", "5672"));
            String virtualHost = props.getOrDefault("virtualHost", "/");
            String username = props.getOrDefault("username", "guest");
            String password = props.getOrDefault("password", "guest");

            logger.info("Connecting to RabbitMQ at {}:{}", host, port);

            // Create connection factory
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(host);
            factory.setPort(port);
            factory.setVirtualHost(virtualHost);
            factory.setUsername(username);
            factory.setPassword(password);

            // Optional: Configure connection properties
            if (props.containsKey("connectionTimeout")) {
                factory.setConnectionTimeout(Integer.parseInt(props.get("connectionTimeout")));
            }
            if (props.containsKey("handshakeTimeout")) {
                factory.setHandshakeTimeout(Integer.parseInt(props.get("handshakeTimeout")));
            }
            if (props.containsKey("requestedHeartbeat")) {
                factory.setRequestedHeartbeat(Integer.parseInt(props.get("requestedHeartbeat")));
            }

            // Create connection and channel
            connection = factory.newConnection();
            channel = connection.createChannel();

            // Get exchange and routing key configuration
            exchangeName = props.getOrDefault("exchangeName", "");
            routingKey = props.getOrDefault("routingKey", mqMock.getDestinationName());

            // Declare queue
            boolean durable = Boolean.parseBoolean(props.getOrDefault("durable", "true"));
            boolean exclusive = false; // Queues should not be exclusive for mocking
            boolean autoDelete = Boolean.parseBoolean(props.getOrDefault("autoDelete", "false"));
            
            channel.queueDeclare(
                mqMock.getDestinationName(),
                durable,
                exclusive,
                autoDelete,
                null
            );

            // If exchange is specified, bind queue to exchange
            if (StringUtils.isNotBlank(exchangeName)) {
                String exchangeType = props.getOrDefault("exchangeType", "direct");
                
                // Declare exchange
                channel.exchangeDeclare(exchangeName, exchangeType, durable);
                
                // Bind queue to exchange with routing key
                channel.queueBind(
                    mqMock.getDestinationName(),
                    exchangeName,
                    routingKey
                );
                
                logger.info("Queue '{}' bound to exchange '{}' with routing key '{}'", 
                    mqMock.getDestinationName(), exchangeName, routingKey);
            }

            // Set prefetch count if specified
            if (props.containsKey("prefetchCount")) {
                int prefetchCount = Integer.parseInt(props.get("prefetchCount"));
                channel.basicQos(prefetchCount);
                logger.info("Prefetch count set to: {}", prefetchCount);
            }

            logger.info("RabbitMQ connection initialized for: {}", mqMock.getDestinationName());

        } catch (MockServerException e) {
            throw e;
        } catch (Exception e) {
            throw new MockServerException("Failed to initialize RabbitMQ connection", e);
        }
    }

    /**
     * Start message consumer
     */
    public void startMessageListener(DeliverCallback deliverCallback) throws MockServerException {
        try {
            consumerTag = channel.basicConsume(
                mqMock.getDestinationName(),
                true, // auto-ack
                deliverCallback,
                consumerTag -> {
                    logger.warn("Consumer cancelled: {}", consumerTag);
                }
            );
            logger.info("RabbitMQ consumer started for: {}", mqMock.getDestinationName());
        } catch (IOException e) {
            throw new MockServerException("Failed to start RabbitMQ consumer", e);
        }
    }

    /**
     * Send a message
     */
    public void sendMessage(String messageBody, String messageKey, 
                           String contentType, Map<String, String> headers) throws MockServerException {
        try {
            AMQP.BasicProperties.Builder propsBuilder = new AMQP.BasicProperties.Builder();

            // Set content type
            if (StringUtils.isNotBlank(contentType)) {
                propsBuilder.contentType(contentType);
            }

            // Set message key as correlation ID or custom header
            if (StringUtils.isNotBlank(messageKey)) {
                propsBuilder.correlationId(messageKey);
            }

            // Add custom headers (convert to Map<String, Object>)
            if (headers != null && !headers.isEmpty()) {
                propsBuilder.headers(new java.util.HashMap<>(headers));
            }

            // Determine target exchange and routing key
            String targetExchange = StringUtils.isNotBlank(exchangeName) ? exchangeName : "";
            String targetRoutingKey = StringUtils.isNotBlank(routingKey) ? routingKey : mqMock.getDestinationName();

            // Publish message
            channel.basicPublish(
                targetExchange,
                targetRoutingKey,
                propsBuilder.build(),
                messageBody.getBytes(StandardCharsets.UTF_8)
            );

            logger.debug("Message sent to RabbitMQ: exchange={}, routingKey={}", 
                targetExchange, targetRoutingKey);

        } catch (IOException e) {
            throw new MockServerException("Failed to send RabbitMQ message", e);
        }
    }

    /**
     * Get connection
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Get channel
     */
    public Channel getChannel() {
        return channel;
    }

    /**
     * Get exchange name
     */
    public String getExchangeName() {
        return exchangeName;
    }

    /**
     * Get routing key
     */
    public String getRoutingKey() {
        return routingKey;
    }

    /**
     * Get consumer tag
     */
    public String getConsumerTag() {
        return consumerTag;
    }

    /**
     * Get MQ Mock configuration
     */
    public MQMock getMqMock() {
        return mqMock;
    }

    /**
     * Close RabbitMQ connection
     */
    @Override
    public void close() {
        logger.info("Closing RabbitMQ connection for: {}", mqMock.getDestinationName());

        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
            if (connection != null && connection.isOpen()) {
                connection.close();
            }
            logger.info("RabbitMQ connection closed successfully");
        } catch (Exception e) {
            logger.error("Error closing RabbitMQ connection", e);
        }
    }

    /**
     * Get connection type
     */
    @Override
    public String getConnectionType() {
        return "RABBITMQ";
    }
}
