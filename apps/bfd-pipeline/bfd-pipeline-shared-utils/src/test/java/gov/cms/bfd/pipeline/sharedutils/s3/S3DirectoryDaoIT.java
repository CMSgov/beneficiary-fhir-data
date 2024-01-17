package gov.cms.bfd.pipeline.sharedutils.s3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.common.base.Strings;
import gov.cms.bfd.AbstractLocalStackTest;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.regions.Region;

/** Integration test for {@link S3DirectoryDao}. */
class S3DirectoryDaoIT extends AbstractLocalStackTest {
  /** Provides S3 access during testing. */
  private S3Dao s3Dao;

  /** Creates the {@link S3Dao} and a bucket for use in tests. */
  @BeforeEach
  void createDao() {
    s3Dao =
        new AwsS3ClientFactory(
                S3ClientConfig.s3Builder()
                    .region(Region.of(localstack.getRegion()))
                    .endpointOverride(localstack.getEndpoint())
                    .accessKey(localstack.getAccessKey())
                    .secretKey(localstack.getSecretKey())
                    .build())
            .createS3Dao();
  }

  /** Deletes bucket and closes the {@link S3Dao} after each test. */
  @AfterEach
  void closeDao() {
    s3Dao.close();
  }

  /** Verify that quotes are stripped properly from eTag value. */
  @Test
  void testETagNormalization() {
    assertEquals("a", S3DirectoryDao.normalizeEtag("a"));
    assertEquals("a", S3DirectoryDao.normalizeEtag("\"a\""));

    assertEquals("abc-1", S3DirectoryDao.normalizeEtag("abc-1"));
    assertEquals("abc-1", S3DirectoryDao.normalizeEtag("\"abc-1\""));
  }

  /**
   * Tests all basic operations of the {@link S3DirectoryDao} when operating without recursion.
   * Uploads and accesses data to a bucket and verifies that cached files are managed as expected.
   *
   * @throws Exception pass through
   */
  @Test
  public void testBasicOperations() throws Exception {
    String s3Bucket = null;
    S3DirectoryDao directoryDao = null;
    Path cacheDirectoryPath;
    try {
      s3Bucket = s3Dao.createTestBucket();
      final String s3Directory = "files-go-here/";
      cacheDirectoryPath = Files.createTempDirectory("test");
      directoryDao =
          new S3DirectoryDao(s3Dao, s3Bucket, s3Directory, cacheDirectoryPath, true, false);

      // no files in the bucket yet
      assertEquals(List.of(), directoryDao.readFileNames());

      // add a couple of files
      String aTag1 = uploadFileToBucket(s3Bucket, s3Directory + "a.txt", "AAA-1");
      String bTag1 = uploadFileToBucket(s3Bucket, s3Directory + "b.txt", "BBB-1");

      // these files will be ignored because recursive flag was false
      uploadFileToBucket(s3Bucket, s3Directory + "x/c.txt", "CCC-1");
      uploadFileToBucket(s3Bucket, s3Directory + "x/y/d.txt", "DDD-1");

      // now the files show up in the list
      assertEquals(
          List.of("a.txt", "b.txt"),
          directoryDao.readFileNames().stream().sorted().collect(Collectors.toList()));

      // no files in cache yet because we have not downloaded the files
      assertDoesNotExist(directoryDao.cacheFilePath("a.txt", aTag1));
      assertDoesNotExist(directoryDao.cacheFilePath("b.txt", bTag1));

      // download and verify the file contents
      assertEquals(
          "AAA-1", directoryDao.downloadFile("a.txt").asCharSource(StandardCharsets.UTF_8).read());
      assertEquals(
          "BBB-1", directoryDao.downloadFile("b.txt").asCharSource(StandardCharsets.UTF_8).read());

      // now that we've download the files we should see them in the cache
      assertFileExists(directoryDao.cacheFilePath("a.txt", aTag1));
      assertFileExists(directoryDao.cacheFilePath("b.txt", bTag1));

      // update one of the files so it has new contents and new eTag
      String aTag2 = uploadFileToBucket(s3Bucket, s3Directory + "a.txt", "AAA-2");
      assertNotEquals(aTag2, aTag1);

      // download and verify the updated file contents
      assertEquals(
          "AAA-2", directoryDao.downloadFile("a.txt").asCharSource(StandardCharsets.UTF_8).read());
      assertEquals(
          "BBB-1", directoryDao.downloadFile("b.txt").asCharSource(StandardCharsets.UTF_8).read());

      // now we have two files for a.txt
      assertFileExists(directoryDao.cacheFilePath("a.txt", aTag1));
      assertFileExists(directoryDao.cacheFilePath("a.txt", aTag2));
      assertFileExists(directoryDao.cacheFilePath("b.txt", bTag1));

      // delete obsolete files and verify the first version of a.txt is now gone
      assertEquals(1, directoryDao.deleteObsoleteFiles());
      assertDoesNotExist(directoryDao.cacheFilePath("a.txt", aTag1));
      assertFileExists(directoryDao.cacheFilePath("a.txt", aTag2));
      assertFileExists(directoryDao.cacheFilePath("b.txt", bTag1));

      // delete all files and verify that they no longer exist
      assertEquals(2, directoryDao.deleteAllFiles());
      assertDoesNotExist(directoryDao.cacheFilePath("a.txt", aTag1));
      assertDoesNotExist(directoryDao.cacheFilePath("a.txt", aTag2));
      assertDoesNotExist(directoryDao.cacheFilePath("b.txt", bTag1));
    } finally {
      s3Dao.deleteTestBucket(s3Bucket);
      if (directoryDao != null) {
        directoryDao.close();
      }
    }
  }

