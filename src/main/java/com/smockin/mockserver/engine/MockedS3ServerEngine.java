package com.smockin.mockserver.engine;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Module;
import com.smockin.admin.persistence.entity.S3Mock;
import com.smockin.admin.persistence.entity.S3MockDir;
import com.smockin.mockserver.dto.MockServerState;
import com.smockin.mockserver.dto.MockedServerConfigDTO;
import com.smockin.mockserver.exception.MockServerException;
import com.smockin.mockserver.service.S3Client;
import com.smockin.utils.GeneralUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.gaul.s3proxy.S3Proxy;
import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.concurrent.DynamicExecutors;
import org.jclouds.concurrent.config.ExecutorServiceModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Service
@Transactional(readOnly = true)
public class MockedS3ServerEngine {

    private final Logger logger = LoggerFactory.getLogger(MockedS3ServerEngine.class);

    @Autowired
    private MockedS3ServerEngineUtils mockedS3ServerEngineUtils;

    private final Object serverStateMonitor = new Object();
    private MockServerState serverState = new MockServerState(false, 0);
    private S3Proxy s3Proxy;
    private Optional<S3Client> s3Client; // A shared client instance for the duration of the S3 mock server

    public void start(final MockedServerConfigDTO configDTO,
                      final List<S3Mock> mockBuckets) throws MockServerException {
        logger.debug("started called");

        s3Proxy = S3Proxy.builder()
                .blobStore(buildInMemoryBlobStore())
                .endpoint(URI.create("http://127.0.0.1:" + configDTO.getPort()))
                .build();

        synchronized (serverStateMonitor) {

            try {
                s3Proxy.start();
                serverState.setRunning(true);
                serverState.setPort(configDTO.getPort());
            } catch (Exception e) {
                throw new MockServerException("Error starting S3 mock engine", e);
            }

            s3Client = Optional.of(buildS3Client(configDTO));
            initBucketContent(mockBuckets);

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
                s3Client = Optional.empty();
            }

        } catch (Exception e) {
            throw new MockServerException("Error shutting down S3 mock engine", e);
        }

    }

    void initBucketContent(final List<S3Mock> buckets) {
        logger.debug("initBucketContent called");

        // Create all buckets
        buckets.forEach(m ->
            s3Client.ifPresent(c ->
                c.createBucket(m.getBucketName())));

        // Create bucket files
        buckets.forEach(m ->
            addBucketFiles(s3Client, m));

        // Start adding bucket dir files
        buckets.forEach(m ->
            commenceAddingBucketDirs(s3Client, m));

    }

    void addBucketFiles(final Optional<S3Client> s3Client,
                        final S3Mock bucket) {

        bucket
            .getFiles()
            .forEach(f ->
                s3Client.ifPresent(c ->
                    c.uploadObject(
                        bucket.getBucketName(),
                        f.getName(),
                        IOUtils.toInputStream(f.getContent(), Charset.defaultCharset()),
                        f.getMimeType())));

    }

    void commenceAddingBucketDirs(final Optional<S3Client> s3Client,
                       final S3Mock bucket) {

        bucket.getChildrenDirs()
                .forEach(d ->
                    addDirFiles(s3Client, d));

    }

    void addDirFiles(final Optional<S3Client> s3Client,
                     final S3MockDir s3MockDir) {

        // Create files in this dir
        s3MockDir
            .getFiles()
            .forEach(f -> {

                final Pair<String, String> bucketAndFilePath = extractBucketAndFilePath(f.getName(), s3MockDir);

                s3Client.ifPresent(c ->
                        c.uploadObject(
                            bucketAndFilePath.getLeft(),
                            bucketAndFilePath.getRight(),
                            IOUtils.toInputStream(f.getContent(), Charset.defaultCharset()),
                            f.getMimeType()));

            });

        // Traverse child dirs
        s3MockDir.getChildren()
                .forEach(d ->
                        addDirFiles(s3Client, d));

    }

    Pair<String, String> extractBucketAndFilePath(final String fileName, final S3MockDir s3MockDir) {

        final MutablePair<String, StringBuilder> collectedData
                = new MutablePair<>(null, new StringBuilder(fileName));

        buildFilePathSegment(s3MockDir, collectedData);

        return new MutablePair<>(collectedData.getLeft(),
                                 collectedData.getRight().toString());
    }

    void buildFilePathSegment(final S3MockDir s3MockDir,
                              final MutablePair<String, StringBuilder> fileInfo) {

        // Add dir segment
        fileInfo.getRight().insert(0, File.separatorChar);
        fileInfo.getRight().insert(0, s3MockDir.getName());

        // Got a bucket so have reached the top of dir tree
        if (s3MockDir.getS3Mock() != null) {
            fileInfo.setLeft(s3MockDir.getS3Mock().getBucketName());
            return;
        }

        // keep working back up tree towards bucket
        if (s3MockDir.getParent() != null) {
            buildFilePathSegment(s3MockDir.getParent(), fileInfo);
        }

    }

    private BlobStore buildInMemoryBlobStore() {
        logger.debug("buildInMemoryBlobStore called");

        ThreadFactory factory = new ThreadFactoryBuilder()
                .setNameFormat("user thread %d")
                .setThreadFactory(Executors.defaultThreadFactory())
                .build();

        ExecutorService executorService = DynamicExecutors.newScalingThreadPool(
                1, 20, 60 * 1000, factory);

        ExecutorService executorFixed = Executors.newFixedThreadPool(10);

        BlobStoreContext context = ContextBuilder
                .newBuilder("transient")
                .credentials("identity", "credential")
                .modules(ImmutableList.<Module>of(
                        new ExecutorServiceModule(executorFixed)))
                .build(BlobStoreContext.class);

        return buildS3EventListenerProxy(context.getBlobStore());

    }

    private BlobStore buildS3EventListenerProxy(final BlobStore originalBlobStore) {
        logger.debug("buildS3EventListenerProxy called");

        final InvocationHandler handler = (proxy, method, args) -> {

            final Optional<Boolean> isInternalCall = mockedS3ServerEngineUtils.isCallInternal(args, method.getName());

            args = (isInternalCall.isPresent() && isInternalCall.get())
                    ? mockedS3ServerEngineUtils.sanitiseContainerNameInArgs(args, method.getName())
                    : args;

            final Object result = method.invoke(originalBlobStore, args);

            // (Hacky, but could not find any other way to differentiate internal and external client calls.)
            // Nothing to update DB as this is an internal S3 client call.
            if (!isInternalCall.isPresent()
                    || (isInternalCall.isPresent() && !isInternalCall.get())) {
                try {
                    mockedS3ServerEngineUtils.persistS3RemoteCall(method.getName(), args);
                } catch (Exception ex) {
                    logger.error("Error persisting update to DB", ex);
                }
            }

            return result;
        };

        return (BlobStore) Proxy.newProxyInstance(
                BlobStore.class.getClassLoader(),
                new Class[] { BlobStore.class },
                handler);

    }

    S3Client buildS3Client(final MockedServerConfigDTO configDTO) {

        return new S3Client(
                GeneralUtils.S3_HOST,
                configDTO.getPort());
    }

}
