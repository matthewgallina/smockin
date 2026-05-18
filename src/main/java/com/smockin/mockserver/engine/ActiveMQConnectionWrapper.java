package com.smockin.mockserver.engine;

import com.smockin.admin.persistence.entity.MQMock;
import com.smockin.mockserver.exception.MockServerException;
import jakarta.jms.*;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * ActiveMQ Artemis Connection Wrapper
 * 
 * Supports Apache ActiveMQ Artemis via JMS 2.0 API.
 * ActiveMQ is the most popular open-source JMS provider.
 * 
 * Configuration Options:
 * - brokerUrl: tcp://localhost:61616 (required)
 * - username: optional
 * - password: optional
 * - clientId: optional (for durable subscriptions)
 * - consumerWindowSize: optional (default: -1, disables prefetch)
 * - cacheLargeMessagesClient: optional (default: true)
 * 
 * Protocol Support:
 * - tcp:// - TCP transport
 * - vm:// - In-VM transport (embedded)
 * - http:// - HTTP transport
 * - https:// - HTTPS transport
 */
public class ActiveMQConnectionWrapper implements IMQConnectionWrapper {

    private final Logger logger = LoggerFactory.getLogger(ActiveMQConnectionWrapper.class);

    private final MQMock mqMock;
    private Connection connection;
    private Session session;
    private Destination destination;
    private MessageConsumer messageConsumer;

    public ActiveMQConnectionWrapper(final MQMock mqMock) {
        this.mqMock = mqMock;
    }

    /**
     * Initialize ActiveMQ Artemis connection
     */
    public void initialize() throws MockServerException {
        logger.debug("Initializing ActiveMQ Artemis connection for: {}", mqMock.getDestinationName());

        try {
            Map<String, String> props = mqMock.getProperties();

            // Get broker URL
            String brokerURL = props.getOrDefault("brokerUrl", "tcp://localhost:61616");
            
            // Validate broker URL
            if (!brokerURL.startsWith("tcp://") && 
                !brokerURL.startsWith("vm://") && 
                !brokerURL.startsWith("http://") && 
                !brokerURL.startsWith("https://")) {
                throw new MockServerException(
                    "Invalid ActiveMQ broker URL. Must start with tcp://, vm://, http://, or https://. Got: " + brokerURL);
            }

            logger.info("Connecting to ActiveMQ Artemis at: {}", brokerURL);

            // Create ConnectionFactory
            ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory(brokerURL);

            // Optional: Configure connection properties
            if (props.containsKey("consumerWindowSize")) {
                cf.setConsumerWindowSize(Integer.parseInt(props.get("consumerWindowSize")));
            }
            if (props.containsKey("cacheLargeMessagesClient")) {
                cf.setCacheLargeMessagesClient(Boolean.parseBoolean(props.get("cacheLargeMessagesClient")));
            }

            // Create connection
            String username = props.get("username");
            String password = props.get("password");

            Connection conn;
            if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
                conn = cf.createConnection(username, password);
            } else {
                conn = cf.createConnection();
            }

            // Set client ID if provided (for durable subscriptions)
            if (props.containsKey("clientId")) {
                conn.setClientID(props.get("clientId"));
                logger.info("Client ID set: {}", props.get("clientId"));
            }

            // Create session
            Session sess = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // Create destination
            Destination dest;
            if (mqMock.isTopic()) {
                dest = sess.createTopic(mqMock.getDestinationName());
            } else {
                dest = sess.createQueue(mqMock.getDestinationName());
            }

            // Start connection
            conn.start();

            // Store references
            this.connection = conn;
            this.session = sess;
            this.destination = dest;

            logger.info("ActiveMQ Artemis connection initialized for: {}", mqMock.getDestinationName());

        } catch (MockServerException e) {
            throw e;
        } catch (Exception e) {
            throw new MockServerException("Failed to initialize ActiveMQ Artemis connection", e);
        }
    }

    /**
     * Start message listener
     */
    public void startMessageListener(MessageListener messageListener) throws MockServerException {
        try {
            messageConsumer = session.createConsumer(destination);
            messageConsumer.setMessageListener(messageListener);
            logger.info("ActiveMQ message listener started for: {}", mqMock.getDestinationName());
        } catch (Exception e) {
            throw new MockServerException("Failed to start ActiveMQ message listener", e);
        }
    }

    /**
     * Send a message
     */
    public void sendMessage(String messageBody, String messageKey, 
                           String contentType, Map<String, String> headers) throws MockServerException {
        try {
            MessageProducer producer = session.createProducer(destination);
            
            TextMessage message = session.createTextMessage(messageBody);
            
            if (StringUtils.isNotBlank(messageKey)) {
                message.setStringProperty("JMSXGroupID", messageKey);
            }
            
            if (StringUtils.isNotBlank(contentType)) {
                message.setStringProperty("contentType", contentType);
            }
            
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    message.setStringProperty(entry.getKey(), entry.getValue());
                }
            }
            
            producer.send(message);
            producer.close();
            
            logger.debug("Message sent to ActiveMQ: {}", mqMock.getDestinationName());
            
        } catch (Exception e) {
            throw new MockServerException("Failed to send ActiveMQ message", e);
        }
    }

    /**
     * Get connection
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Get session
     */
    public Session getSession() {
        return session;
    }

    /**
     * Get destination
     */
    public Destination getDestination() {
        return destination;
    }

    /**
     * Get message consumer
     */
    public MessageConsumer getMessageConsumer() {
        return messageConsumer;
    }

    /**
     * Get MQ Mock configuration
     */
    public MQMock getMqMock() {
        return mqMock;
    }

    /**
     * Close ActiveMQ connection
     */
    @Override
    public void close() {
        logger.info("Closing ActiveMQ Artemis connection for: {}", mqMock.getDestinationName());

        try {
            if (messageConsumer != null) {
                messageConsumer.close();
            }
            if (session != null) {
                session.close();
            }
            if (connection != null) {
                connection.close();
            }
            logger.info("ActiveMQ Artemis connection closed successfully");
        } catch (Exception e) {
            logger.error("Error closing ActiveMQ Artemis connection", e);
        }
    }

    /**
     * Get connection type
     */
    @Override
    public String getConnectionType() {
        return "ACTIVEMQ";
    }
}
