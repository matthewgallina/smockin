package com.smockin.mockserver.engine;

import com.smockin.admin.persistence.entity.JmsQueueMock;
import com.smockin.mockserver.dto.MockServerState;
import com.smockin.mockserver.dto.MockedServerConfigDTO;
import com.smockin.mockserver.exception.MockServerException;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import spark.Spark;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.Session;
import java.util.List;

/**
 * Created by mgallina.
 */
@Service
public class JmsMockServerEngine implements MockServerEngine<MockedServerConfigDTO, List<JmsQueueMock>> {

    private final Logger logger = LoggerFactory.getLogger(JmsMockServerEngine.class);

    @Override
    public void start(final MockedServerConfigDTO config, final List<JmsQueueMock> data) throws MockServerException {

        initServer(config.getPort());



    }

    void initServer(final int port) throws MockServerException {
        logger.debug("initServer called");

        try {

            final ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost");

            // Create a Connection
            final Connection connection = connectionFactory.createConnection();
            connection.start();

            // Create a Session
            final Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // Create the destination (Topic or Queue)
            final Destination destination = session.createQueue("TEST.FOO");

        } catch (Throwable ex) {
            throw new MockServerException(ex);
        }

    }

    @Override
    public MockServerState getCurrentState() throws MockServerException {
        return null;
    }

    @Override
    public void shutdown() throws MockServerException {

    }
}
