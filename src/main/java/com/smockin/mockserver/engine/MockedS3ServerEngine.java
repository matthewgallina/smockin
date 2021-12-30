package com.smockin.mockserver.engine;

import com.google.common.collect.ImmutableList;
import com.google.inject.Module;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.persistence.entity.S3Mock;
import com.smockin.mockserver.dto.MockServerState;
import com.smockin.mockserver.dto.MockedServerConfigDTO;
import com.smockin.mockserver.exception.MockServerException;
import com.smockin.mockserver.service.S3Client;
import org.gaul.s3proxy.S3Proxy;
import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.concurrent.config.ExecutorServiceModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Transactional(readOnly = true)
public class MockedS3ServerEngine {

    private final Logger logger = LoggerFactory.getLogger(MockedS3ServerEngine.class);

    private String host = "http://127.0.0.1";

    @Autowired
    private MockedS3ServerEngineUtils mockedS3ServerEngineUtils;

    private final Object serverStateMonitor = new Object();
    private MockServerState serverState = new MockServerState(false, 0);
    private S3Proxy s3Proxy;
    private S3Client s3Client; // A shared client instance for the duration of the S3 mock server

    public void start(final MockedServerConfigDTO configDTO,
                      final List<S3Mock> mockBuckets) throws MockServerException {
        logger.debug("started called");

        s3Proxy = S3Proxy.builder()
                .blobStore(buildInMemoryBlobStore(configDTO))
                .endpoint(URI.create(host + ":" + configDTO.getPort()))
                .build();

        synchronized (serverStateMonitor) {

            try {
                s3Proxy.start();
                serverState.setRunning(true);
                serverState.setPort(configDTO.getPort());
            } catch (Exception e) {
                throw new MockServerException("Error starting S3 mock engine", e);
            }

            s3Client = mockedS3ServerEngineUtils.buildS3Client(configDTO.getPort());
            mockedS3ServerEngineUtils.initBucketContent(s3Client, mockBuckets);
        }

    }

    public MockServerState getCurrentState() throws MockServerException {
        synchronized (serverStateMonitor) {
            return serverState;
        }
    }

    public void shutdown() throws MockServerException {
        logger.debug("shutdown called");

        try {

            synchronized (serverStateMonitor) {
                s3Proxy.stop();
                serverState.setRunning(false);
                s3Client = null;
            }

        } catch (Exception e) {
            throw new MockServerException("Error shutting down S3 mock engine", e);
        }

    }

    private BlobStore buildInMemoryBlobStore(final MockedServerConfigDTO configDTO) {
        logger.debug("buildInMemoryBlobStore called");

        final ExecutorService executorFixed = Executors.newFixedThreadPool(10);

        BlobStoreContext context = ContextBuilder
                .newBuilder("transient")
                .credentials("identity", "credential")
                .modules(ImmutableList.<Module>of(
                        new ExecutorServiceModule(executorFixed)))
                .build(BlobStoreContext.class);

        return buildS3EventListenerProxy(context.getBlobStore(), configDTO);

    }

    private BlobStore buildS3EventListenerProxy(final BlobStore originalBlobStore,
                                                final MockedServerConfigDTO configDTO) {
        logger.debug("buildS3EventListenerProxy called");

        final InvocationHandler handler = (proxy, method, args) -> {

            if (logger.isDebugEnabled())
                logger.debug("PROXY METHOD: " + method.getName());

            final Optional<Boolean> isInternalCall = mockedS3ServerEngineUtils.isCallInternal(args, method.getName());

            if (logger.isDebugEnabled())
                logger.debug("PROXY > isInternalCall: " + isInternalCall);

            args = (isInternalCall.isPresent() && isInternalCall.get())
                    ? mockedS3ServerEngineUtils.sanitiseContainerNameInArgs(args, method.getName())
                    : args;

            if (logger.isDebugEnabled()) {
                if (args != null) {
                    for (Object arg : args) {
                        logger.debug("PROXY ARG: " + arg);
                    }
                }
            }

            final Object result = method.invoke(originalBlobStore, args);
            Optional<String> bucketOwnerId = Optional.empty();
            boolean bucketNotFoundOnServer = false;

            // (Hacky, but could not find any other way to differentiate internal and external client calls.)
            // Nothing to update in DB as this is an internal S3 client call.
            if (!isInternalCall.isPresent()
                    || (isInternalCall.isPresent() && !isInternalCall.get())) {
                try {
                    bucketOwnerId = mockedS3ServerEngineUtils.persistS3RemoteCall(method.getName(), args, configDTO);
                } catch (RecordNotFoundException ex) {
                    bucketNotFoundOnServer = true;
                } catch (Exception ex) {
                    logger.error("Error persisting update to DB", ex);
                }
            }

            if (!bucketNotFoundOnServer) {
                try {
                    mockedS3ServerEngineUtils.logS3RemoteCall(method.getName(), args, bucketOwnerId);
                } catch (RecordNotFoundException ex) {
                } catch (Exception ex) {
                    logger.error("Error logging S3 activity", ex);
                }
            }

            return result;
        };

        return (BlobStore) Proxy.newProxyInstance(
                BlobStore.class.getClassLoader(),
                new Class[] { BlobStore.class },
                handler);

    }

}
