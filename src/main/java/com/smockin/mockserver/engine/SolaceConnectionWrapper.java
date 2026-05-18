package com.smockin.mockserver.engine;

import com.smockin.admin.persistence.entity.MQMock;
import com.smockin.mockserver.exception.MockServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.jms.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Solace JMS Connection Wrapper
 * 
 * Supports Solace Message Router via JMS API.
 * Protocol support: smf:// (plain), smfs:// (TLS/SSL), tcp:// (compatible)
 * 
 * Configuration Options:
 * 1. JNDI-based (Recommended for production):
 *    - jndiInitialContextFactory: com.solacesystems.jndi.SolJNDIInitialContextFactory
 *    - jndiProviderUrl: smf://<host>:<port> or smfs://<host>:<port> or tcp://<host>:<port>
 *    - jndiConnectionFactoryName: /jms/cf/default
 *    - jndiDestinationName: /jms/queue/<queue-name> or /jms/topic/<topic-name>
 * 
 * 2. Direct JMS API (Simplified):
 *    - solaceHost: smf://<host>:<port> or smfs://<host>:<port>
 *    - solaceVPN: <message-vpn-name>
 *    - solaceUsername: <username>
 *    - solacePassword: <password>
 *    - solaceReconnectRetries: <number> (optional, default: 3)
 *    - solaceReconnectRetryWaitInMillis: <ms> (optional, default: 3000)
 * 
 * Protocol Details:
 * - smf://  - Solace Message Format (plain text, port 55555 default)
 * - smfs:// - Solace Message Format over TLS/SSL (port 55443 default)
 * - tcp://  - Compatible mode (auto-detected)
 */
public class SolaceConnectionWrapper implements AutoCloseable {

    private final Logger logger = LoggerFactory.getLogger(SolaceConnectionWrapper.class);

    private final MQMock mqMock;
    private Connection connection;
    private Session session;
    private Destination destination;
    private MessageConsumer messageConsumer;

    public SolaceConnectionWrapper(final MQMock mqMock) {
        this.mqMock = mqMock;
    }

    /**
     * Initialize Solace JMS connection
     */
    public void initialize() throws MockServerException {
        logger.debug("Initializing Solace JMS connection for: {}", mqMock.getDestinationName());

        try {
            Map<String, String> props = mqMock.getProperties();

            // Check if JNDI configuration is provided
            String jndiInitialContextFactory = props.get("jndiInitialContextFactory");
            String jndiProviderUrl = props.get("jndiProviderUrl");

            if (jndiInitialContextFactory != null && jndiProviderUrl != null) {
                // Use JNDI-based approach (Recommended for production)
                initializeWithJNDI(props);
            } else {
                // Use direct JMS API approach (Simplified)
                initializeWithDirectAPI(props);
            }

            // Start the connection
            connection.start();

            logger.info("Solace JMS connection initialized for: {}", mqMock.getDestinationName());

        } catch (MockServerException e) {
            throw e;
        } catch (Exception e) {
            throw new MockServerException("Failed to initialize Solace JMS connection", e);
        }
    }

    /**
     * Initialize using JNDI (Recommended for production)
     */
    private void initializeWithJNDI(Map<String, String> props) throws MockServerException {
        try {
            String jndiInitialContextFactory = props.get("jndiInitialContextFactory");
            String jndiProviderUrl = props.get("jndiProviderUrl");
            String jndiConnectionFactoryName = props.getOrDefault("jndiConnectionFactoryName", "/jms/cf/default");
            String jndiDestinationName = props.get("jndiDestinationName");
            String username = props.get("username");
            String password = props.get("password");

            // Setup JNDI environment
            Properties jndiProps = new Properties();
            jndiProps.put(javax.naming.Context.INITIAL_CONTEXT_FACTORY, jndiInitialContextFactory);
            jndiProps.put(javax.naming.Context.PROVIDER_URL, jndiProviderUrl);

            // Add authentication if provided
            if (username != null) {
                jndiProps.put(javax.naming.Context.SECURITY_PRINCIPAL, username);
            }
            if (password != null) {
                jndiProps.put(javax.naming.Context.SECURITY_CREDENTIALS, password);
            }

            // Create JNDI InitialContext
            javax.naming.InitialContext initialContext = new javax.naming.InitialContext(jndiProps);

            // Lookup ConnectionFactory
            ConnectionFactory connectionFactory = (ConnectionFactory) initialContext.lookup(jndiConnectionFactoryName);

            // Create Connection
            if (username != null && password != null) {
                connection = connectionFactory.createConnection(username, password);
            } else {
                connection = connectionFactory.createConnection();
            }

            // Create Session
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // Lookup or create Destination
            if (jndiDestinationName != null) {
                destination = (Destination) initialContext.lookup(jndiDestinationName);
            } else {
                // Create destination based on topic flag
                if (mqMock.isTopic()) {
                    destination = session.createTopic(mqMock.getDestinationName());
                } else {
                    destination = session.createQueue(mqMock.getDestinationName());
                }
            }

            initialContext.close();
            logger.info("Solace JMS connection initialized via JNDI");

        } catch (Exception e) {
            throw new MockServerException("Failed to initialize Solace JMS connection via JNDI", e);
        }
    }

