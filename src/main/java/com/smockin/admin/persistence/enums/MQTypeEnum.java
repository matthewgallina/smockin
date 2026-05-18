package com.smockin.admin.persistence.enums;

/**
 * MQ Type Enum - Protocol-based classification
 * 
 * Supports 3 major MQ protocols:
 * - JMS: Java Message Service (ActiveMQ, IBM MQ, Solace, etc.)
 * - AMQP: Advanced Message Queuing Protocol (RabbitMQ, Qpid, etc.)
 * - KAFKA: Apache Kafka Protocol
 */
public enum MQTypeEnum {
    /**
     * Java Message Service (JMS) 2.0
     * Supports: Apache ActiveMQ, IBM MQ, Solace, WebLogic JMS, Oracle AQ, etc.
     * Provider specified in properties.jmsProvider field
     */
    JMS,
    
    /**
     * Advanced Message Queuing Protocol (AMQP) 0-9-1
     * Supports: RabbitMQ, Apache Qpid, Azure Service Bus, etc.
     * Provider specified in properties.amqpProvider field
     */
    AMQP,
    
    /**
     * Apache Kafka Protocol
     * Supports: Apache Kafka, Confluent Platform
     */
    KAFKA;
    
    /**
     * JMS Provider types (specified in properties.jmsProvider)
     */
    public enum JMSProvider {
        ACTIVEMQ,    // Apache ActiveMQ (default)
        IBMMQ,       // IBM MQ
        SOLACE,      // Solace PubSub+
        GENERIC      // Generic JMS provider
    }
    
    /**
     * AMQP Provider types (specified in properties.amqpProvider)
     */
    public enum AMQPProvider {
        RABBITMQ,    // RabbitMQ (default)
        QPID,        // Apache Qpid
        GENERIC      // Generic AMQP provider
    }

    public static MQTypeEnum toMQType(final String value) {
        if (value == null) {
            return null;
        }

        for (MQTypeEnum mt : values()) {
            if (org.apache.commons.lang3.StringUtils.equals(mt.name(), value)) {
                return mt;
            }
        }

        return null;
    }
}
