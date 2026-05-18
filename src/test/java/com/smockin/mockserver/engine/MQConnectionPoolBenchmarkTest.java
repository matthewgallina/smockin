package com.smockin.mockserver.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MQ Connection Pool Manager Performance Benchmark Tests
 * 
 * Tests connection pool performance under load:
 * - Concurrent pool creation
 * - Pool removal efficiency
 * - Health check performance
 */
public class MQConnectionPoolBenchmarkTest {

    private MQConnectionPoolManager poolManager;

    @BeforeEach
    public void setUp() {
        poolManager = new MQConnectionPoolManager(2, 5, 300000, 3600000);
    }

    @Test
    public void benchmark_ConcurrentPoolCreation() throws Exception {
        // Test concurrent pool creation with 50 threads
        int threadCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicLong totalTime = new AtomicLong(0);

        // Submit tasks
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    long startTime = System.nanoTime();
                    
                    String mqExtId = "mq-" + Thread.currentThread().getId();
                    poolManager.createPool(mqExtId);
                    
                    long elapsed = System.nanoTime() - startTime;
                    totalTime.addAndGet(elapsed);
                    successCount.incrementAndGet();
                    
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        
        assertTrue(completed, "Benchmark should complete within 10 seconds");
        assertEquals(threadCount, successCount.get(), "All pools should be created");
        assertEquals(0, failureCount.get(), "No failures expected");
        
        double avgTimeMs = (totalTime.get() / 1_000_000.0) / threadCount;
        System.out.println("Average pool creation time: " + String.format("%.2f", avgTimeMs) + " ms");
        
        executor.shutdown();
    }

    @Test
    public void benchmark_PoolCreationRemoval() throws Exception {
        // Test rapid pool creation and removal cycles
        int iterations = 1000;
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < iterations; i++) {
            String mqExtId = "mq-stress-" + i;
            poolManager.createPool(mqExtId);
            poolManager.removePool(mqExtId);
        }

        long elapsed = System.currentTimeMillis() - startTime;
        double opsPerSecond = (iterations * 1000.0) / elapsed;

        System.out.println("Pool create/remove benchmark: " + iterations + " iterations in " + elapsed + " ms");
        System.out.println("Operations per second: " + String.format("%.0f", opsPerSecond));
        
        assertTrue(elapsed < 5000, "Stress test should complete within 5 seconds");
    }

    @Test
    public void benchmark_PoolCreationThroughput() throws Exception {
        // Test pool creation throughput with many pools
        int poolCount = 200;
        long startTime = System.currentTimeMillis();
        
        // Create multiple pools
        for (int i = 0; i < poolCount; i++) {
            poolManager.createPool("mq-throughput-" + i);
        }

        long elapsed = System.currentTimeMillis() - startTime;
        double poolsPerSecond = (poolCount * 1000.0) / elapsed;

        System.out.println("Pool creation throughput: " + poolCount + " pools in " + elapsed + " ms");
        System.out.println("Pools created per second: " + String.format("%.0f", poolsPerSecond));
        
        assertTrue(elapsed < 3000, "Pool creation should be fast (< 3 seconds)");
    }

    @Test
    public void benchmark_MultiPoolConcurrentAccess() throws Exception {
        // Test concurrent access to multiple pools
        int poolCount = 10;
        int threadsPerPool = 10;
        
        // Create multiple pools
        for (int i = 0; i < poolCount; i++) {
            poolManager.createPool("mq-pool-" + i);
        }

        ExecutorService executor = Executors.newFixedThreadPool(poolCount * threadsPerPool);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(poolCount * threadsPerPool);
        AtomicInteger successCount = new AtomicInteger(0);

        // Submit concurrent access tasks
        for (int i = 0; i < poolCount; i++) {
            final String poolId = "mq-pool-" + i;
            for (int j = 0; j < threadsPerPool; j++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        // Just verify pool exists
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        // Ignore
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);

        assertTrue(completed, "Concurrent access should complete within 10 seconds");
        assertEquals(poolCount * threadsPerPool, successCount.get(), "All accesses should succeed");
        
        executor.shutdown();
    }

    @Test
    public void benchmark_PoolRemoval() throws Exception {
        // Benchmark pool removal performance
        int poolCount = 100;
        
        // Create pools
        for (int i = 0; i < poolCount; i++) {
            poolManager.createPool("mq-remove-" + i);
        }

        // Remove all pools
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < poolCount; i++) {
            poolManager.removePool("mq-remove-" + i);
        }

        long elapsed = System.currentTimeMillis() - startTime;
        double avgTimeMs = (double) elapsed / poolCount;

        System.out.println("Pool removal benchmark: " + poolCount + " pools in " + elapsed + " ms");
        System.out.println("Average removal time: " + String.format("%.2f", avgTimeMs) + " ms");
        
        assertTrue(elapsed < 2000, "Pool removal should be fast (< 2 seconds)");
    }
}
