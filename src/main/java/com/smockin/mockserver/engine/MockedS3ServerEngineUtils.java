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
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jclouds.blobstore.domain.Blob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Transactional
public class MockedS3ServerEngineUtils {

    private final Logger logger = LoggerFactory.getLogger(MockedS3ServerEngineUtils.class);

    private List<String> supportedInternalS3ClientUpdateMethods; // i.e BlobStore update based methods based on the functions present in S3Client

    // Using hardcoded forward slash rather then File.separatorChar as not sure how this will behave on Windows machines
    public static final String SEPARATOR_CHAR = GeneralUtils.URL_PATH_SEPARATOR;

    private static final String APPLICATION_XDIRECTORY = "application/x-directory";

    private static final String CREATE_CONTAINER_IN_LOCATION_METHOD = "createContainerInLocation";
    private static final String DELETE_CONTAINER_METHOD = "deleteContainer";
    private static final String CLEAR_CONTAINER_METHOD = "clearContainer";
    private static final String PUT_BLOB_METHOD = "putBlob";
    private static final String REMOVE_BLOB_METHOD = "removeBlob";
    private static final String COPY_BLOB_METHOD = "copyBlob";
    private static final String CONTAINER_EXISTS_METHOD = "containerExists";
    private static final String DELETE_CONTAINER_IF_EMPTY_METHOD = "deleteContainerIfEmpty";
    private static final String REMOVE_BLOBS_METHOD = "removeBlobs";
    private static final String LIST_METHOD = "list";
    private static final String BLOB_BUILDER_METHOD = "blobBuilder";


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


