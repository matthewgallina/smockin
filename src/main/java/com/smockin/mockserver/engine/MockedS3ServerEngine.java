package com.smockin.mockserver.engine;

import com.smockin.admin.persistence.entity.S3Mock;
import com.smockin.mockserver.dto.MockServerState;
import com.smockin.mockserver.dto.MockedServerConfigDTO;
import com.smockin.mockserver.exception.MockServerException;
import com.smockin.mockserver.service.S3Client;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.gaul.s3proxy.S3Proxy;
import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class MockedS3ServerEngine {

    private final Logger logger = LoggerFactory.getLogger(MockedS3ServerEngine.class);

    // Set via MockedServerConfigDTO
    private String username = "ABCDEF";
    private String password = "letmein";

    private final Object serverStateMonitor = new Object();
    private MockServerState serverState = new MockServerState(false, 0);
    private S3Proxy s3Proxy;


    public void start(final MockedServerConfigDTO configDTO,
                      final List<S3Mock> mocks) throws MockServerException {
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

            initBucketContent(configDTO, mocks);

        }

//        while (!s3Proxy.getState().equals(AbstractLifeCycle.STARTED)) {
//            Thread.sleep(1);
//        }

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
            }

        } catch (Exception e) {
            throw new MockServerException("Error shutting down S3 mock engine", e);
        }

    }

    void initBucketContent(final MockedServerConfigDTO configDTO,
                           final List<S3Mock> mocks) {
        logger.debug("initBucketContent called");

        // TODO assign settings from configDTO
        final S3Client s3Client = new S3Client(username, password, "localhost", 9090);

        // Create all buckets
        mocks.forEach(m ->
            s3Client.createBucket(m.getBucket()));

        mocks.forEach(m ->
            addFiles(s3Client, m));

    }

    void addFiles(final S3Client s3Client,
                  final S3Mock s3Mock) {

        s3Mock
            .getFiles()
            .forEach(f -> {

                final Pair<String, String> bucketAndFilePath = extractBucketAndFilePath(f.getName(), s3Mock);

                s3Client.uploadObject(
                        bucketAndFilePath.getLeft(),
                        bucketAndFilePath.getRight(),
                        IOUtils.toInputStream(f.getContent(), Charset.defaultCharset()),
                        f.getMimeType());

        });

        s3Mock.getChildren()
              .forEach(m ->
                addFiles(s3Client, m));

    }

    Pair<String, String> extractBucketAndFilePath(final String fileName, final S3Mock s3Mock) {

        final MutablePair<String, StringBuilder> collectedData
                = new MutablePair<>(null, new StringBuilder(fileName));

        buildFilePathSegment(s3Mock, collectedData);

        return new MutablePair<>(collectedData.getLeft(), collectedData.getRight().toString());
    }

    void buildFilePathSegment(final S3Mock s3Mock,
                   final MutablePair<String, StringBuilder> fileInfo) {

        if (s3Mock.getParent() == null) {
            fileInfo.setLeft(s3Mock.getBucket());
            return;
        }

        fileInfo.getRight().insert(0, File.separatorChar);
        fileInfo.getRight().insert(0, s3Mock.getBucket());

        if (s3Mock.getParent() != null) {
            buildFilePathSegment(s3Mock.getParent(), fileInfo);
        }

    }

    private BlobStore buildInMemoryBlobStore() {

        BlobStoreContext context = ContextBuilder
                .newBuilder("transient")
                .credentials("identity", "credential")
                .build(BlobStoreContext.class);

        return buildS3EventListenerProxy(context.getBlobStore());

    }

    private BlobStore buildS3EventListenerProxy(final BlobStore originalBlobStore) {

        InvocationHandler handler = (proxy, method, args) -> {

            Object result = method.invoke(originalBlobStore, args);

            processS3Action(method.getName());

            return result;
        };

        return (BlobStore) Proxy.newProxyInstance(
                BlobStore.class.getClassLoader(),
                new Class[] { BlobStore.class },
                handler);

    }

    private void processS3Action(final String methodName) {
        logger.debug("processS3Action called");

        if (logger.isDebugEnabled())
            logger.debug("method name: " + methodName);

        if ("createContainerInLocation".equalsIgnoreCase(methodName)) {

        } else if ("clearContainer".equalsIgnoreCase(methodName)) {

        } else if ("deleteContainer".equalsIgnoreCase(methodName)) {

        } else if ("putBlob".equalsIgnoreCase(methodName)) {

        } else if ("removeBlob".equalsIgnoreCase(methodName)) {

        } else if ("getBlob".equalsIgnoreCase(methodName)) {

        }

    }

}
