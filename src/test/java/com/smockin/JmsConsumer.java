package com.smockin;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.Test;

import javax.jms.*;

/**
 * Created by mgallina on 20/10/17.
 */
public class JmsConsumer {

    @Test
    public void start() throws Exception {

        final String brokerUrl = "tcp://localhost:61616";
        final String queueName = "SMOCKIN.QUEUE";

        final ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);

        // Create a Connection
        final Connection connection = factory.createConnection();
        connection.start();

        final Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        final MessageConsumer consumer = session.createConsumer(session.createQueue(queueName));
        final Message message = consumer.receive(20000);

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
