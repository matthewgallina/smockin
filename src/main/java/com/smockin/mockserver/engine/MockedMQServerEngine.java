package com.smockin.mockserver.engine;

import com.smockin.admin.persistence.dao.MQMockDAO;
import com.smockin.admin.persistence.dao.MQMockMessageDAO;
import com.smockin.admin.persistence.entity.MQMock;
import com.smockin.admin.persistence.entity.MQMockMessage;
import com.smockin.admin.persistence.enums.MQTypeEnum;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.mockserver.dto.MockServerState;
import com.smockin.mockserver.dto.MockedServerConfigDTO;
import com.smockin.mockserver.dto.MQMockMessageDTO;
import com.smockin.mockserver.exception.MockServerException;
import com.smockin.utils.GeneralUtils;
import jakarta.jms.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
public class MockedMQServerEngine {

    private final Logger logger = LoggerFactory.getLogger(MockedMQServerEngine.class);

    private final Object serverStateMonitor = new Object();
    private MockServerState serverState = new MockServerState(false, 0);

    // Cache for active MQ connections: key = mqMock.extId, value = MQConnectionWrapper
    private final ConcurrentHashMap<String, MQConnectionWrapper> mqConnectionsMap = new ConcurrentHashMap<>();

    // Cache for Kafka connections: key = mqMock.extId, value = KafkaConnectionWrapper
    private final ConcurrentHashMap<String, KafkaConnectionWrapper> kafkaConnectionsMap = new ConcurrentHashMap<>();

    // Cache for ActiveMQ Artemis connections: key = mqMock.extId, value = ActiveMQConnectionWrapper
    private final ConcurrentHashMap<String, ActiveMQConnectionWrapper> activeMQConnectionsMap = new ConcurrentHashMap<>();

    // Cache for RabbitMQ connections: key = mqMock.extId, value = RabbitMQConnectionWrapper
    private final ConcurrentHashMap<String, RabbitMQConnectionWrapper> rabbitMQConnectionsMap = new ConcurrentHashMap<>();

    // Cache for received messages: key = mqMock.extId, value = list of messages
    private final ConcurrentHashMap<String, MQMessageCache> mqMessageCacheMap = new ConcurrentHashMap<>();

    @Autowired
    private MQMockDAO mqMockDAO;

    @Autowired
    private MQMockMessageDAO mqMockMessageDAO;

    /**
     * Start the MQ Mock Engine with active MQ configurations
     */
    public void start(final MockedServerConfigDTO configDTO,
                      final List<MQMock> mqMocks) throws MockServerException {
        logger.debug("start called");

        synchronized (serverStateMonitor) {
            // Check if engine is already running
            if (serverState.isRunning()) {
                throw new MockServerException("MQ Mock Engine is already running");
            }

            try {
                clearAllConnections();

                if (mqMocks != null && !mqMocks.isEmpty()) {
                    for (MQMock mqMock : mqMocks) {
                        try {
                            initializeMQConnection(mqMock);
                        } catch (Exception e) {
                            logger.error("Failed to initialize MQ connection for: " + mqMock.getName(), e);
                        }
                    }
                }

                serverState.setRunning(true);
                serverState.setPort(configDTO.getPort() != null ? configDTO.getPort() : 0);

                logger.info("MQ Mock Engine started successfully with {} connections", mqConnectionsMap.size());

            } catch (Exception e) {
                logger.error("Error starting MQ Mock Engine", e);
                shutdown();
                throw new MockServerException("Error starting MQ Mock Engine", e);
            }
        }
    }

    /**
     * Get current server state
     */
    public MockServerState getCurrentState() throws MockServerException {
        synchronized (serverStateMonitor) {
            return serverState;
        }
    }

    /**
     * Shutdown the MQ Mock Engine
     */
    public void shutdown() throws MockServerException {
        logger.debug("shutdown called");

        try {
            synchronized (serverStateMonitor) {
                clearAllConnections();
                serverState.setRunning(false);
            }

            logger.info("MQ Mock Engine shutdown successfully");

        } catch (Exception e) {
            logger.error("Error shutting down MQ Mock Engine", e);
            throw new MockServerException("Error shutting down MQ Mock Engine", e);
        }
    }

