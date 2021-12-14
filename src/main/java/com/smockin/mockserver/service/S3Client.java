package com.smockin.mockserver.service;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.smockin.mockserver.engine.MockedS3ServerEngineUtils;
import com.smockin.utils.GeneralUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class S3Client {

    private final Logger logger = LoggerFactory.getLogger(S3Client.class);

    static final String REGION = "us-east-1";
    public static final String SMOCKIN_INTERNAL_UPDATE_CALL_PREFIX = "s3mockin-upc-";

    private final String host;
    private final int port;

    public S3Client(final String host,
                    final int port) {
        this.host = host;
        this.port = port;
    }

    public void createBucket(final String bucketName) {
        logger.debug(String.format("creating bucket '%s'", bucketName));

        createS3Client().createBucket(SMOCKIN_INTERNAL_UPDATE_CALL_PREFIX + bucketName);
    }

    public void createSubDirectory(final String bucketName,
                                   final String folderPath) {

        final ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(0);

        final PutObjectRequest putObjectRequest = new PutObjectRequest(
                SMOCKIN_INTERNAL_UPDATE_CALL_PREFIX + bucketName,
                folderPath + handleSeparatorSuffix(folderPath),
                    new ByteArrayInputStream(new byte[0]),
                    metadata);

        createS3Client().putObject(putObjectRequest);
    }

    String handleSeparatorSuffix(final String folderPath) {

        return ((StringUtils.endsWith(folderPath, MockedS3ServerEngineUtils.SEPARATOR_CHAR))
                ? ""
                : MockedS3ServerEngineUtils.SEPARATOR_CHAR);
    }

    public void deleteBucket(final String bucketName,
                             final boolean muteFailure) {
        logger.debug(String.format("deleting bucket '%s'", bucketName));

        try {

            final AmazonS3 client = createS3Client();

            final ObjectListing listing = client.listObjects(SMOCKIN_INTERNAL_UPDATE_CALL_PREFIX + bucketName);

            final List<DeleteObjectsRequest.KeyVersion> files =
                    listing.getObjectSummaries()
                            .stream()
                            .map(e ->
                                    new DeleteObjectsRequest.KeyVersion(e.getKey()))
                            .collect(Collectors.toList());

            if (!files.isEmpty()) {

                final DeleteObjectsRequest multiObjectDeleteRequest = new DeleteObjectsRequest(bucketName)
                        .withBucketName(SMOCKIN_INTERNAL_UPDATE_CALL_PREFIX + bucketName)
                        .withKeys(files)
                        .withQuiet(false);

                // delete all bucket content...
                client.deleteObjects(multiObjectDeleteRequest);

            }

            // ...now delete the bucket itself
            client.deleteBucket(SMOCKIN_INTERNAL_UPDATE_CALL_PREFIX + bucketName);

        } catch (Exception ex) {

            logger.error("Error deleting bucket", ex);

            if (!muteFailure) {
                throw ex;
            }
        }

    }

    public void uploadObject(final String bucketName, final String filePath, final InputStream is, final String mimeType) {
        logger.debug(String.format("uploading file '%s' to bucket '%s'", filePath, bucketName));

        try {
            final ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentType(mimeType);
            createS3Client().putObject(SMOCKIN_INTERNAL_UPDATE_CALL_PREFIX + bucketName, filePath, is, objectMetadata);
        } finally {
            GeneralUtils.closeSilently(is);
        }
    }

    public void deleteObject(final String bucketName, final String filePath) {
        logger.debug(String.format("deleting file '%s' from bucket '%s'", filePath, bucketName));

        createS3Client().deleteObject(SMOCKIN_INTERNAL_UPDATE_CALL_PREFIX + bucketName, filePath);

    }

    public Optional<String> getObjectContent(final String bucketName, final String filePath) {

        final GetObjectRequest getObjectRequest =
                new GetObjectRequest(SMOCKIN_INTERNAL_UPDATE_CALL_PREFIX + bucketName, filePath);

        final S3Object s3Object = createS3Client().getObject(getObjectRequest);

        try {
            return Optional.of(IOUtils.toString(s3Object.getObjectContent(), Charset.defaultCharset()));
        } catch (IOException ex) {
            logger.error(String.format("Error retrieving s3Object '%s' in bucket '%s' from mock server", filePath, bucketName), ex);
        } finally {
            try {
                s3Object.close();
            } catch (IOException ex) {
                logger.error("Error closing s3Object", ex);
            }
        }

        return Optional.empty();
    }

    AmazonS3 createS3Client() {
        return createS3Client(REGION);
    }

    AmazonS3 createS3Client(final String region) {

        return AmazonS3ClientBuilder.standard()
//                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(username, password)))
//                .withClientConfiguration(
//                        configureClientToIgnoreInvalidSslCertificates(new ClientConfiguration()))
                .withEndpointConfiguration(getEndpointConfiguration(region))
                .enablePathStyleAccess()
                .build();
    }

    /*
    ClientConfiguration configureClientToIgnoreInvalidSslCertificates(
            final ClientConfiguration clientConfiguration) {

        clientConfiguration.getApacheHttpClientConfig()
                .withSslSocketFactory(new SSLConnectionSocketFactory(
                        createBlindlyTrustingSslContext(),
                        NoopHostnameVerifier.INSTANCE));

        return clientConfiguration;
    }
*/

    AwsClientBuilder.EndpointConfiguration getEndpointConfiguration(final String region) {
        return new AwsClientBuilder.EndpointConfiguration(getServiceEndpoint(), region);
    }

    String getServiceEndpoint() {

//        final Map<String, Object> properties = new HashMap<>();
//        properties.put(DEFAULT_HTTPS_PORT, "0");
//        properties.put(String.valueOf(port), "0");
//        final boolean isSecureConnection = (boolean) properties.getOrDefault(S3MockApplication.PROP_SECURE_CONNECTION, true);

//        final boolean isSecureConnection = false;

//        return isSecureConnection ? "https://" + host + ":" + DEFAULT_HTTPS_PORT
//               : "http://" + host + ":" + port;

        return "http://" + host + ":" + port;
    }

/*
    private SSLContext createBlindlyTrustingSslContext() {
        try {
            final SSLContext sc = SSLContext.getInstance("TLS");

            sc.init(null, new TrustManager[]{new X509ExtendedTrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                @Override
                public void checkClientTrusted(final X509Certificate[] arg0, final String arg1,
                                               final Socket arg2) {
                    // no-op
                }

                @Override
                public void checkClientTrusted(final X509Certificate[] arg0, final String arg1,
                                               final SSLEngine arg2) {
                    // no-op
                }

                @Override
                public void checkClientTrusted(final X509Certificate[] certs, final String authType) {
                    // no-op
                }

                @Override
                public void checkServerTrusted(final X509Certificate[] certs, final String authType) {
                    // no-op
                }

                @Override
                public void checkServerTrusted(final X509Certificate[] arg0, final String arg1,
                                               final Socket arg2) {
                    // no-op
                }

                @Override
                public void checkServerTrusted(final X509Certificate[] arg0, final String arg1,
                                               final SSLEngine arg2) {
                    // no-op
                }
            }
            }, new java.security.SecureRandom());

            return sc;
        } catch (final NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException("Unexpected exception", e);
        }
    }
*/
}