    /**
     * Initialize using direct JMS API (Simplified approach)
     * Note: This requires Solace JMS client library (sol-jms)
     * 
     * Supports protocols:
     * - smf://host:port (plain text)
     * - smfs://host:port (TLS/SSL)
     * - tcp://host:port (compatible)
     */
    private void initializeWithDirectAPI(Map<String, String> props) throws MockServerException {
        try {
            String solaceHost = props.getOrDefault("solaceHost", "smf://localhost:55555");
            String solaceVPN = props.getOrDefault("solaceVPN", "default");
            String solaceUsername = props.getOrDefault("solaceUsername", "default");
            String solacePassword = props.getOrDefault("solacePassword", "default");
            int reconnectRetries = Integer.parseInt(props.getOrDefault("solaceReconnectRetries", "3"));
            int reconnectRetryWaitInMillis = Integer.parseInt(props.getOrDefault("solaceReconnectRetryWaitInMillis", "3000"));

            // Validate protocol
            if (!solaceHost.startsWith("smf://") && 
                !solaceHost.startsWith("smfs://") && 
                !solaceHost.startsWith("tcp://")) {
                throw new MockServerException(
                    "Invalid Solace host format. Must start with smf://, smfs://, or tcp://. Got: " + solaceHost);
            }

            // Detect protocol type
            String protocol;
            if (solaceHost.startsWith("smfs://")) {
                protocol = "SMF_SSL";
            } else {
                protocol = "SMF";
            }

            logger.info("Connecting to Solace at {} with protocol {}", solaceHost, protocol);

            // Try to load Solace JMS client using reflection
            try {
                // Load Solace ConnectionFactory class
                Class<?> solaceCfClass = Class.forName("com.solacesystems.jms.SolConnectionFactory");
                Object solaceCf = solaceCfClass.getDeclaredConstructor().newInstance();

                // Set connection properties using reflection
                solaceCfClass.getMethod("setHost", String.class).invoke(solaceCf, solaceHost);
                solaceCfClass.getMethod("setVPN", String.class).invoke(solaceCf, solaceVPN);
                solaceCfClass.getMethod("setUsername", String.class).invoke(solaceCf, solaceUsername);
                solaceCfClass.getMethod("setPassword", String.class).invoke(solaceCf, solacePassword);
                solaceCfClass.getMethod("setReconnectRetries", int.class).invoke(solaceCf, reconnectRetries);
                solaceCfClass.getMethod("setReconnectRetryWaitInMillis", int.class).invoke(solaceCf, reconnectRetryWaitInMillis);

                // Create connection via Solace factory
                // Note: Solace client uses javax.jms internally, need to adapt to jakarta.jms
                // Use reflection to avoid compile-time dependency issues
                jakarta.jms.Connection jmsConnection = (jakarta.jms.Connection) 
                    solaceCfClass.getMethod("createConnection").invoke(solaceCf);
                connection = jmsConnection;

                // Create Session
                session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

                // Create destination
                if (mqMock.isTopic()) {
                    destination = session.createTopic(mqMock.getDestinationName());
                } else {
                    destination = session.createQueue(mqMock.getDestinationName());
                }

                logger.info("Solace JMS connection initialized via direct API at {}", solaceHost);

            } catch (ClassNotFoundException e) {
                // Solace JMS client not available
                throw new MockServerException(
                    "Solace JMS client (sol-jms) not found. Please install it:\n" +
                    "1. Download sol-jms from https://solace.com/downloads/\n" +
                    "2. Install to local Maven:\n" +
                    "   mvn install:install-file -Dfile=sol-jms-10.20.0.jar \\\n" +
                    "   -DgroupId=com.solace.pubsub -DartifactId=sol-jms -Dversion=10.20.0 -Dpackaging=jar\n" +
                    "3. Uncomment sol-jms dependency in pom.xml\n\n" +
                    "Alternatively, use JNDI-based configuration which is recommended for production.");
            }

        } catch (MockServerException e) {
            throw e;
        } catch (Exception e) {
            throw new MockServerException("Failed to initialize Solace JMS connection via direct API", e);
        }
    }

    /**
     * Start message listener
     */
    public void startMessageListener(MessageListener messageListener) throws MockServerException {
        try {
            messageConsumer = session.createConsumer(destination);
            messageConsumer.setMessageListener(messageListener);
            logger.info("Solace message listener started for: {}", mqMock.getDestinationName());
        } catch (Exception e) {
            throw new MockServerException("Failed to start Solace message listener", e);
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
     * Set message consumer
     */
    public void setMessageConsumer(MessageConsumer consumer) {
        this.messageConsumer = consumer;
    }

    /**
     * Get MQ Mock configuration
     */
    public MQMock getMqMock() {
        return mqMock;
    }

    /**
     * Close Solace JMS connection
     */
    @Override
    public void close() {
        logger.info("Closing Solace JMS connection for: {}", mqMock.getDestinationName());

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
            logger.info("Solace JMS connection closed successfully");
        } catch (Exception e) {
            logger.error("Error closing Solace JMS connection", e);
        }
    }
}