    /**
     * Initialize connection to MQ (supports IBM MQ, Solace, Kafka)
     */
    private void initializeMQConnection(final MQMock mqMock) throws MockServerException {
        logger.debug("Initializing MQ connection for: {}", mqMock.getName());

        if (mqMock.getStatus() != RecordStatusEnum.ACTIVE) {
            logger.warn("MQ Mock is not active: {}", mqMock.getName());
            return;
        }

        try {
            switch (mqMock.getMqType()) {
                case JMS:
                    // JMS provider - supports ActiveMQ, IBM MQ, Solace, etc.
                    String jmsProvider = mqMock.getProperties().getOrDefault("jmsProvider", "ACTIVEMQ");
                    switch (jmsProvider.toUpperCase()) {
                        case "IBMMQ":
                            MQConnectionWrapper ibmMqConnection = createIBMMQConnection(mqMock);
                            if (ibmMqConnection != null) {
                                mqConnectionsMap.put(mqMock.getExtId(), ibmMqConnection);
                                startMessageListener(mqMock, ibmMqConnection);
                            }
                            break;
                        case "SOLACE":
                            MQConnectionWrapper solaceConnection = createSolaceJMSConnection(mqMock);
                            if (solaceConnection != null) {
                                mqConnectionsMap.put(mqMock.getExtId(), solaceConnection);
                                startMessageListener(mqMock, solaceConnection);
                            }
                            break;
                        case "ACTIVEMQ":
                            createActiveMQConnection(mqMock);
                            break;
                        case "GENERIC":
                        default:
                            MQConnectionWrapper genericConnection = createGenericJMSConnection(mqMock);
                            if (genericConnection != null) {
                                mqConnectionsMap.put(mqMock.getExtId(), genericConnection);
                                startMessageListener(mqMock, genericConnection);
                            }
                            break;
                    }
                    break;
                case KAFKA:
                    createKafkaConnection(mqMock);
                    break;
                case AMQP:
                    createRabbitMQConnection(mqMock);
                    break;
                default:
                    throw new MockServerException("Unsupported MQ type: " + mqMock.getMqType());
            }

            // Initialize message cache for all MQ types
            mqMessageCacheMap.put(mqMock.getExtId(), new MQMessageCache());
            logger.info("MQ connection initialized for: {}", mqMock.getName());

        } catch (Exception e) {
            logger.error("Failed to initialize MQ connection for: " + mqMock.getName(), e);
            throw new MockServerException("Failed to initialize MQ connection for: " + mqMock.getName(), e);
        }
    }

