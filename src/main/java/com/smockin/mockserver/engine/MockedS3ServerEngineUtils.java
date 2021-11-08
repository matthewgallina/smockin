package com.smockin.mockserver.engine;

import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.persistence.dao.S3MockDAO;
import com.smockin.admin.persistence.dao.S3MockDirDAO;
import com.smockin.admin.persistence.dao.S3MockFileDAO;
import com.smockin.admin.persistence.entity.S3Mock;
import com.smockin.admin.persistence.entity.S3MockDir;
import com.smockin.admin.persistence.entity.S3MockFile;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.service.SmockinUserService;
import com.smockin.admin.websocket.LiveLoggingHandler;
import com.smockin.mockserver.service.S3Client;
import com.smockin.utils.GeneralUtils;
import com.smockin.utils.LiveLoggingUtils;
import org.apache.commons.lang3.StringUtils;
import org.jclouds.blobstore.domain.Blob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.io.IOException;
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
    private S3MockFileDAO s3MockFileDAO;

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

            // todo test this!

            final String containerName = (String)args[0];
            final Blob blob = (Blob) args[1];
            final String fileName = blob.getMetadata().getName();
            final String mimeType = blob.getPayload().getContentMetadata().getContentType();

            Optional<String> content;

            try {

                content = GeneralUtils.convertInputStreamToString(blob.getPayload().openStream());

                if (!content.isPresent()) {
                    logger.error("Error reading client's uploaded file");
                    return;
                }

            } catch (IOException ex) {
                logger.error("Error reading client's uploaded file", ex);
                return;
            }

            final S3Mock s3Mock = findS3MockByBucketName(containerName);

            createS3DirsAndFile(fileName, mimeType, content.get(), s3Mock);

            handleS3Logging("Added file " + fileName + " to bucket '" + containerName + "'");

        } else if (REMOVE_BLOB_METHOD.equalsIgnoreCase(methodName)) {

            // todo test this!

            final String containerName = (String)args[0];
            final String fileName = (String) args[1];

            final List<S3MockFile> files = s3MockFileDAO.findAllByName(fileName);

            if (files.isEmpty()) {
                logger.error(String.format("Error unable to locate the file %s in container %s to delete from DB", fileName, containerName));
                return;
            }

            final S3MockFile fromS3MockFile = findS3MockFile(files, containerName);

            if (fromS3MockFile == null) {
                logger.error(String.format("Error unable to locate the file %s in container %s to delete from DB", fileName, containerName));
                return;
            }

            s3MockFileDAO.delete(fromS3MockFile);

            handleS3Logging("Removed file " + fileName + " from bucket '" + containerName + "'");

        } else if (COPY_BLOB_METHOD.equalsIgnoreCase(methodName)) {

            // todo test this!

            final String fromContainer = (String)args[0];
            final String fromName = (String) args[1];
            final String toContainer = (String) args[2];
            final String toName = (String) args[3];
//            final CopyOptions options = (CopyOptions) args[4];

            final List<S3MockFile> fromFiles = s3MockFileDAO.findAllByName(fromName);

            if (fromFiles.isEmpty()) {
                logger.error("Error unable to locate (from DB) file to copy:" + fromName);
                return;
            }

            final S3MockFile fromS3MockFile = findS3MockFile(fromFiles, fromContainer);

            if (fromS3MockFile == null) {
                logger.error(String.format("Error unable to locate source file %s from container %s to copy", fromName, fromContainer));
                return;
            }

            final S3Mock destinationBucket = findS3MockByBucketName(toContainer);

            createS3DirsAndFile(toName, fromS3MockFile.getMimeType(), fromS3MockFile.getContent(), destinationBucket);

            handleS3Logging("Copied file " + fromName + " from bucket '" + fromContainer + "' into bucket '" + toContainer + "'");

        }

    }

    private S3MockFile findS3MockFile(final List<S3MockFile> fromFiles,
                                      final String fromContainer) {

        if (fromFiles.size() == 1) {

            return locateFileForBucket(fromFiles.get(0), fromContainer);

        } else {

            for (final S3MockFile s3MockFile : fromFiles) {

                final S3MockFile fromS3MockFile = locateFileForBucket(s3MockFile, fromContainer);

                if (fromS3MockFile != null) {
                    return fromS3MockFile;
                }
            }
        }

        return null;
    }

    private S3MockFile locateFileForBucket(final S3MockFile s3MockFile,
                                           final String container) {

        final S3Mock bucket = s3MockFile.getS3Mock();

        if (bucket != null
                && StringUtils.equals(container, bucket.getBucketName())) {

            return s3MockFile;
        }

        if (bucket == null) {

            final S3MockDir dir = s3MockFile.getS3MockDir();

            if (dir != null) {

                final S3Mock fromBucket2 = locateParentBucket(dir);

                if (fromBucket2 != null
                        && StringUtils.equals(container, fromBucket2.getBucketName())) {

                    return s3MockFile;
                }
            }
        }

        return null;
    }

    void createS3DirsAndFile(final String fullFilePath,
                        final String mimeType,
                        final String content,
                        final S3Mock s3Mock) {

        final String[] paths = StringUtils.split(fullFilePath, "/");

        // Just the file to save in the root of the bucket
        if (paths.length == 1) {
            saveS3MockFile(fullFilePath, mimeType, content, s3Mock, null);
            return;
        }

        // Save dir tree
        int index = 0;
        S3MockDir parentS3MockDir = null;

        for (String p : paths) {

            if (index == (paths.length - 1)) {
                break;
            }

            final S3MockDir s3MockDir = new S3MockDir();
            s3MockDir.setName(p);

            if (index == 0) {
                s3MockDir.setS3Mock(s3Mock);
            } else if (parentS3MockDir != null) {
                s3MockDir.setParent(parentS3MockDir);
            }

            parentS3MockDir = s3MockDirDAO.save(s3MockDir);
            index++;
        }

        // Save file
        final String fileName = paths[paths.length - 1];
        saveS3MockFile(fileName, mimeType, content, null, parentS3MockDir);

    }

    void saveS3MockFile(final String fileName,
                        final String mimeType,
                        final String content,
                        final S3Mock s3Mock,
                        final S3MockDir parentS3MockDir) {

        final S3MockFile s3File = new S3MockFile();
        s3File.setName(fileName);
        s3File.setMimeType(mimeType);
        s3File.setContent(content);
        if (s3Mock != null)
            s3File.setS3Mock(s3Mock);
        if (parentS3MockDir != null)
            s3File.setS3MockDir(parentS3MockDir);

        s3MockFileDAO.save(s3File);
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

    public S3Mock locateParentBucket(final S3MockDir s3MockDir) {

        if (s3MockDir.getS3Mock() != null) {
            return s3MockDir.getS3Mock();
        }

        return locateParentBucket(s3MockDir.getParent());
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