  /**
   * Tests all basic operations of the {@link S3DirectoryDao} when operating in recursive mode.
   * Uploads and accesses data to a bucket and verifies that cached files are managed as expected.
   *
   * @throws Exception pass through
   */
  @Test
  public void testRecursiveOperations() throws Exception {
    String s3Bucket = null;
    S3DirectoryDao directoryDao = null;
    Path cacheDirectoryPath;
    try {
      s3Bucket = s3Dao.createTestBucket();
      final String s3Directory = "files-go-here/";
      cacheDirectoryPath = Files.createTempDirectory("test");
      directoryDao =
          new S3DirectoryDao(s3Dao, s3Bucket, s3Directory, cacheDirectoryPath, true, true);

      // no files in the bucket yet
      assertEquals(List.of(), directoryDao.readFileNames());

      final String xcName = "x/c.txt";
      final String xydName = "x/y/d.txt";

      // add a couple of files
      String aTag1 = uploadFileToBucket(s3Bucket, s3Directory + "a.txt", "AAA-1");
      String bTag1 = uploadFileToBucket(s3Bucket, s3Directory + "b.txt", "BBB-1");
      String xcTag1 = uploadFileToBucket(s3Bucket, s3Directory + xcName, "CCC-1");
      String xydTag1 = uploadFileToBucket(s3Bucket, s3Directory + xydName, "DDD-1");

      // now the files show up in the list
      assertEquals(
          List.of("a.txt", "b.txt", xcName, xydName),
          directoryDao.readFileNames().stream().sorted().collect(Collectors.toList()));

      // no files in cache yet because we have not downloaded the files
      assertDoesNotExist(directoryDao.cacheFilePath("a.txt", aTag1));
      assertDoesNotExist(directoryDao.cacheFilePath("b.txt", bTag1));
      assertDoesNotExist(directoryDao.cacheFilePath(xcName, xcTag1));
      assertDoesNotExist(directoryDao.cacheFilePath(xydName, xydTag1));

      // download and verify the file contents
      assertEquals(
          "AAA-1", directoryDao.downloadFile("a.txt").asCharSource(StandardCharsets.UTF_8).read());
      assertEquals(
          "BBB-1", directoryDao.downloadFile("b.txt").asCharSource(StandardCharsets.UTF_8).read());
      assertEquals(
          "CCC-1", directoryDao.downloadFile(xcName).asCharSource(StandardCharsets.UTF_8).read());
      assertEquals(
          "DDD-1", directoryDao.downloadFile(xydName).asCharSource(StandardCharsets.UTF_8).read());

      // now that we've download the files we should see them in the cache
      assertFileExists(directoryDao.cacheFilePath("a.txt", aTag1));
      assertFileExists(directoryDao.cacheFilePath("b.txt", bTag1));

      // update two of the files so it has new contents and new eTag
      String aTag2 = uploadFileToBucket(s3Bucket, s3Directory + "a.txt", "AAA-2");
      assertNotEquals(aTag2, aTag1);
      String xydTag2 = uploadFileToBucket(s3Bucket, s3Directory + xydName, "DDD-2");
      assertNotEquals(xydTag2, xydTag1);

      // download and verify the updated file contents
      assertEquals(
          "AAA-2", directoryDao.downloadFile("a.txt").asCharSource(StandardCharsets.UTF_8).read());
      assertEquals(
          "BBB-1", directoryDao.downloadFile("b.txt").asCharSource(StandardCharsets.UTF_8).read());
      assertEquals(
          "CCC-1", directoryDao.downloadFile(xcName).asCharSource(StandardCharsets.UTF_8).read());
      assertEquals(
          "DDD-2", directoryDao.downloadFile(xydName).asCharSource(StandardCharsets.UTF_8).read());

      // now we have two files for a.txt
      assertFileExists(directoryDao.cacheFilePath("a.txt", aTag1));
      assertFileExists(directoryDao.cacheFilePath("a.txt", aTag2));
      assertFileExists(directoryDao.cacheFilePath("b.txt", bTag1));
      assertFileExists(directoryDao.cacheFilePath(xcName, xcTag1));
      assertFileExists(directoryDao.cacheFilePath(xydName, xydTag2));

      // delete obsolete files and verify the first version of a.txt is now gone
      assertEquals(2, directoryDao.deleteObsoleteFiles());
      assertDoesNotExist(directoryDao.cacheFilePath("a.txt", aTag1));
      assertFileExists(directoryDao.cacheFilePath("a.txt", aTag2));
      assertFileExists(directoryDao.cacheFilePath("b.txt", bTag1));
      assertFileExists(directoryDao.cacheFilePath(xcName, xcTag1));
      assertDoesNotExist(directoryDao.cacheFilePath(xydName, xydTag1));
      assertFileExists(directoryDao.cacheFilePath(xydName, xydTag2));

      // delete all files and verify that they no longer exist
      assertEquals(4, directoryDao.deleteAllFiles());
      assertDoesNotExist(directoryDao.cacheFilePath("a.txt", aTag1));
      assertDoesNotExist(directoryDao.cacheFilePath("a.txt", aTag2));
      assertDoesNotExist(directoryDao.cacheFilePath("b.txt", bTag1));
      assertDoesNotExist(directoryDao.cacheFilePath(xcName, xcTag1));
      assertDoesNotExist(directoryDao.cacheFilePath(xydName, xydTag1));
      assertDoesNotExist(directoryDao.cacheFilePath(xydName, xydTag2));
    } finally {
      s3Dao.deleteTestBucket(s3Bucket);
      if (directoryDao != null) {
        directoryDao.close();
      }
    }
  }

