package com.smockin.admin.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * sMockin Comprehensive Metrics Collector
 * 
 * Monitors ALL sMockin functional modules:
 * - REST API Mock
 * - S3 Bucket Mock
 * - Mail Mock Server
 * - MQ Mock (Kafka, RabbitMQ, ActiveMQ, IBM MQ, Solace)
 * - Proxy Server
 * - WebSocket
 * - Database (HikariCP)
 * - System Resources (JVM, CPU, Memory)
 * 
 * @author sMockin Team
 */
@Service
public class SmockinMetricsCollector {

    private final MeterRegistry meterRegistry;

    // REST API Mock Metrics
    private Counter restRequestsTotal;
    private Timer restRequestDuration;
    private Counter restErrorsTotal;
    private AtomicInteger activeMocksCount;
    private Counter jsExecutionsTotal;
    private Timer jsExecutionDuration;

    // S3 Mock Metrics
    private Counter s3RequestsTotal;
    private Timer s3RequestDuration;
    private AtomicInteger activeBucketsCount;
    private Gauge s3StorageBytes;
    private Counter s3UploadsTotal;
    private Counter s3DownloadsTotal;

    // Mail Mock Metrics
    private Counter mailReceivedTotal;
    private AtomicInteger activeMailAccountsCount;
    private Gauge mailMessagesStored;
    private Counter mailAttachmentsTotal;

    // MQ Mock Metrics
    private Counter mqMessagesSentTotal;
    private Counter mqMessagesReceivedTotal;
    private Timer mqMessageProcessingDuration;
    private AtomicInteger mqActiveConnectionsCount;
    private Gauge mqConnectionPoolSize;
    private Gauge mqConnectionPoolActive;
    private Gauge mqConnectionPoolIdle;
    private Counter mqErrorsTotal;

    // Proxy Server Metrics
    private Counter proxyRequestsTotal;
    private Timer proxyRequestDuration;
    private Counter proxyErrorsTotal;
    private Counter proxyBlockedRequestsTotal;

    // WebSocket Metrics
    private Gauge wsActiveConnections;
    private Counter wsConnectionsTotal;
    private Counter wsMessagesSentTotal;
    private Counter wsMessagesReceivedTotal;

    // Database Metrics
    private Gauge dbConnectionsActive;
    private Gauge dbConnectionsIdle;