    /**
     * Create IBM MQ Connection using Reflection (Jakarta EE Compatible)
     * 
     * Supports both javax.jms (IBM MQ 9.3.x) and jakarta.jms (IBM MQ 9.4+)
     * Uses reflection to avoid compile-time dependency issues.
     */
    private MQConnectionWrapper createIBMMQConnection(final MQMock mqMock) throws MockServerException {
        logger.debug("Creating IBM MQ connection for: {}", mqMock.getDestinationName());

        try {
            Map<String, String> props = mqMock.getProperties();

            // IBM MQ Connection Properties
            String queueManager = props.getOrDefault("queueManager", "QM1");
            String channel = props.getOrDefault("channel", "SYSTEM.DEF.SVRCONN");
            String connectionName = props.getOrDefault("connectionName", "localhost(1414)");
            int ccsid = Integer.parseInt(props.getOrDefault("ccsid", "1208"));
            String username = props.get("username");
            String password = props.get("password");

            // Try Jakarta EE compatible IBM MQ client first (9.4+)
            try {
                Class<?> mqCfClass = Class.forName("com.ibm.mq.jakarta.jms.MQConnectionFactory");
                Object cf = mqCfClass.getDeclaredConstructor().newInstance();

                // Set connection properties
                mqCfClass.getMethod("setTransportType", int.class).invoke(cf, 1); // WMQ_CM_CLIENT
                mqCfClass.getMethod("setQueueManager", String.class).invoke(cf, queueManager);
                mqCfClass.getMethod("setChannel", String.class).invoke(cf, channel);
                mqCfClass.getMethod("setConnectionNameList", String.class).invoke(cf, connectionName);
                mqCfClass.getMethod("setCCSID", int.class).invoke(cf, ccsid);

                // Create connection
                Connection connection;
                if (StringUtils.isNotBlank(username)) {
                    connection = (Connection) mqCfClass.getMethod("createConnection", String.class, String.class)
                        .invoke(cf, username, password);
                } else {
                    connection = (Connection) mqCfClass.getMethod("createConnection").invoke(cf);
                }

                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                Destination destination = mqMock.isTopic() ?
                    session.createTopic(mqMock.getDestinationName()) :
                    session.createQueue(mqMock.getDestinationName());

                connection.start();

                logger.info("IBM MQ (Jakarta) connection created for: {}", mqMock.getDestinationName());
                return new MQConnectionWrapper(connection, session, destination, mqMock);

            } catch (ClassNotFoundException e) {
                // Jakarta client not available, try standard client with adapter
                logger.debug("IBM MQ Jakarta client not available, trying standard client");
                
                Class<?> mqCfClass = Class.forName("com.ibm.mq.jms.MQConnectionFactory");
                Object cf = mqCfClass.getDeclaredConstructor().newInstance();

                mqCfClass.getMethod("setTransportType", int.class).invoke(cf, 1);
                mqCfClass.getMethod("setQueueManager", String.class).invoke(cf, queueManager);
                mqCfClass.getMethod("setChannel", String.class).invoke(cf, channel);
                mqCfClass.getMethod("setConnectionNameList", String.class).invoke(cf, connectionName);
                mqCfClass.getMethod("setCCSID", int.class).invoke(cf, ccsid);

                // Standard client returns javax.jms.Connection, need to adapt to jakarta.jms
                throw new MockServerException(
                    "IBM MQ 9.3.x (javax.jms) detected. For Spring Boot 3.x compatibility:\n" +
                    "1. Upgrade to IBM MQ 9.4+ Jakarta client\n" +
                    "2. Or use a javax-to-jakarta JMS adapter library\n\n" +
                    "Download IBM MQ Jakarta client from IBM Fix Central.");
            }

        } catch (MockServerException e) {
            throw e;
        } catch (Exception e) {
            throw new MockServerException("Failed to create IBM MQ connection: " + e.getMessage(), e);
        }
    }

    /**
     * Create Solace JMS Connection
     * Priority: HIGH
     */
    private MQConnectionWrapper createSolaceJMSConnection(final MQMock mqMock) throws MockServerException {
        logger.debug("Creating Solace JMS connection for: {}", mqMock.getDestinationName());

        try {
            Map<String, String> props = mqMock.getProperties();

            String solaceHost = props.getOrDefault("solaceHost", "tcp://localhost:55555");
            String solaceVPN = props.getOrDefault("solaceVPN", "default");
            String solaceUsername = props.getOrDefault("solaceUsername", "default");
            String solacePassword = props.getOrDefault("solacePassword", "default");

            // Solace JNDI InitialContextFactory
            // Note: Solace requires JNDI configuration or direct JMS API usage
            // This is a simplified implementation - production should use proper JNDI or Solace JMS API
            
            throw new MockServerException("Solace JMS integration requires external JNDI configuration. " +
                "Please configure Solace JNDI properties and retry.");

        } catch (MockServerException e) {
            throw e;
        } catch (Exception e) {
            throw new MockServerException("Failed to create Solace JMS connection", e);
        }
    }