  /**
   * Verify that closing a {@link S3DirectoryDao} deletes the cache directory and its contents when
   * delete on close is set and dao is closed.
   *
   * @throws Exception pass through
   */
  @Test
  public void testDeleteOnClose() throws Exception {
    String s3Bucket = null;
    S3DirectoryDao directoryDao = null;
    Path cacheDirectoryPath;
    String aTag1;
    String bTag1;
    try {
      s3Bucket = s3Dao.createTestBucket();
      final String s3Directory = "";
      cacheDirectoryPath = Files.createTempDirectory("test");
      directoryDao =
          new S3DirectoryDao(s3Dao, s3Bucket, s3Directory, cacheDirectoryPath, true, false);

      // add a couple of files
      aTag1 = uploadFileToBucket(s3Bucket, s3Directory + "a.txt", "AAA-1");
      bTag1 = uploadFileToBucket(s3Bucket, s3Directory + "b.txt", "BBB-1");

      // verify the file contents
      assertEquals(
          "AAA-1", directoryDao.downloadFile("a.txt").asCharSource(StandardCharsets.UTF_8).read());
      assertEquals(
          "BBB-1", directoryDao.downloadFile("b.txt").asCharSource(StandardCharsets.UTF_8).read());

      // now that we've download the files we should see them in the cache
      assertFileExists(directoryDao.cacheFilePath("a.txt", aTag1));
      assertFileExists(directoryDao.cacheFilePath("b.txt", bTag1));

    } finally {
      s3Dao.deleteTestBucket(s3Bucket);
      if (directoryDao != null) {
        directoryDao.close();
      }
    }

    // close should have deleted the files and the directory
    assertDoesNotExist(directoryDao.cacheFilePath("a.txt", aTag1));
    assertDoesNotExist(directoryDao.cacheFilePath("b.txt", bTag1));
    assertDoesNotExist(cacheDirectoryPath);
  }

