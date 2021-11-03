package com.smockin.mockserver.service;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.smockin.utils.GeneralUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

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

    public File downloadObject(final String bucketName, final String filePath) throws IOException {
        logger.debug(String.format("Attempting to download file '%s' from bucket '%s'", filePath, bucketName));

        S3Object s3Object = null;

        try {

            s3Object = createS3Client().getObject(bucketName, filePath);

            final String content = IOUtils.toString(s3Object.getObjectContent(), Charset.defaultCharset());

            // TODO

            return null;
        } finally {
            if (s3Object != null) {
                s3Object.close();
            }
        }

    }

    public void createBucket(final String bucketName) {
        logger.debug(String.format("creating bucket '%s'", bucketName));

        createS3Client().createBucket(SMOCKIN_INTERNAL_UPDATE_CALL_PREFIX + bucketName);
    }

    public void deleteBucket(final String bucketName) {
        logger.debug(String.format("creating bucket '%s'", bucketName));

        createS3Client().deleteBucket(SMOCKIN_INTERNAL_UPDATE_CALL_PREFIX + bucketName);
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

    public void listsObjects(final String bucketName) {

        final ObjectListing s3Objects = createS3Client().listObjects(bucketName);

        s3Objects.getObjectSummaries()
                .stream()
                .forEach(o -> {

                    System.out.println("bucket name: " + o.getBucketName());
                    System.out.println("key: " + o.getKey());

                });

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