    /**
     * Create Kafka Connection using native Kafka API
     * Priority: MEDIUM
     */
    private void createKafkaConnection(final MQMock mqMock) throws MockServerException {
        logger.debug("Creating Kafka connection for: {}", mqMock.getDestinationName());

        try {
            KafkaConnectionWrapper kafkaWrapper = new KafkaConnectionWrapper(mqMock);
            kafkaWrapper.initialize();
            
            // Store in Kafka connections map
            kafkaConnectionsMap.put(mqMock.getExtId(), kafkaWrapper);
            
            // Start message listener
            kafkaWrapper.startMessageListener(record -> {
                try {
                    handleKafkaMessageReceived(mqMock, record);
                } catch (Exception e) {
                    logger.error("Error handling Kafka message for: " + mqMock.getName(), e);
                }
            });
            
            logger.info("Kafka connection created for topic: {}", mqMock.getDestinationName());
            
        } catch (Exception e) {
            throw new MockServerException("Failed to create Kafka connection", e);
        }
    }

    /**
     * Create ActiveMQ Artemis Connection
     * Uses ActiveMQ Artemis native JMS client.
     */
    private void createActiveMQConnection(final MQMock mqMock) throws MockServerException {
        logger.debug("Creating ActiveMQ Artemis connection for: {}", mqMock.getDestinationName());

        try {
            ActiveMQConnectionWrapper activeMQWrapper = new ActiveMQConnectionWrapper(mqMock);
            activeMQWrapper.initialize();
            
            // Store wrapper reference
            activeMQConnectionsMap.put(mqMock.getExtId(), activeMQWrapper);
            
            // Start message listener
            activeMQWrapper.startMessageListener(message -> {
                try {
                    handleMessageReceived(mqMock, message);
                } catch (Exception e) {
                    logger.error("Error handling ActiveMQ message for: " + mqMock.getName(), e);
                }
            });
            
            logger.info("ActiveMQ Artemis connection created for: {}", mqMock.getDestinationName());
            
            // Initialize message cache
            mqMessageCacheMap.put(mqMock.getExtId(), new MQMessageCache());
            
        } catch (Exception e) {
            throw new MockServerException("Failed to create ActiveMQ Artemis connection", e);
        }
    }