  /**
   * Verify that closing a {@link S3DirectoryDao} does not delete the cache directory or its
   * contents when closed if the delete on close flag is not set.
   *
   * @throws Exception pass through
   */
  @Test
  public void testCloseDeletesNothingWhenFlagNotTrue() throws Exception {
    String s3Bucket = null;
    S3DirectoryDao directoryDao = null;
    Path cacheDirectoryPath;
    String aTag1;
    String bTag1;
    try {
      s3Bucket = s3Dao.createTestBucket();
      final String s3Directory = "files-go-here/";
      cacheDirectoryPath = Files.createTempDirectory("test");
      directoryDao =
          new S3DirectoryDao(s3Dao, s3Bucket, s3Directory, cacheDirectoryPath, false, false);

      // add a couple of files
      aTag1 = uploadFileToBucket(s3Bucket, s3Directory + "a.txt", "AAA-1");
      bTag1 = uploadFileToBucket(s3Bucket, s3Directory + "b.txt", "BBB-1");

      // verify the file contents
      assertEquals(
          "AAA-1", directoryDao.downloadFile("a.txt").asCharSource(StandardCharsets.UTF_8).read());
      assertEquals(
          "BBB-1", directoryDao.downloadFile("b.txt").asCharSource(StandardCharsets.UTF_8).read());

      // now that we've download the files we should see them in the cache
      assertFileExists(directoryDao.cacheFilePath("a.txt", aTag1));
      assertFileExists(directoryDao.cacheFilePath("b.txt", bTag1));

    } finally {
      s3Dao.deleteTestBucket(s3Bucket);
      if (directoryDao != null) {
        directoryDao.close();
      }
    }

    // close should NOT have deleted the files or the directory
    assertFileExists(directoryDao.cacheFilePath("a.txt", aTag1));
    assertFileExists(directoryDao.cacheFilePath("b.txt", bTag1));
    assertDirectoryExists(cacheDirectoryPath);

    // clean up
    Files.delete(directoryDao.cacheFilePath("a.txt", aTag1));
    Files.delete(directoryDao.cacheFilePath("b.txt", bTag1));
    Files.delete(cacheDirectoryPath);
  }

  /**
   * Verify that S3 object not found throws {@link FileNotFoundException}.
   *
   * @throws Exception pass through if anything fails
   */
  @Test
  public void testGetObjectMetaDataForMissingFile() throws Exception {
    String s3Bucket = null;
    try {
      s3Bucket = s3Dao.createTestBucket();
      final String s3Directory = "";
      try (var directoryDao =
          new S3DirectoryDao(
              s3Dao, s3Bucket, s3Directory, Files.createTempDirectory("test"), true, false)) {
        assertThrows(
            FileNotFoundException.class,
            () -> {
              directoryDao.downloadFile("a.txt");
            });
      }
    } finally {
      s3Dao.deleteTestBucket(s3Bucket);
    }
  }

  /**
   * Upload the string as a "file" to the s3 bucket and return the ETag assigned to it by S3.
   *
   * @param bucket the bucket to receive the file
   * @param objectKey the key for the object
   * @param fileData a string uploaded as a file
   * @return eTag assigned to the file by S3
   */
  private String uploadFileToBucket(String bucket, String objectKey, String fileData) {
    S3Dao.S3ObjectSummary putResponse =
        s3Dao.putObject(bucket, objectKey, fileData.getBytes(StandardCharsets.UTF_8), Map.of());
    Assertions.assertFalse(
        Strings.isNullOrEmpty(putResponse.getETag()), "eTag should be non-empty");

    S3Dao.S3ObjectDetails readResponse = s3Dao.readObjectMetaData(bucket, objectKey);
    assertEquals(putResponse.getETag(), readResponse.getETag());
    return readResponse.getETag();
  }

  /**
   * Shortcut for asserting a directory exists.
   *
   * @param path path of the file
   */
  private void assertDirectoryExists(Path path) {
    if (!Files.isDirectory(path)) {
      fail("expected directory to exist: " + path);
    }
  }

  /**
   * Shortcut for asserting a file exists.
   *
   * @param path path of the file
   */
  private void assertFileExists(Path path) {
    if (!Files.isRegularFile(path)) {
      fail("expected file to exist: " + path);
    }
  }

  /**
   * Shortcut for asserting a file exists.
   *
   * @param path path of the file
   */
  private void assertDoesNotExist(Path path) {
    if (Files.exists(path)) {
      fail("expected file to not exist: " + path);
    }
  }
}
