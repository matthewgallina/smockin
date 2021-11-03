package com.smockin.mockserver.engine;

import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.persistence.dao.S3MockDAO;
import com.smockin.admin.persistence.dao.S3MockDirDAO;
import com.smockin.admin.persistence.entity.S3Mock;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.service.SmockinUserService;
import com.smockin.admin.websocket.LiveLoggingHandler;
import com.smockin.mockserver.service.S3Client;
import com.smockin.utils.LiveLoggingUtils;
import org.apache.commons.lang3.StringUtils;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.blobstore.options.CopyOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class MockedS3ServerEngineUtils {

    private final Logger logger = LoggerFactory.getLogger(MockedS3ServerEngineUtils.class);

    private List<String> supportedInternalS3ClientUpdateMethods; // i.e BlobStore update based methods based on the functions present in S3Client

    private static final String CREATE_CONTAINER_IN_LOCATION_METHOD = "createContainerInLocation";
    private static final String DELETE_CONTAINER_METHOD = "deleteContainer";
    private static final String CLEAR_CONTAINER_METHOD = "clearContainer";
    private static final String PUT_BLOB_METHOD = "putBlob";
    private static final String REMOVE_BLOB_METHOD = "removeBlob";
    private static final String COPY_BLOB_METHOD = "copyBlob";


    @Autowired
    private S3MockDAO s3MockDAO;

    @Autowired
    private S3MockDirDAO s3MockDirDAO;

    @Autowired
    private SmockinUserService smockinUserService;

    @Autowired
    private LiveLoggingHandler liveLoggingHandler;


    @Transactional
    public void persistS3RemoteCall(final String methodName,
                                 final Object[] args) {

        logger.debug("persistS3RemoteCall called");

        if (logger.isDebugEnabled())
            logger.debug("method name: " + methodName);

        if (!supportedInternalS3ClientUpdateMethods.contains(methodName)) {
            return;
        }

        /*
        if (args != null) {
            for (Object arg : args) {
                System.out.println(arg);
                if (arg != null) {
                    System.out.println(arg.getClass().getName());
                }
            }
        }
        */

        if (CREATE_CONTAINER_IN_LOCATION_METHOD.equalsIgnoreCase(methodName)) {

            final String containerName = (String)args[1];

            final Optional<SmockinUser> adminUserOpt = smockinUserService.loadDefaultUser();

            if (!adminUserOpt.isPresent()) {
                return;
            }

            final S3Mock s3Mock = new S3Mock();
            s3Mock.setBucketName(containerName);
            s3Mock.setStatus(RecordStatusEnum.ACTIVE);
            s3Mock.setCreatedBy(adminUserOpt.get());
            s3MockDAO.save(s3Mock);

            handleS3Logging("A new bucket '" + containerName + "' was created");

        } else if (CLEAR_CONTAINER_METHOD.equalsIgnoreCase(methodName)) {

            final String containerName = (String)args[0];

            final S3Mock s3Mock = findS3MockByBucketName(containerName);

            s3Mock.getChildrenDirs().clear();
            s3Mock.getFiles().clear();

            s3MockDAO.save(s3Mock);

            handleS3Logging("The bucket '" + containerName + "' has had all content removed");

        } else if (DELETE_CONTAINER_METHOD.equalsIgnoreCase(methodName)) {

            final String containerName = (String)args[0];

            final S3Mock s3Mock = findS3MockByBucketName(containerName);

            s3MockDAO.delete(s3Mock);

            handleS3Logging("The bucket '" + containerName + "' was deleted");

        } else if (PUT_BLOB_METHOD.equalsIgnoreCase(methodName)) {

            final String containerName = (String)args[0];
            final Blob blob = (Blob) args[1];

            final S3Mock s3Mock = findS3MockByBucketName(containerName);

            // todo
System.out.println(containerName);
System.out.println(blob.getAllHeaders());
System.out.println(blob.getMetadata());

            handleS3Logging("Added file " + "todo" + " to bucket '" + containerName + "'");

        } else if (REMOVE_BLOB_METHOD.equalsIgnoreCase(methodName)) {

            final String containerName = (String)args[0];
            final String name = (String) args[1];

System.out.println(containerName);
System.out.println(name);

            // todo

            handleS3Logging("Removed file " + name + " from bucket '" + containerName + "'");

        } else if (COPY_BLOB_METHOD.equalsIgnoreCase(methodName)) {

            final String fromContainer = (String)args[0];
            final String fromName = (String) args[1];
            final String toContainer = (String) args[2];
            final String toName = (String) args[3];
            final CopyOptions options = (CopyOptions) args[4];

            final S3Mock fromBucket = findS3MockByBucketName(fromContainer);
            final S3Mock toBucket = findS3MockByBucketName(toContainer);

            // todo

            handleS3Logging("Copied file " + fromName + " from bucket '" + fromContainer + "' into bucket '" + toBucket + "'");

        }

    }

    S3Mock findS3MockByBucketName(final String name) throws RecordNotFoundException {

        final S3Mock s3Mock = s3MockDAO.findByBucketName(name);

        if (s3Mock == null)
            throw new RecordNotFoundException();

        return s3Mock;
    }

    void handleS3Logging(final String message) {

        liveLoggingHandler.broadcast(LiveLoggingUtils.buildS3LiveLogging(message));
    }



    public Object[] sanitiseContainerNameInArgs(final Object[] originalArgs,
                                                final String methodName) {
        logger.debug("sanitiseContainerNameInArgs called");

        if (!supportedInternalS3ClientUpdateMethods.contains(methodName)) {
            return originalArgs;
        }

        final String containerName = (CREATE_CONTAINER_IN_LOCATION_METHOD.equalsIgnoreCase(methodName))
                ? (String) originalArgs[1]
                : (String) originalArgs[0];

        if (StringUtils.startsWith(containerName, S3Client.SMOCKIN_INTERNAL_UPDATE_CALL_PREFIX)) {

            return IntStream.range(0, originalArgs.length)
                    .mapToObj(index -> {

                        if (index == 0
                                && !CREATE_CONTAINER_IN_LOCATION_METHOD.equalsIgnoreCase(methodName)) {
                            return StringUtils.removeStart((String)originalArgs[index], S3Client.SMOCKIN_INTERNAL_UPDATE_CALL_PREFIX);
                        }

                        if (CREATE_CONTAINER_IN_LOCATION_METHOD.equalsIgnoreCase(methodName)
                                && index == 1) {
                            return StringUtils.removeStart((String)originalArgs[index], S3Client.SMOCKIN_INTERNAL_UPDATE_CALL_PREFIX);
                        }

                        return originalArgs[index];

                    })
                    .collect(Collectors.toList())
                    .toArray();
        }

        return originalArgs;
    }

    public Optional<Boolean> isCallInternal(final Object[] originalArgs,
                                  final String methodName) {
        logger.debug("isCallInternal called");

        if (!supportedInternalS3ClientUpdateMethods.contains(methodName)) {
            return Optional.empty();
        }

        final String containerName = (CREATE_CONTAINER_IN_LOCATION_METHOD.equalsIgnoreCase(methodName))
                ? (String) originalArgs[1]
                : (String) originalArgs[0];

        return Optional.of(StringUtils.startsWith(containerName, S3Client.SMOCKIN_INTERNAL_UPDATE_CALL_PREFIX));
    }

    @PostConstruct
    public void after() {

        supportedInternalS3ClientUpdateMethods = Arrays.asList(
            CREATE_CONTAINER_IN_LOCATION_METHOD
            , DELETE_CONTAINER_METHOD
            , CLEAR_CONTAINER_METHOD
            , PUT_BLOB_METHOD
            , REMOVE_BLOB_METHOD
            , COPY_BLOB_METHOD
        );

    }

}
