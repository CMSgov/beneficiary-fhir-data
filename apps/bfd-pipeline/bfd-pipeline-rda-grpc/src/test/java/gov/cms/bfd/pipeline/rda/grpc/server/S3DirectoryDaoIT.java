package gov.cms.bfd.pipeline.rda.grpc.server;

import static gov.cms.bfd.pipeline.sharedutils.s3.SharedS3Utilities.REGION_DEFAULT;
import static gov.cms.bfd.pipeline.sharedutils.s3.SharedS3Utilities.createS3Client;
import static gov.cms.bfd.pipeline.sharedutils.s3.SharedS3Utilities.createTestBucket;
import static gov.cms.bfd.pipeline.sharedutils.s3.SharedS3Utilities.deleteTestBucket;
import static gov.cms.bfd.pipeline.sharedutils.s3.SharedS3Utilities.uploadJsonToBucket;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;
import com.google.common.io.ByteSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Integration test for {@link S3DirectoryDao}. */
public class S3DirectoryDaoIT {
  /**
   * Tests all basic operations of the {@link S3DirectoryDao}. Uploads and accesses data to a bucket
   * and verifies that cached files are managed as expected.
   *
   * @throws Exception pass through
   */
  @Test
  public void testBasicOperations() throws Exception {
    AmazonS3 s3Client = createS3Client(REGION_DEFAULT);
    Bucket s3Bucket = null;
    S3DirectoryDao s3Dao = null;
    Path cacheDirectoryPath;
    try {
      s3Bucket = createTestBucket(s3Client);
      final String s3Directory = "files-go-here/";
      cacheDirectoryPath = Files.createTempDirectory("test");
      s3Dao =
          new S3DirectoryDao(s3Client, s3Bucket.getName(), s3Directory, cacheDirectoryPath, true);

      // no files in the bucket yet
      assertEquals(List.of(), s3Dao.readFileNames());

      // add a couple of files
      String aTag1 = uploadFileToBucket(s3Client, s3Bucket, s3Directory + "a.txt", "AAA-1");
      String bTag1 = uploadFileToBucket(s3Client, s3Bucket, s3Directory + "b.txt", "BBB-1");

      // now the files show up in the list
      assertEquals(
          List.of("a.txt", "b.txt"),
          s3Dao.readFileNames().stream().sorted().collect(Collectors.toList()));

      // no files in cache yet because we have not downloaded the files
      assertDoesNotExist(s3Dao.dataFilePath("a.txt", aTag1));
      assertDoesNotExist(s3Dao.dataFilePath("b.txt", bTag1));

      // verify the file contents
      assertEquals(
          "AAA-1", s3Dao.downloadFile("a.txt").asCharSource(StandardCharsets.UTF_8).read());
      assertEquals(
          "BBB-1", s3Dao.downloadFile("b.txt").asCharSource(StandardCharsets.UTF_8).read());

      // now that we've download the files we should see them in the cache
      assertFileExists(s3Dao.dataFilePath("a.txt", aTag1));
      assertFileExists(s3Dao.dataFilePath("b.txt", bTag1));

      // update one of the files so it has new contents and new eTag
      String aTag2 = uploadFileToBucket(s3Client, s3Bucket, s3Directory + "a.txt", "AAA-2");
      assertNotEquals(aTag2, aTag1);

      // verify the updated file contents
      assertEquals(
          "AAA-2", s3Dao.downloadFile("a.txt").asCharSource(StandardCharsets.UTF_8).read());
      assertEquals(
          "BBB-1", s3Dao.downloadFile("b.txt").asCharSource(StandardCharsets.UTF_8).read());

      // now we have two files for a.txt
      assertFileExists(s3Dao.dataFilePath("a.txt", aTag1));
      assertFileExists(s3Dao.dataFilePath("a.txt", aTag2));
      assertFileExists(s3Dao.dataFilePath("b.txt", bTag1));

      // delete obsolete files and verify the first version of a.txt is now gone
      assertEquals(1, s3Dao.deleteObsoleteFiles());
      assertDoesNotExist(s3Dao.dataFilePath("a.txt", aTag1));
      assertFileExists(s3Dao.dataFilePath("a.txt", aTag2));
      assertFileExists(s3Dao.dataFilePath("b.txt", bTag1));

      // delete all files and verify that they no longer exist
      assertEquals(2, s3Dao.deleteAllFiles());
      assertDoesNotExist(s3Dao.dataFilePath("a.txt", aTag1));
      assertDoesNotExist(s3Dao.dataFilePath("a.txt", aTag2));
      assertDoesNotExist(s3Dao.dataFilePath("b.txt", bTag1));
    } finally {
      deleteTestBucket(s3Client, s3Bucket);
      if (s3Dao != null) {
        s3Dao.close();
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
    AmazonS3 s3Client = createS3Client(REGION_DEFAULT);
    Bucket s3Bucket = null;
    S3DirectoryDao s3Dao = null;
    Path cacheDirectoryPath;
    String aTag1;
    String bTag1;
    try {
      s3Bucket = createTestBucket(s3Client);
      final String s3Directory = "";
      cacheDirectoryPath = Files.createTempDirectory("test");
      s3Dao =
          new S3DirectoryDao(s3Client, s3Bucket.getName(), s3Directory, cacheDirectoryPath, true);

      // add a couple of files
      aTag1 = uploadFileToBucket(s3Client, s3Bucket, s3Directory + "a.txt", "AAA-1");
      bTag1 = uploadFileToBucket(s3Client, s3Bucket, s3Directory + "b.txt", "BBB-1");

      // verify the file contents
      assertEquals(
          "AAA-1", s3Dao.downloadFile("a.txt").asCharSource(StandardCharsets.UTF_8).read());
      assertEquals(
          "BBB-1", s3Dao.downloadFile("b.txt").asCharSource(StandardCharsets.UTF_8).read());

      // now that we've download the files we should see them in the cache
      assertFileExists(s3Dao.dataFilePath("a.txt", aTag1));
      assertFileExists(s3Dao.dataFilePath("b.txt", bTag1));

    } finally {
      deleteTestBucket(s3Client, s3Bucket);
      if (s3Dao != null) {
        s3Dao.close();
      }
    }

    // close should have deleted the files and the directory
    assertDoesNotExist(s3Dao.dataFilePath("a.txt", aTag1));
    assertDoesNotExist(s3Dao.dataFilePath("b.txt", bTag1));
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
    AmazonS3 s3Client = createS3Client(REGION_DEFAULT);
    Bucket s3Bucket = null;
    S3DirectoryDao s3Dao = null;
    Path cacheDirectoryPath;
    String aTag1;
    String bTag1;
    try {
      s3Bucket = createTestBucket(s3Client);
      final String s3Directory = "files-go-here/";
      cacheDirectoryPath = Files.createTempDirectory("test");
      s3Dao =
          new S3DirectoryDao(s3Client, s3Bucket.getName(), s3Directory, cacheDirectoryPath, false);

      // add a couple of files
      aTag1 = uploadFileToBucket(s3Client, s3Bucket, s3Directory + "a.txt", "AAA-1");
      bTag1 = uploadFileToBucket(s3Client, s3Bucket, s3Directory + "b.txt", "BBB-1");

      // verify the file contents
      assertEquals(
          "AAA-1", s3Dao.downloadFile("a.txt").asCharSource(StandardCharsets.UTF_8).read());
      assertEquals(
          "BBB-1", s3Dao.downloadFile("b.txt").asCharSource(StandardCharsets.UTF_8).read());

      // now that we've download the files we should see them in the cache
      assertFileExists(s3Dao.dataFilePath("a.txt", aTag1));
      assertFileExists(s3Dao.dataFilePath("b.txt", bTag1));

    } finally {
      deleteTestBucket(s3Client, s3Bucket);
      if (s3Dao != null) {
        s3Dao.close();
      }
    }

    // close should have deleted the files and the directory
    assertFileExists(s3Dao.dataFilePath("a.txt", aTag1));
    assertFileExists(s3Dao.dataFilePath("b.txt", bTag1));
    assertDirectoryExists(cacheDirectoryPath);

    // clean up
    Files.delete(s3Dao.dataFilePath("a.txt", aTag1));
    Files.delete(s3Dao.dataFilePath("b.txt", bTag1));
    Files.delete(cacheDirectoryPath);
  }

  /**
   * Upload the string as a "file" to the s3 bucket and return the 3Tag assigned to it by S3.
   *
   * @param s3Client the {@link AmazonS3} client to use
   * @param bucket the bucket to receive the file
   * @param objectKey the key for the object
   * @param fileData a string uploaded as a file
   * @return eTag assigned to the file by S3
   * @throws IOException pass through if anything fails
   */
  private String uploadFileToBucket(
      AmazonS3 s3Client, Bucket bucket, String objectKey, String fileData) throws IOException {
    uploadJsonToBucket(
        s3Client,
        bucket.getName(),
        objectKey,
        ByteSource.wrap(fileData.getBytes(StandardCharsets.UTF_8)));
    var metaData = s3Client.getObjectMetadata(bucket.getName(), objectKey);
    assertNotNull(metaData);
    return metaData.getETag();
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
