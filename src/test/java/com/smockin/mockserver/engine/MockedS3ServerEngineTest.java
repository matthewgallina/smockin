package com.smockin.mockserver.engine;

import com.smockin.admin.persistence.entity.S3Mock;
import com.smockin.admin.persistence.entity.S3MockDir;
import com.smockin.admin.persistence.entity.S3MockFile;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MockedS3ServerEngineTest {

    private MockedS3ServerEngine mockedS3ServerEngine;

    @Before
    public void setUp() {

        mockedS3ServerEngine = new MockedS3ServerEngine();
    }

    @Test
    public void extractBucketAndFilePathTest() {

        // Setup
        final S3Mock s3MockParent = new S3Mock("A", null, null);

        final S3MockDir s3MockDirLevel1 = new S3MockDir("B", s3MockParent);
        final S3MockDir s3MockDirLevel2 = new S3MockDir("C", s3MockDirLevel1);
        final S3MockDir s3MockDirLevel3 = new S3MockDir("D",  s3MockDirLevel2);

        final S3MockFile s3MockFile = new S3MockFile("foo.bar", null, "HelloWorld", s3MockDirLevel3);
        s3MockDirLevel3.getFiles().add(s3MockFile);

        // Test
        final Pair<String, String> fileInfo = mockedS3ServerEngine.extractBucketAndFilePath(s3MockFile.getName(), s3MockFile.getS3MockDir());

        // Assertions
        Assert.assertNotNull(fileInfo);
        Assert.assertEquals("A", fileInfo.getLeft());
        Assert.assertEquals("B/C/D/foo.bar", fileInfo.getRight());

    }

}