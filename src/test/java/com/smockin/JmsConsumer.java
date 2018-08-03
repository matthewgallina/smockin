package com.smockin;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.Ignore;
import org.junit.Test;

import javax.jms.*;

/**
 * Created by mgallina.
 */
@Ignore
public class JmsConsumer {

    @Test
    public void start() throws Exception {

        // curl -i -H "Content-Type: application/json" -X POST -d '{ "name" : "dogs", "body" : "Hello", "mimeType" : "text/plain" }' http://mgallina-desktop:8000/jmsmock/EXT_ID/queue

        final String brokerUrl = "tcp://localhost:8002";
        final String queueName = "user/jmsName";

        final ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);

        // Create a Connection
        final Connection connection = factory.createConnection();
        connection.start();

        final Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        final MessageConsumer consumer = session.createConsumer(session.createQueue(queueName));
        final Message message = consumer.receive(30000); // 30 secs

        if (message instanceof TextMessage) {

            final TextMessage textMessage = (TextMessage) message;
            String text = textMessage.getText();

            System.out.println("Received Text: " + text);

        } else {

            System.out.println("Received Object: " + message);

        }

        consumer.close();
        session.close();
        connection.close();
    }

}
