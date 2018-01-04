package com.smockin;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.Ignore;
import org.junit.Test;

import javax.jms.*;

/**
 * Created by mgallina.
 */
@Ignore
public class JmsDurableSubscriber {

    @Test
    public void start() throws Exception {

        // curl -i -H "Content-Type: application/json" -X POST -d '{ "name" : "mytopic", "body" : "Hello", "mimeType" : "text/plain" }' http://mgallina-desktop:8000/jms/topic

        final String brokerUrl = "tcp://localhost:61616";
        final String clientId = "bob";
        final String subscriptionName = clientId;
        final String topicName = "mytopic";

        final ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);

        // Create a Connection
        final Connection connection = factory.createConnection();
        connection.setClientID(clientId);

        final Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
//        final MessageConsumer consumer = session.createConsumer(session.createTopic(topicName));
        final MessageConsumer consumer = session.createDurableSubscriber(session.createTopic(topicName), subscriptionName);

        connection.start();

        final Message message = consumer.receive(60000); // 1 min

        if (message instanceof TextMessage) {

            final TextMessage textMessage = (TextMessage) message;
            String text = textMessage.getText();

            System.out.println("Received Broadcast Text: " + text);

        } else {

            System.out.println("Received Broadcast Object: " + message);

        }

        /*
        session.unsubscribe(subscriptionName);
        consumer.close();
        session.close();
        connection.close();
        */
    }

}