    public void persistS3RemoteCall(final String methodName,
                                 final Object[] args) {

        logger.debug("persistS3RemoteCall called");

        if (logger.isDebugEnabled())
            logger.debug("Remote S3 Client > Method name: " + methodName);

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

            // TODO test this!

            final String containerName = (String)args[0];
            final Blob blob = (Blob) args[1];
            final String fileName = blob.getMetadata().getName();
            final String mimeType = blob.getPayload().getContentMetadata().getContentType();

            final S3Mock s3Mock = findS3MockByBucketName(containerName);

            if (APPLICATION_XDIRECTORY.equals(mimeType)) {
                createS3Dir(fileName, s3Mock);
                return;
            }

            Optional<String> content;

            try {

                content = GeneralUtils.convertInputStreamToString(blob.getPayload().openStream(), true);

                if (!content.isPresent()) {
                    logger.error("Error reading client's uploaded file");
                    return;
                }

            } catch (IOException ex) {
                logger.error("Error reading client's uploaded file", ex);
                return;
            }

            createS3DirsAndFile(fileName, mimeType, content.get(), s3Mock);

            handleS3Logging("Added file " + fileName + " to bucket '" + containerName + "'");

        } else if (REMOVE_BLOB_METHOD.equalsIgnoreCase(methodName)) {

            // TODO need to differentiate between a dir and file.

            final String containerName = (String)args[0];
            final String fullFilePathOrDir = (String)args[1];

            //
            // Remove Directory
            if (StringUtils.endsWith(fullFilePathOrDir, SEPARATOR_CHAR)) { // This a dir

                final String[] dirsSplitByPath = StringUtils.split(fullFilePathOrDir, SEPARATOR_CHAR);
                final String singleDirectory = (dirsSplitByPath.length > 1)
                        ? dirsSplitByPath[dirsSplitByPath.length - 1]
                        : fullFilePathOrDir;

                final List<S3MockDir> dirs = s3MockDirDAO.findAllByName(StringUtils.removeEnd(singleDirectory, SEPARATOR_CHAR));

                if (dirs.isEmpty()) {
                    logger.error(String.format("Error unable to locate the dir %s in container %s to delete from DB. (No dirs found)", fullFilePathOrDir, containerName));
                    return;
                }

                final S3MockDir s3MockDir = findS3MockDir(fullFilePathOrDir, dirs, containerName);

                if (s3MockDir == null) {
                    logger.error(String.format("Error unable to locate the dir %s in container %s to delete from DB. (dir match not made)", fullFilePathOrDir, containerName));
                    return;
                }

                s3MockDirDAO.delete(s3MockDir);

                handleS3Logging(String.format("Removed dir %s from bucket '%s'", fullFilePathOrDir, containerName));

                return;
            }

            //
            // Remove File
            final String[] paths = StringUtils.split(fullFilePathOrDir, SEPARATOR_CHAR);
            final String fileName = paths[ paths.length -1 ];

            final List<S3MockFile> files = s3MockFileDAO.findAllByName(fileName);

            if (files.isEmpty()) {
                logger.error(String.format("Error unable to locate the file %s in container %s to delete from DB. (No files found)", fullFilePathOrDir, containerName));
                return;
            }

            // TODO include 'fullFilePath' in this search...  MG check this done...
            final S3MockFile fromS3MockFile = findS3MockFile(fullFilePathOrDir, files, containerName);

            if (fromS3MockFile == null) {
                logger.error(String.format("Error unable to locate the file %s in container %s to delete from DB. (file match not made)", fullFilePathOrDir, containerName));
                return;
            }

            s3MockFileDAO.delete(fromS3MockFile);

            handleS3Logging(String.format("Removed file %s from bucket '%s'", fullFilePathOrDir, containerName));

        } else if (COPY_BLOB_METHOD.equalsIgnoreCase(methodName)) {

            // NOTE, copyBlob seems to just handle the file during a dir rename.
            // TODO test this!

            final String fromContainer = (String)args[0];
            final String fromName = (String) args[1];
            final String toContainer = (String) args[2];
            final String toName = (String) args[3];
//            final CopyOptions options = (CopyOptions) args[4];

            final String[] fromPaths = StringUtils.split(fromName, SEPARATOR_CHAR);
            final String fromFileName = fromPaths[ fromPaths.length -1 ];

            final List<S3MockFile> fromFiles = s3MockFileDAO.findAllByName(fromFileName);

            if (fromFiles.isEmpty()) {
                logger.error("Error unable to locate (from DB) file to copy:" + fromName);
                return;
            }

            final S3MockFile fromS3MockFile = findS3MockFile(fromName, fromFiles, fromContainer);

            if (fromS3MockFile == null) {
                logger.error(String.format("Error unable to locate source file %s from container %s to copy", fromName, fromContainer));
                return;
            }

            final S3Mock destinationBucket = findS3MockByBucketName(toContainer);

            createS3DirsAndFile(toName, fromS3MockFile.getMimeType(), fromS3MockFile.getContent(), destinationBucket);

            handleS3Logging("Copied file " + fromName + " from bucket '" + fromContainer + "' into bucket '" + toContainer + "'");

        }

    }

    S3MockFile findS3MockFile(final String expectedPath,
                                      final List<S3MockFile> fromFiles,
                                      final String fromContainer) {

        if (fromFiles.size() == 1) {

            return locateFileForBucket(expectedPath, fromFiles.get(0), fromContainer);

        } else {

            for (final S3MockFile s3MockFile : fromFiles) {

                final S3MockFile fromS3MockFile = locateFileForBucket(expectedPath, s3MockFile, fromContainer);

                if (fromS3MockFile != null) {
                    return fromS3MockFile;
                }
            }
        }

        return null;
    }

    S3MockFile locateFileForBucket(final String expectedPath,
                                   final S3MockFile s3MockFile,
                                   final String container) {

        final S3Mock bucket = s3MockFile.getS3Mock();

        if (bucket != null
                && StringUtils.equals(container, bucket.getBucketName())) {

            return s3MockFile;
        }

        if (bucket == null) {

            final S3MockDir dir = s3MockFile.getS3MockDir();

            if (dir != null) {

                final StringBuilder filePathTracer = new StringBuilder();
                final S3Mock fromBucket = locateParentBucket(filePathTracer, dir);

                if (fromBucket != null
                        && StringUtils.equals(container, fromBucket.getBucketName())
                        && StringUtils.equals(filePathTracer.toString() + s3MockFile.getName(), expectedPath)) {

                    return s3MockFile;
                }

            }

        }

        return null;
    }

    S3MockDir findS3MockDir(final String expectedPath,
                            final List<S3MockDir> dirs,
                            final String fromContainer) {

        if (dirs.size() == 1) {

            return locateDirForBucket(expectedPath, dirs.get(0), fromContainer);

        } else {

            for (final S3MockDir s3MockDir : dirs) {

                final S3MockDir fromS3MockDir = locateDirForBucket(expectedPath, s3MockDir, fromContainer);

                if (fromS3MockDir != null) {
                    return fromS3MockDir;
                }
            }
        }

        return null;
    }

    S3MockDir locateDirForBucket(final String expectedPath,
                                 final S3MockDir s3MockDir,
                                 final String container) {

        final S3Mock bucket = s3MockDir.getS3Mock();

        if (bucket != null
                && StringUtils.equals(container, bucket.getBucketName())) {

            return s3MockDir;
        }

        if (bucket == null) {

            final S3MockDir dir = s3MockDir.getParent();

            if (dir != null) {

                final StringBuilder filePathTracer = new StringBuilder();

                final S3Mock fromBucket = locateParentBucket(filePathTracer, dir);

                if (fromBucket != null
                        && StringUtils.equals(container, fromBucket.getBucketName())
                        && StringUtils.equals(filePathTracer.toString() + s3MockDir.getName(), sanitiseSeparatorSuffix(expectedPath))) {

                    return s3MockDir;
                }

            }

        }

        return null;
    }

    String sanitiseSeparatorSuffix(final String value) {

        return ((StringUtils.endsWith(value, SEPARATOR_CHAR))
                ? StringUtils.removeEnd(value, SEPARATOR_CHAR)
                : value);
    }

    void createS3Dir(final String fullDirPath,
                     final S3Mock s3Mock) {

        final String[] paths = StringUtils.split(fullDirPath, SEPARATOR_CHAR);

        // Just the dir to save in the root of the bucket
        if (paths.length == 1) {

            final S3MockDir s3MockDir = new S3MockDir();
            s3MockDir.setName(paths[0]);
            s3MockDir.setS3Mock(s3Mock);
            s3MockDirDAO.save(s3MockDir);
            return;
        }

        // Save dir tree and dir
//        final String[] dirPaths = Arrays.copyOf(paths, paths.length - 1);
        handleSubDirsCreation(paths, s3Mock);
    }

    void createS3DirsAndFile(final String fullFilePath,
                        final String mimeType,
                        final String content,
                        final S3Mock s3Mock) {

        final String[] paths = StringUtils.split(fullFilePath, SEPARATOR_CHAR);

        // Just the file to save in the root of the bucket
        if (paths.length == 1) {
            saveS3MockFile(fullFilePath, mimeType, content, s3Mock, null);
            return;
        }

        // Save dir tree
        final String[] dirPaths = Arrays.copyOf(paths, paths.length - 1);
        S3MockDir parentS3MockDir = handleSubDirsCreation(dirPaths, s3Mock);

        // Save file
        final String fileName = paths[paths.length - 1];
        saveS3MockFile(fileName, mimeType, content, null, parentS3MockDir);

    }

    S3MockDir handleSubDirsCreation(final String[] paths,
                                    final S3Mock s3Mock) {

        int index = 0;
        S3MockDir parentS3MockDir = null;

        for (String p : paths) {

            // Check if directory already exists so as to not duplicate it
            if (index == 0) {

                // check sub dirs in bucket root
                final Optional<S3MockDir> dirOpt = s3Mock
                        .getChildrenDirs()
                        .stream()
                        .filter(d ->
                                d.getName().equals(p))
                        .findFirst();

                if (dirOpt.isPresent()) {
                    parentS3MockDir = dirOpt.get();
                    index++;
                    continue;
                }

            } else if (parentS3MockDir != null) {

                // check sub dirs in current directory
                final Optional<S3MockDir> dirOpt = parentS3MockDir
                        .getChildren()
                        .stream()
                        .filter(d ->
                                d.getName().equals(p))
                        .findFirst();

                if (dirOpt.isPresent()) {
                    parentS3MockDir = dirOpt.get();
                    index++;
                    continue;
                }

            }

            // Create directory
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

        return parentS3MockDir;
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


        if (originalArgs == null || originalArgs.length == 0) {
            return originalArgs;
        }

        return IntStream
                    .range(0, originalArgs.length)
                    .mapToObj(index -> {

                        final Object arg = originalArgs[index];

                        if (arg instanceof String
                                && StringUtils.startsWith((String)arg, S3Client.SMOCKIN_INTERNAL_UPDATE_CALL_PREFIX)) {

                            return StringUtils.removeStart((String)arg, S3Client.SMOCKIN_INTERNAL_UPDATE_CALL_PREFIX);
                        }

                        return arg;
                    })
                    .collect(Collectors.toList())
                    .toArray();


        /*
        final String containerName = (CREATE_CONTAINER_IN_LOCATION_METHOD.equalsIgnoreCase(methodName))
                ? (String) originalArgs[1]
                : (String) originalArgs[0];


        if (StringUtils.startsWith(containerName, S3Client.SMOCKIN_INTERNAL_UPDATE_CALL_PREFIX)) {

            return IntStream
                    .range(0, originalArgs.length)
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
        */
    }

    public Optional<Boolean> isCallInternal(final Object[] originalArgs,
                                            final String methodName) {
        logger.debug("isCallInternal called");

        if (!supportedInternalS3ClientUpdateMethods.contains(methodName)) {
            return Optional.empty();
        }

        if (originalArgs == null || originalArgs.length == 0) {
            return Optional.empty();
        }

        final boolean matchMade = IntStream
            .range(0, originalArgs.length)
            .anyMatch(i -> {
                final Object arg = originalArgs[i];
                return (arg instanceof String)
                            && StringUtils.startsWith((String)arg, S3Client.SMOCKIN_INTERNAL_UPDATE_CALL_PREFIX);
            });

        return Optional.of(matchMade);
    }

    public S3Mock locateParentBucket(final S3MockDir s3MockDir) {

        return locateParentBucket(null, s3MockDir);
    }

    public S3Mock locateParentBucket(final StringBuilder filePathTracer,
                                     final S3MockDir s3MockDir) {

        if (filePathTracer != null) {
            filePathTracer.insert(0, SEPARATOR_CHAR);
            filePathTracer.insert(0, s3MockDir.getName());
        }

        if (s3MockDir.getS3Mock() != null) {
            return s3MockDir.getS3Mock();
        }

        return locateParentBucket(filePathTracer, s3MockDir.getParent());
    }

    public void initBucketContent(final S3Client s3Client,
                                  final S3Mock bucket) {
        logger.debug("initBucketContent called");

        initBucketContent(s3Client, Arrays.asList(bucket));
    }

    public void initBucketContent(final S3Client s3Client,
                                  final List<S3Mock> buckets) {
        logger.debug("initBucketContent called");

        // Create all buckets
        buckets.forEach(m ->
                s3Client.createBucket(m.getBucketName()));

        // Create bucket files
        buckets.forEach(m ->
                addBucketFiles(s3Client, m));

        // Start adding bucket dir files
        buckets.forEach(m ->
                commenceAddingBucketDirs(s3Client, m));

    }

    public void addBucketFiles(final S3Client s3Client,
                               final S3Mock bucket) {

        bucket
                .getFiles()
                .forEach(f ->
                        s3Client.uploadObject(
                                        bucket.getBucketName(),
                                        f.getName(),
                                        IOUtils.toInputStream(f.getContent(), Charset.defaultCharset()),
                                        f.getMimeType()));

    }

    public void commenceAddingBucketDirs(final S3Client s3Client,
                                         final S3Mock bucket) {

        bucket.getChildrenDirs()
                .forEach(d ->
                        addDirFiles(bucket, s3Client, d));

    }

    void addDirFiles(final S3Mock bucket,
                     final S3Client s3Client,
                     final S3MockDir s3MockDir) {

        // Create dir
        final StringBuilder filePathTracer = new StringBuilder();
        locateParentBucket(filePathTracer, s3MockDir);
        s3Client.createSubDirectory(bucket.getBucketName(), filePathTracer.toString());

        // Create files in this dir
        s3MockDir
                .getFiles()
                .forEach(f -> {

                    final Pair<String, String> bucketAndFilePath = extractBucketAndFilePath(f);

                    s3Client.uploadObject(
                                    bucket.getBucketName(),
                                    bucketAndFilePath.getRight(),
                                    IOUtils.toInputStream(f.getContent(), Charset.defaultCharset()),
                                    f.getMimeType());

                });

        // Traverse child dirs
        s3MockDir.getChildren()
                .forEach(d ->
                        addDirFiles(bucket, s3Client, d));

    }

    public Pair<String, String> extractBucketAndFilePath(final S3MockFile s3MockFile) {

        final MutablePair<String, StringBuilder> collectedData
                = new MutablePair<>(null, new StringBuilder(s3MockFile.getName()));

        buildFilePathSegment(s3MockFile.getS3MockDir(), collectedData);

        return new MutablePair<>(collectedData.getLeft(),
                collectedData.getRight().toString());
    }

    void buildFilePathSegment(final S3MockDir s3MockDir,
                              final MutablePair<String, StringBuilder> fileInfo) {

        // Add dir segment
        fileInfo.getRight().insert(0, SEPARATOR_CHAR);
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

    public S3Client buildS3Client(final int port) {

        return new S3Client(
                GeneralUtils.S3_HOST,
                port);
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
            , CONTAINER_EXISTS_METHOD
            , DELETE_CONTAINER_IF_EMPTY_METHOD
            , REMOVE_BLOBS_METHOD
            , LIST_METHOD
            , BLOB_BUILDER_METHOD
        );

    }

}
