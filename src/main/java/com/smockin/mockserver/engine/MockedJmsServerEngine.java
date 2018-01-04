package com.smockin.mockserver.engine;

import com.smockin.admin.persistence.dao.JmsMockDAO;
import com.smockin.admin.persistence.entity.JmsMock;
import com.smockin.admin.persistence.enums.JmsMockTypeEnum;
import com.smockin.mockserver.dto.MockServerState;
import com.smockin.mockserver.dto.MockedServerConfigDTO;
import com.smockin.mockserver.exception.MockServerException;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.command.ActiveMQDestination;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.jms.*;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by mgallina.
 */
@Service
public class MockedJmsServerEngine implements MockServerEngine<MockedServerConfigDTO, List<JmsMock>> {

    private final Logger logger = LoggerFactory.getLogger(MockedJmsServerEngine.class);

    @Autowired
    private JmsMockDAO jmsQueueMockDAO;

    private BrokerService broker = null;
    private ActiveMQConnectionFactory connectionFactory = null; // NOTE this is thread safe
    private final Object monitor = new Object();
    private MockServerState serverState = new MockServerState(false, 0);

    @Override
    public void start(final MockedServerConfigDTO config, final List<JmsMock> data) throws MockServerException {

        // Invoke all lazily loaded data and detach entity.
        invokeAndDetachData(data);

        // Build JMS broker
        initServerConfig(config);

        // Define JMS queue and topic destinations
        buildDestinations(data);

        // Start JMS Broker
        initServer(config.getPort());

    }

    public MockServerState getCurrentState() throws MockServerException {
        synchronized (monitor) {
            return serverState;
        }
    }

    @Override
    public void shutdown() throws MockServerException {

        try {

            synchronized (monitor) {

                connectionFactory = null;

                if (broker != null)
                    broker.stop();

                serverState.setRunning(false);
            }

        } catch (Throwable ex) {
            throw new MockServerException(ex);
        }

    }

    public void clearQueue(final String queueName) {

        ActiveMQConnection connection = null;

        try {

            connection = (ActiveMQConnection) connectionFactory.createConnection();
            connection.destroyDestination(new ActiveMQQueue(queueName));

        } catch (JMSException ex) {
            logger.error("clearing all messages on queue " + queueName, ex);
        } finally {

            if (connection != null) {
                try {
                    connection.close();
                } catch (JMSException ex) {
                    logger.error("Closing JMS connection", ex);
                }
            }

        }

    }

    public void sendTextMessageToQueue(final String queueName, final String textBody, final long timeToLive) {

        Connection connection = null;
        Session session = null;
        MessageProducer producer = null;

        try {

            if (!getCurrentState().isRunning()) {
                return;
            }

            connection = connectionFactory.createConnection();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            producer = session.createProducer(session.createQueue(queueName));
            producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
            producer.send(session.createTextMessage(textBody));
            producer.setTimeToLive(timeToLive);

        } catch (MockServerException | JMSException ex) {
            logger.error("Pushing message to queue " + queueName, ex);
        } finally {

            if (producer != null) {
                try {
                    producer.close();
                } catch (JMSException ex) {
                    logger.error("Closing JMS producer", ex);
                }
            }

            if (session != null) {
                try {
                    session.close();
                } catch (JMSException ex) {
                    logger.error("Closing JMS session", ex);
                }
            }

            if (connection != null) {
                try {
                    connection.close();
                } catch (JMSException ex) {
                    logger.error("Closing JMS connection", ex);
                }
            }

        }

    }

    public void broadcastTextMessageToTopic(final String topicName, final String textBody) {

        Connection connection = null;
        Session session = null;
        MessageProducer producer = null;

        try {

            if (!getCurrentState().isRunning()) {
                return;
            }

            connection = connectionFactory.createConnection();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            producer = session.createProducer(session.createTopic(topicName));
            producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
            producer.send(session.createTextMessage(textBody));

        } catch (MockServerException | JMSException ex) {
            logger.error("Pushing message to topic " + topicName, ex);
        } finally {

            if (producer != null) {
                try {
                    producer.close();
                } catch (JMSException ex) {
                    logger.error("Closing JMS topic producer", ex);
                }
            }

            if (session != null) {
                try {
                    session.close();
                } catch (JMSException ex) {
                    logger.error("Closing JMS topic session", ex);
                }
            }

            if (connection != null) {
                try {
                    connection.close();
                } catch (JMSException ex) {
                    logger.error("Closing JMS topic connection", ex);
                }
            }

        }

    }


    void initServerConfig(final MockedServerConfigDTO config) throws MockServerException {
        logger.debug("initServerConfig called");

        try {

            // Configure the MQ broker
            synchronized (monitor) {
                broker = new BrokerService();
                broker.setPersistent(false);
                broker.setUseJmx(false);
                broker.addConnector(config.getNativeProperties().get("BROKER_URL") + config.getPort());
            }

            connectionFactory = new ActiveMQConnectionFactory(config.getNativeProperties().get("BROKER_URL") + config.getPort());
            connectionFactory.setMaxThreadPoolSize(config.getMaxThreads());
            connectionFactory.setRejectedTaskHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        } catch (Throwable ex) {
            throw new MockServerException(ex);
        }

    }

    void initServer(final int port) throws MockServerException {
        logger.debug("initServer called");

        try {

            synchronized (monitor) {
                broker.start();

                serverState.setRunning(true);
                serverState.setPort(port);
            }

        } catch (Throwable ex) {
            throw new MockServerException(ex);
        }

    }

    @Transactional
    void invokeAndDetachData(final List<JmsMock> mocks) {

        for (JmsMock mock : mocks) {

            // Important!
            // Detach all JPA entity beans from EntityManager Context, so they can be
            // continually accessed again here as a simple data bean
            // within each request to the mocked JMS endpoint.
            jmsQueueMockDAO.detach(mock);
        }

    }

    // Expects JmsQueueMock to be detached
    void buildDestinations(final List<JmsMock> mocks) throws MockServerException {
        logger.debug("buildDestinations called");

        synchronized (monitor) {
            mocks.forEach(mock -> {
                if (JmsMockTypeEnum.QUEUE.equals(mock.getJmsType())) {
                    broker.setDestinations(new ActiveMQDestination[] { new ActiveMQQueue(mock.getName()) });
                } else if (JmsMockTypeEnum.TOPIC.equals(mock.getJmsType())) {
                    broker.setDestinations(new ActiveMQDestination[] { new ActiveMQTopic(mock.getName()) });
                }
            });
        }

    }

}