    /**
     * Create RabbitMQ AMQP Connection
     * Uses RabbitMQ Java Client (AMQP 0-9-1).
     */
    private void createRabbitMQConnection(final MQMock mqMock) throws MockServerException {
        logger.debug("Creating RabbitMQ connection for: {}", mqMock.getDestinationName());

        try {
            RabbitMQConnectionWrapper rabbitMQWrapper = new RabbitMQConnectionWrapper(mqMock);
            rabbitMQWrapper.initialize();
            
            // Store wrapper reference
            rabbitMQConnectionsMap.put(mqMock.getExtId(), rabbitMQWrapper);
            
            // Start message listener
            com.rabbitmq.client.DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                try {
                    handleRabbitMQMessageReceived(mqMock, delivery);
                } catch (Exception e) {
                    logger.error("Error handling RabbitMQ message for: " + mqMock.getName(), e);
                }
            };
            rabbitMQWrapper.startMessageListener(deliverCallback);
            
            logger.info("RabbitMQ connection created for: {}", mqMock.getDestinationName());
            
            // Initialize message cache
            mqMessageCacheMap.put(mqMock.getExtId(), new MQMessageCache());
            
        } catch (Exception e) {
            throw new MockServerException("Failed to create RabbitMQ connection", e);
        }
    }

    /**
     * Create Generic JMS Connection (for other JMS providers)
     */
    private MQConnectionWrapper createGenericJMSConnection(final MQMock mqMock) throws MockServerException {
        logger.debug("Creating Generic JMS connection for: {}", mqMock.getDestinationName());

        try {
            Map<String, String> props = mqMock.getProperties();

            String brokerURL = mqMock.getBrokerUrl();
            if (StringUtils.isBlank(brokerURL)) {
                throw new MockServerException("brokerUrl is required for Generic JMS connection");
            }

            // Use ActiveMQ Artemis as default JMS provider
            org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory cf = 
                new org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory(brokerURL);

            String username = props.get("username");
            String password = props.get("password");

            Connection connection;
            if (StringUtils.isNotBlank(username)) {
                connection = cf.createConnection(username, password);
            } else {
                connection = cf.createConnection();
            }

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            Destination destination;
            if (mqMock.isTopic()) {
                destination = session.createTopic(mqMock.getDestinationName());
            } else {
                destination = session.createQueue(mqMock.getDestinationName());
            }

            connection.start();

            logger.info("Generic JMS connection created for: {}", mqMock.getDestinationName());
            return new MQConnectionWrapper(connection, session, destination, mqMock);

        } catch (MockServerException e) {
            throw e;
        } catch (Exception e) {
            throw new MockServerException("Failed to create Generic JMS connection", e);
        }
    }

    /**
     * Start message listener for a MQ connection
     */
    private void startMessageListener(final MQMock mqMock, 
                                      final MQConnectionWrapper connectionWrapper) {
        logger.debug("Starting message listener for: {}", mqMock.getName());

        try {
            MessageConsumer consumer = connectionWrapper.getSession()
                .createConsumer(connectionWrapper.getDestination());

            consumer.setMessageListener(message -> {
                try {
                    handleMessageReceived(mqMock, message);
                } catch (Exception e) {
                    logger.error("Error handling message for: " + mqMock.getName(), e);
                }
            });

            connectionWrapper.setMessageConsumer(consumer);
            logger.info("Message listener started for: {}", mqMock.getName());

        } catch (Exception e) {
            logger.error("Failed to start message listener for: " + mqMock.getName(), e);
        }
    }

    /**
     * Handle received message from JMS
     */
    private void handleMessageReceived(final MQMock mqMock, final Message message) {
        logger.debug("Message received for: {}", mqMock.getName());

        try {
            String messageBody = null;
            String contentType = null;
            String messageKey = null;

            if (message instanceof TextMessage) {
                messageBody = ((TextMessage) message).getText();
                contentType = "text/plain";
            } else if (message instanceof BytesMessage) {
                BytesMessage bytesMessage = (BytesMessage) message;
                byte[] bytes = new byte[(int) bytesMessage.getBodyLength()];
                bytesMessage.readBytes(bytes);
                messageBody = new String(bytes);
                contentType = "application/octet-stream";
            } else if (message instanceof ObjectMessage) {
                messageBody = ((ObjectMessage) message).getObject().toString();
                contentType = "application/x-java-serialized-object";
            } else if (message instanceof MapMessage) {
                messageBody = message.toString();
                contentType = "application/x-map";
            }

            // Get message properties
            messageKey = message.getJMSMessageID();
            String correlationId = message.getJMSCorrelationID();
            String producerId = null;

            // Build headers map
            Map<String, String> headers = new ConcurrentHashMap<>();
            headers.put("JMSMessageID", message.getJMSMessageID());
            headers.put("JMSTimestamp", String.valueOf(message.getJMSTimestamp()));
            headers.put("JMSDeliveryMode", String.valueOf(message.getJMSDeliveryMode()));
            headers.put("JMSPriority", String.valueOf(message.getJMSPriority()));
            if (StringUtils.isNotBlank(correlationId)) {
                headers.put("JMSCorrelationID", correlationId);
            }

            // Create message DTO
            MQMockMessageDTO messageDTO = new MQMockMessageDTO(
                GeneralUtils.generateUUID(),
                messageKey,
                messageBody,
                contentType,
                headers,
                GeneralUtils.getCurrentDate(),
                producerId,
                mqMock.getExtId()
            );

            // Add to cache
            addToMessageCache(mqMock.getExtId(), messageDTO);

            // Save to database if enabled
            if (mqMock.isSaveMessages()) {
                saveMessageToDatabase(mqMock, messageDTO);
            }

            logger.info("Message processed for: {}, MessageID: {}", mqMock.getName(), messageKey);

        } catch (Exception e) {
            logger.error("Error handling message for: " + mqMock.getName(), e);
        }
    }

    /**
     * Handle received message from Kafka
     */
    private void handleKafkaMessageReceived(final MQMock mqMock, 
                                            org.apache.kafka.clients.consumer.ConsumerRecord<String, String> record) {
        logger.debug("Kafka message received for: {}", mqMock.getName());

        try {
            String messageBody = record.value();
            String messageKey = record.key();
            String contentType = "text/plain";

            // Extract headers
            Map<String, String> headers = new ConcurrentHashMap<>();
            headers.put("kafka-topic", record.topic());
            headers.put("kafka-partition", String.valueOf(record.partition()));
            headers.put("kafka-offset", String.valueOf(record.offset()));
            headers.put("kafka-timestamp", String.valueOf(record.timestamp()));

            // Extract content-type header if present
            for (org.apache.kafka.common.header.Header header : record.headers()) {
                headers.put(header.key(), new String(header.value()));
                if ("content-type".equals(header.key())) {
                    contentType = new String(header.value());
                }
            }

            // Create message DTO
            MQMockMessageDTO messageDTO = new MQMockMessageDTO(
                GeneralUtils.generateUUID(),
                messageKey,
                messageBody,
                contentType,
                headers,
                GeneralUtils.getCurrentDate(),
                null, // Kafka doesn't have producer ID in the same way
                mqMock.getExtId()
            );

            // Add to cache
            addToMessageCache(mqMock.getExtId(), messageDTO);

            // Save to database if enabled
            if (mqMock.isSaveMessages()) {
                saveMessageToDatabase(mqMock, messageDTO);
            }

            logger.info("Kafka message processed for: {}, Offset: {}", mqMock.getName(), record.offset());

        } catch (Exception e) {
            logger.error("Error handling Kafka message for: " + mqMock.getName(), e);
        }
    }

    /**
     * Handle received message from RabbitMQ
     */
    private void handleRabbitMQMessageReceived(final MQMock mqMock,
                                               com.rabbitmq.client.Delivery delivery) {
        logger.debug("RabbitMQ message received for: {}", mqMock.getName());

        try {
            String messageBody = new String(delivery.getBody(), java.nio.charset.StandardCharsets.UTF_8);
            String messageKey = null;
            String contentType = "text/plain";

            // Extract properties
            com.rabbitmq.client.AMQP.BasicProperties props = delivery.getProperties();
            Map<String, String> headers = new ConcurrentHashMap<>();
            
            if (props != null) {
                if (props.getCorrelationId() != null) {
                    messageKey = props.getCorrelationId();
                    headers.put("correlationId", messageKey);
                }
                if (props.getContentType() != null) {
                    contentType = props.getContentType();
                }
                if (props.getMessageId() != null) {
                    headers.put("messageId", props.getMessageId());
                }
                if (props.getDeliveryMode() != null) {
                    headers.put("deliveryMode", props.getDeliveryMode().toString());
                }
                if (props.getHeaders() != null) {
                    for (Map.Entry<String, Object> entry : props.getHeaders().entrySet()) {
                        headers.put(entry.getKey(), entry.getValue().toString());
                    }
                }
            }

            headers.put("rabbitmq-queue", mqMock.getDestinationName());
            headers.put("rabbitmq-deliveryTag", String.valueOf(delivery.getEnvelope().getDeliveryTag()));
            headers.put("rabbitmq-exchange", delivery.getEnvelope().getExchange());
            headers.put("rabbitmq-routingKey", delivery.getEnvelope().getRoutingKey());

            // Create message DTO
            MQMockMessageDTO messageDTO = new MQMockMessageDTO(
                GeneralUtils.generateUUID(),
                messageKey,
                messageBody,
                contentType,
                headers,
                GeneralUtils.getCurrentDate(),
                null,
                mqMock.getExtId()
            );

            // Add to cache
            addToMessageCache(mqMock.getExtId(), messageDTO);

            // Save to database if enabled
            if (mqMock.isSaveMessages()) {
                saveMessageToDatabase(mqMock, messageDTO);
            }

            logger.info("RabbitMQ message processed for: {}, DeliveryTag: {}", 
                       mqMock.getName(), delivery.getEnvelope().getDeliveryTag());

        } catch (Exception e) {
            logger.error("Error handling RabbitMQ message for: " + mqMock.getName(), e);
        }
    }

    /**
     * Send message to MQ
     */
    public void sendMessage(final String mqMockExtId, 
                           final String messageBody,
                           final String messageKey,
                           final String contentType,
                           final Map<String, String> headers) throws MockServerException {
        logger.debug("Sending message to MQ Mock: {}", mqMockExtId);

        // Try JMS connection first (IBM MQ, Solace, Generic JMS)
        MQConnectionWrapper jmsConnection = mqConnectionsMap.get(mqMockExtId);
        if (jmsConnection != null) {
            sendJmsMessage(jmsConnection, messageBody, messageKey, contentType, headers);
            return;
        }

        // Try Kafka connection
        KafkaConnectionWrapper kafkaConnection = kafkaConnectionsMap.get(mqMockExtId);
        if (kafkaConnection != null) {
            sendKafkaMessage(kafkaConnection, messageBody, messageKey, contentType, headers);
            return;
        }

        // Try ActiveMQ Artemis connection
        ActiveMQConnectionWrapper activeMQConnection = activeMQConnectionsMap.get(mqMockExtId);
        if (activeMQConnection != null) {
            activeMQConnection.sendMessage(messageBody, messageKey, contentType, headers);
            return;
        }

        // Try RabbitMQ connection
        RabbitMQConnectionWrapper rabbitMQConnection = rabbitMQConnectionsMap.get(mqMockExtId);
        if (rabbitMQConnection != null) {
            rabbitMQConnection.sendMessage(messageBody, messageKey, contentType, headers);
            return;
        }

        throw new MockServerException("MQ Mock not found or not started: " + mqMockExtId);
    }

    /**
     * Send JMS message
     */
    private void sendJmsMessage(MQConnectionWrapper connectionWrapper,
                                String messageBody,
                                String messageKey,
                                String contentType,
                                Map<String, String> headers) throws MockServerException {
        try {
            Session session = connectionWrapper.getSession();
            MessageProducer producer = session.createProducer(connectionWrapper.getDestination());

            Message message = session.createTextMessage(messageBody);

            // Set message properties
            if (StringUtils.isNotBlank(messageKey)) {
                message.setJMSMessageID(messageKey);
            }

            if (headers != null && !headers.isEmpty()) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    message.setStringProperty(entry.getKey(), entry.getValue());
                }
            }

            producer.send(message);
            logger.info("JMS Message sent successfully");

        } catch (Exception e) {
            logger.error("Failed to send JMS message", e);
            throw new MockServerException("Failed to send JMS message", e);
        }
    }

    /**
     * Send Kafka message
     */
    private void sendKafkaMessage(KafkaConnectionWrapper kafkaConnection,
                                  String messageBody,
                                  String messageKey,
                                  String contentType,
                                  Map<String, String> headers) throws MockServerException {
        try {
            java.util.concurrent.CompletableFuture<org.apache.kafka.clients.producer.RecordMetadata> future = 
                kafkaConnection.sendMessage(messageKey, messageBody, contentType, headers);
            
            // Wait for send to complete (with timeout)
            future.get(30, java.util.concurrent.TimeUnit.SECONDS);
            kafkaConnection.flush();
            logger.info("Kafka Message sent successfully");

        } catch (Exception e) {
            logger.error("Failed to send Kafka message", e);
            throw new MockServerException("Failed to send Kafka message", e);
        }
    }

    /**
     * Add message to cache
     */
    private void addToMessageCache(final String mqMockExtId, final MQMockMessageDTO messageDTO) {
        MQMessageCache cache = mqMessageCacheMap.get(mqMockExtId);
        if (cache != null) {
            cache.addMessage(messageDTO);
        }
    }

    /**
     * Save message to database
     */
    private void saveMessageToDatabase(final MQMock mqMock, final MQMockMessageDTO messageDTO) {
        try {
            String headersJson = GeneralUtils.serialiseJson(messageDTO.getHeaders());

            MQMockMessage mqMessage = new MQMockMessage(
                messageDTO.getMessageKey(),
                messageDTO.getMessageBody(),
                messageDTO.getContentType(),
                headersJson,
                mqMock
            );

            mqMockMessageDAO.save(mqMessage);
            logger.debug("Message saved to database for: {}", mqMock.getName());

        } catch (Exception e) {
            logger.error("Failed to save message to database for: " + mqMock.getName(), e);
        }
    }

    /**
     * Get messages from cache
     */
    public List<MQMockMessageDTO> getMessagesFromCache(final String mqMockExtId) {
        MQMessageCache cache = mqMessageCacheMap.get(mqMockExtId);
        if (cache != null) {
            return cache.getMessages();
        }
        return List.of();
    }

    /**
     * Clear all connections
     */
    private void clearAllConnections() {
        logger.debug("Clearing all MQ connections");

        // Close JMS connections (IBM MQ, Solace, Generic JMS)
        for (Map.Entry<String, MQConnectionWrapper> entry : mqConnectionsMap.entrySet()) {
            try {
                entry.getValue().close();
            } catch (Exception e) {
                logger.error("Error closing MQ connection", e);
            }
        }
        mqConnectionsMap.clear();

        // Close Kafka connections
        for (Map.Entry<String, KafkaConnectionWrapper> entry : kafkaConnectionsMap.entrySet()) {
            try {
                entry.getValue().close();
            } catch (Exception e) {
                logger.error("Error closing Kafka connection", e);
            }
        }
        kafkaConnectionsMap.clear();

        // Close ActiveMQ connections
        for (Map.Entry<String, ActiveMQConnectionWrapper> entry : activeMQConnectionsMap.entrySet()) {
            try {
                entry.getValue().close();
            } catch (Exception e) {
                logger.error("Error closing ActiveMQ connection", e);
            }
        }
        activeMQConnectionsMap.clear();

        // Close RabbitMQ connections
        for (Map.Entry<String, RabbitMQConnectionWrapper> entry : rabbitMQConnectionsMap.entrySet()) {
            try {
                entry.getValue().close();
            } catch (Exception e) {
                logger.error("Error closing RabbitMQ connection", e);
            }
        }
        rabbitMQConnectionsMap.clear();

        mqMessageCacheMap.clear();
    }

    /**
     * Add a new MQ Mock connection
     */
    public void addMQMock(final MQMock mqMock) throws MockServerException {
        initializeMQConnection(mqMock);
    }

    /**
     * Remove a MQ Mock connection
     */
    public void removeMQMock(final String mqMockExtId) {
        MQConnectionWrapper connectionWrapper = mqConnectionsMap.remove(mqMockExtId);
        if (connectionWrapper != null) {
            connectionWrapper.close();
        }
        mqMessageCacheMap.remove(mqMockExtId);
    }

    /**
     * Get connection count
     */
    public int getConnectionCount() {
        return mqConnectionsMap.size();
    }

    /**
     * MQ Connection Wrapper
     */
    static class MQConnectionWrapper implements IMQConnectionWrapper {
        private Connection connection;
        private Session session;
        private Destination destination;
        private MessageConsumer messageConsumer;
        private MQMock mqMock;

        public MQConnectionWrapper(Connection connection, Session session, 
                                  Destination destination, MQMock mqMock) {
            this.connection = connection;
            this.session = session;
            this.destination = destination;
            this.mqMock = mqMock;
        }

        public Connection getConnection() { return connection; }
        public Session getSession() { return session; }
        public Destination getDestination() { return destination; }
        public MessageConsumer getMessageConsumer() { return messageConsumer; }
        public void setMessageConsumer(MessageConsumer consumer) { this.messageConsumer = consumer; }
        public MQMock getMqMock() { return mqMock; }

        public void close() {
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
            } catch (Exception e) {
                // Log but don't throw
            }
        }

        @Override
        public String getConnectionType() {
            return "JMS";
        }
    }

    /**
     * MQ Message Cache
     */
    private static class MQMessageCache {
        private final List<MQMockMessageDTO> messages = new java.util.ArrayList<>();
        private static final int MAX_CACHE_SIZE = 10000;

        public synchronized void addMessage(MQMockMessageDTO message) {
            if (messages.size() >= MAX_CACHE_SIZE) {
                messages.remove(0); // Remove oldest
            }
            messages.add(message);
        }

        public synchronized List<MQMockMessageDTO> getMessages() {
            return new java.util.ArrayList<>(messages);
        }

        public synchronized void clear() {
            messages.clear();
        }
    }
}
