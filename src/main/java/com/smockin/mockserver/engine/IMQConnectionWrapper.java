package com.smockin.mockserver.engine;

/**
 * MQ Connection Wrapper Interface
 * Common interface for all MQ connection types (JMS, Kafka, Solace)
 */
public interface IMQConnectionWrapper extends AutoCloseable {
    
    /**
     * Close the connection
     */
    @Override
    void close();
    
    /**
     * Get connection type
     */
    String getConnectionType();
}