    public SmockinMetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Initialize counters and gauges
        this.activeMocksCount = new AtomicInteger(0);
        this.activeBucketsCount = new AtomicInteger(0);
        this.activeMailAccountsCount = new AtomicInteger(0);
        this.mqActiveConnectionsCount = new AtomicInteger(0);
    }

    @PostConstruct
    public void init() {
        // REST API Mock Metrics
        restRequestsTotal = Counter.builder("smockin_rest_requests_total")
                .description("Total REST API mock requests")
                .tags("application", "smockin")
                .register(meterRegistry);

        restRequestDuration = Timer.builder("smockin_rest_request_duration_seconds")
                .description("REST API mock request duration")
                .register(meterRegistry);

        restErrorsTotal = Counter.builder("smockin_rest_errors_total")
                .description("Total REST API mock errors")
                .register(meterRegistry);

        Gauge.builder("smockin_rest_active_mocks", activeMocksCount, AtomicInteger::get)
                .description("Number of active REST API mocks")
                .register(meterRegistry);

        jsExecutionsTotal = Counter.builder("smockin_rest_js_executions_total")
                .description("Total JavaScript rule executions")
                .register(meterRegistry);

        jsExecutionDuration = Timer.builder("smockin_rest_js_execution_duration_seconds")
                .description("JavaScript rule execution duration")
                .register(meterRegistry);

        // S3 Mock Metrics
        s3RequestsTotal = Counter.builder("smockin_s3_requests_total")
                .description("Total S3 mock requests")
                .register(meterRegistry);

        s3RequestDuration = Timer.builder("smockin_s3_request_duration_seconds")
                .description("S3 mock request duration")
                .register(meterRegistry);

        Gauge.builder("smockin_s3_active_buckets", activeBucketsCount, AtomicInteger::get)
                .description("Number of active S3 buckets")
                .register(meterRegistry);

        s3UploadsTotal = Counter.builder("smockin_s3_uploads_total")
                .description("Total S3 file uploads")
                .register(meterRegistry);

        s3DownloadsTotal = Counter.builder("smockin_s3_downloads_total")
                .description("Total S3 file downloads")
                .register(meterRegistry);

        // Mail Mock Metrics
        mailReceivedTotal = Counter.builder("smockin_mail_received_total")
                .description("Total mail messages received")
                .register(meterRegistry);

        Gauge.builder("smockin_mail_active_accounts", activeMailAccountsCount, AtomicInteger::get)
                .description("Number of active mail accounts")
                .register(meterRegistry);

        mailAttachmentsTotal = Counter.builder("smockin_mail_attachments_total")
                .description("Total mail attachments received")
                .register(meterRegistry);

        // MQ Mock Metrics
        mqMessagesSentTotal = Counter.builder("smockin_mq_messages_sent_total")
                .description("Total MQ messages sent")
                .tags("application", "smockin")
                .register(meterRegistry);

        mqMessagesReceivedTotal = Counter.builder("smockin_mq_messages_received_total")
                .description("Total MQ messages received")
                .tags("application", "smockin")
                .register(meterRegistry);

        mqMessageProcessingDuration = Timer.builder("smockin_mq_message_processing_duration_seconds")
                .description("MQ message processing duration")
                .register(meterRegistry);

        Gauge.builder("smockin_mq_active_connections", mqActiveConnectionsCount, AtomicInteger::get)
                .description("Number of active MQ connections")
                .register(meterRegistry);

        mqErrorsTotal = Counter.builder("smockin_mq_errors_total")
                .description("Total MQ operation errors")
                .register(meterRegistry);

        // Proxy Server Metrics
        proxyRequestsTotal = Counter.builder("smockin_proxy_requests_total")
                .description("Total proxy server requests")
                .register(meterRegistry);

        proxyRequestDuration = Timer.builder("smockin_proxy_request_duration_seconds")
                .description("Proxy server request duration")
                .register(meterRegistry);

        proxyErrorsTotal = Counter.builder("smockin_proxy_errors_total")
                .description("Total proxy server errors")
                .register(meterRegistry);

        proxyBlockedRequestsTotal = Counter.builder("smockin_proxy_blocked_requests_total")
                .description("Total blocked proxy requests")
                .register(meterRegistry);

        // WebSocket Metrics
        wsConnectionsTotal = Counter.builder("smockin_ws_connections_total")
                .description("Total WebSocket connections")
                .register(meterRegistry);

        wsMessagesSentTotal = Counter.builder("smockin_ws_messages_sent_total")
                .description("Total WebSocket messages sent")
                .register(meterRegistry);

        wsMessagesReceivedTotal = Counter.builder("smockin_ws_messages_received_total")
                .description("Total WebSocket messages received")
                .register(meterRegistry);

        // Database Metrics (HikariCP metrics are automatically collected by Spring Boot)
        // Additional custom DB metrics can be added here if needed
    }

    // ==================== REST API Mock Metrics Methods ====================

    public void recordRestRequest(String method, String path, int statusCode) {
        restRequestsTotal.increment();
    }

    public void recordRestRequestDuration(long durationMs, String method, String path) {
        restRequestDuration.record(java.time.Duration.ofMillis(durationMs));
    }

    public void recordRestError(String method, String errorType) {
        restErrorsTotal.increment();
    }

    public void setActiveMocksCount(int count) {
        activeMocksCount.set(count);
    }

    public void recordJsExecution(String mockId) {
        jsExecutionsTotal.increment();
    }

    public void recordJsExecutionDuration(long durationMs) {
        jsExecutionDuration.record(java.time.Duration.ofMillis(durationMs));
    }

    // ==================== S3 Mock Metrics Methods ====================

    public void recordS3Request(String operation, String bucket, int statusCode) {
        s3RequestsTotal.increment();
    }

    public void recordS3RequestDuration(long durationMs, String operation, String bucket) {
        s3RequestDuration.record(java.time.Duration.ofMillis(durationMs));
    }

    public void setActiveBucketsCount(int count) {
        activeBucketsCount.set(count);
    }

    public void recordS3Upload(String bucket) {
        s3UploadsTotal.increment();
    }

    public void recordS3Download(String bucket) {
        s3DownloadsTotal.increment();
    }

    // ==================== Mail Mock Metrics Methods ====================

    public void recordMailReceived(String account, boolean hasAttachment) {
        mailReceivedTotal.increment();
        if (hasAttachment) {
            mailAttachmentsTotal.increment();
        }
    }

    public void setActiveMailAccountsCount(int count) {
        activeMailAccountsCount.set(count);
    }

    // ==================== MQ Mock Metrics Methods ====================

    public void recordMqMessageSent(String mqType, String destination, String mockId) {
        mqMessagesSentTotal.increment();
    }

    public void recordMqMessageReceived(String mqType, String destination, String mockId) {
        mqMessagesReceivedTotal.increment();
    }

    public void recordMqMessageProcessingDuration(long durationMs, String mqType, String operation) {
        mqMessageProcessingDuration.record(java.time.Duration.ofMillis(durationMs));
    }

    public void setMqActiveConnectionsCount(int count) {
        mqActiveConnectionsCount.set(count);
    }

    public void recordMqError(String mqType, String errorType) {
        mqErrorsTotal.increment();
    }

    // ==================== Proxy Server Metrics Methods ====================

    public void recordProxyRequest(String method, String path, String target, int statusCode) {
        proxyRequestsTotal.increment();
    }

    public void recordProxyRequestDuration(long durationMs, String target) {
        proxyRequestDuration.record(java.time.Duration.ofMillis(durationMs));
    }

    public void recordProxyError(String target, String errorType) {
        proxyErrorsTotal.increment();
    }

    public void recordBlockedRequest(String path) {
        proxyBlockedRequestsTotal.increment();
    }

    // ==================== WebSocket Metrics Methods ====================

    public void recordWebSocketConnection() {
        wsConnectionsTotal.increment();
    }

    public void recordWsMessageSent(String connectionId) {
        wsMessagesSentTotal.increment();
    }

    public void recordWsMessageReceived(String connectionId) {
        wsMessagesReceivedTotal.increment();
    }

    public void setWsActiveConnections(int count) {
        // This would be a Gauge that updates dynamically
    }
}
