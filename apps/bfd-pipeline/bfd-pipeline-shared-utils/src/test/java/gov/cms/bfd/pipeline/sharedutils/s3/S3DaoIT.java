package gov.cms.bfd.pipeline.sharedutils.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.io.ByteStreams;
import gov.cms.bfd.AbstractLocalStackTest;
import gov.cms.bfd.pipeline.sharedutils.s3.S3Dao.S3ObjectDetails;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import gov.cms.bfd.sharedutils.exceptions.UncheckedIOException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ServiceClientConfiguration;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

/** Integration tests for {@link S3Dao}. */
class S3DaoIT extends AbstractLocalStackTest {
  /**
   * {@link S3TransferManager#copy} does not preserve meta data when using multi-part transfers. To
   * test with with reasonable overhead we need to reduce the partition size to the minimum allowed
   * value and try to transfer more than that.
   */
  private static final int MIN_PART_SIZE_FOR_TESTING = 8 * 1024 * 1024;

  /** Provides access to a sample file to be uploaded to a bucket. */
  private static final URL SAMPLE_FILE_FOR_PUT_TEST =
      ClassLoader.getSystemResource("data-for-bucket-test.txt");

  /** Object we're testing. */
  private S3Dao s3Dao;

  /** Name of bucket created for each test. */
  private String bucket;

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
                    .minimumPartSizeForDownload((long) MIN_PART_SIZE_FOR_TESTING)
                    .build())
            .createS3Dao();
    bucket = s3Dao.createTestBucket();
  }

  /** Deletes bucket and closes the {@link S3Dao} after each test. */
  @AfterEach
  void closeDao() {
    s3Dao.deleteTestBucket(bucket);
    s3Dao.close();
  }

  /** Verifies that creating and deleting buckets works correctly. */
  @Test
  void shouldManageBucketsCorrectly() {
    String bucket = s3Dao.createTestBucket();
    assertThat(bucket).isNotEmpty();
    try {
      // Verify that the bucket exists by verifying we can list its contents without thrown an
      // exception.
      var bucketContents = s3Dao.listObjects(bucket).toList();
      assertThat(bucketContents).isEmpty();
    } finally {
      s3Dao.deleteTestBucket(bucket);

      // Verify that the bucket no longer exists.
      assertThatThrownBy(() -> s3Dao.listObjects(bucket).toList())
          .isInstanceOf(NoSuchBucketException.class);
    }
  }

  /**
   * Verifies that we can use {@link S3Dao#putObject} to upload a file using a {@link URL} and that
   * reading the file and its meta data back produces the same bytes we uploaded.
   *
   * @throws IOException pass through
   */
  @Test
  void shouldPutAndDownloadObjectsCorrectlyUsingURL() throws IOException {
    final var objectKey = "url-object-key";
    final var metaData = Map.of("meta-data-1", "value-1", "meta-data-2", "value-2");
    final byte[] expectedBytes;
    try (var stream = SAMPLE_FILE_FOR_PUT_TEST.openStream()) {
      expectedBytes = ByteStreams.toByteArray(stream);
    }

    // we haven't uploaded the file yet
    assertEquals(false, s3Dao.objectExists(bucket, objectKey));

    // upload the file to the bucket using URL and verify the summary has expected values
    final var uploadSummary =
        s3Dao.putObject(bucket, objectKey, SAMPLE_FILE_FOR_PUT_TEST, metaData);
    assertEquals(objectKey, uploadSummary.getKey());
    assertThat(uploadSummary.getETag()).isNotEmpty();
    assertEquals(expectedBytes.length, uploadSummary.getSize());

    // we have uploaded the file now
    assertEquals(true, s3Dao.objectExists(bucket, objectKey));

    // read back details and verify they have expected values
    final var readMetaDetails = s3Dao.readObjectMetaData(bucket, objectKey);
    assertEquals(objectKey, readMetaDetails.getKey());
    assertEquals(expectedBytes.length, readMetaDetails.getSize());
    assertEquals(uploadSummary.getETag(), readMetaDetails.getETag());
    assertEquals(metaData, readMetaDetails.getMetaData());

    final Path tempFile = File.createTempFile("s3dao", "dat").toPath();
    try {
      // download the file and ensure it has expected values
      S3ObjectDetails downloadObjectDetails = s3Dao.downloadObject(bucket, objectKey, tempFile);
      assertEquals(readMetaDetails, downloadObjectDetails);

      // read back file contents and verify they match what we uploaded
      byte[] actualBytes = Files.readAllBytes(tempFile);
      assertThat(actualBytes).isEqualTo(expectedBytes);
    } finally {
      Files.delete(tempFile);
    }
  }

  /**
   * Verifies that we can use {@link S3Dao#putObject} to upload files using a byte array and that
   * reading the file back produces the same bytes we uploaded.
   *
   * @throws IOException pass through
   */
  @Test
  void shouldPutAndReadObjectsCorrectlyUsingByteArray() throws IOException {
    final var objectKey = "bytes-object-key";
    final var metaData = Map.of("meta-data-1", "value-1", "meta-data-2", "value-2");
    final byte[] expectedBytes;
    try (var stream = SAMPLE_FILE_FOR_PUT_TEST.openStream()) {
      expectedBytes = ByteStreams.toByteArray(stream);
    }

    // we haven't uploaded the file yet
    assertEquals(false, s3Dao.objectExists(bucket, objectKey));

    // upload the file to the bucket using byte array and verify the summary has expected values
    final var uploadSummary = s3Dao.putObject(bucket, objectKey, expectedBytes, metaData);
    assertEquals(objectKey, uploadSummary.getKey());
    assertThat(uploadSummary.getETag()).isNotEmpty();
    assertEquals(expectedBytes.length, uploadSummary.getSize());

    // we have uploaded the file now
    assertEquals(true, s3Dao.objectExists(bucket, objectKey));

    // read back file details and verify they have expected values
    final var objectDetails = s3Dao.readObjectMetaData(bucket, objectKey);
    assertEquals(objectKey, objectDetails.getKey());
    assertEquals(expectedBytes.length, objectDetails.getSize());
    assertEquals(uploadSummary.getETag(), objectDetails.getETag());
    assertEquals(metaData, objectDetails.getMetaData());

    // read back file contents and verify they match what we uploaded
    byte[] downloadedBytes;
    try (var stream = s3Dao.readObject(bucket, objectKey)) {
      downloadedBytes = ByteStreams.toByteArray(stream);
    }
    assertThat(downloadedBytes).isEqualTo(expectedBytes);
  }

  /** Verify we can delete files from a bucket. */
  @Test
  void shouldDeleteFilesCorrectly() {
    final var objectKey = "my-object-key";

    // we haven't uploaded the file yet
    assertEquals(false, s3Dao.objectExists(bucket, objectKey));

    // upload it and verify it exists
    s3Dao.putObject(bucket, objectKey, "hello, s3!".getBytes(StandardCharsets.UTF_8), Map.of());
    assertEquals(true, s3Dao.objectExists(bucket, objectKey));

    // delete the file and verify it no longer exists
    s3Dao.deleteObject(bucket, objectKey);
    assertEquals(false, s3Dao.objectExists(bucket, objectKey));
  }

  /**
   * Verify we can list files with and without prefixes and that paging doesn't affect the result.
   */
  @Test
  void shouldListObjectsCorrectly() {
    // Create a bunch of random files at random paths
    // Random seed is arbitrary but fixed so test runs consistently.
    final var random = new Random(1000);
    final var now = Instant.now();
    final var directories = List.of("/a/", "/a/c/", "/d/e/f/");
    final var files = new HashMap<String, S3Dao.S3ObjectSummary>();
    for (int i = 1; i <= 50; ++i) {
      // actual file contents don't matter but we want variety of sizes
      var directory = directories.get(random.nextInt(directories.size()));
      var name = String.valueOf(random.nextInt(1000));
      var size = random.nextInt(1000) + 100;
      var key = directory + name;

      // upload the file and stash its summary for use below
      var summary = s3Dao.putObject(bucket, key, new byte[size], Map.of());
      summary.setLastModified(now);
      files.put(key, summary);
    }

    // now pull down summaries for every directory and ensure they match
    for (String directory : directories) {
      // extract all of the summaries for the directory from our Map
      final var expected =
          files.entrySet().stream()
              .filter(e -> e.getKey().startsWith(directory))
              .map(Map.Entry::getValue)
              .collect(Collectors.toSet());

      // download a list using default (large) page size
      final var actual = s3Dao.listObjects(bucket, directory).collect(Collectors.toSet());
      for (var object : actual) {
        object.setLastModified(now);
      }
      assertEquals(expected, actual);
    }

    // extract all summaries from our Map
    final var expected = Set.copyOf(files.values());

    // download a list with no prefix using default (large) page size
    final var actual = s3Dao.listObjects(bucket).collect(Collectors.toSet());
    for (var object : actual) {
      object.setLastModified(now);
    }

    assertEquals(expected, actual);

    // download a list with no prefix using tiny page size, should still be the same
    final var paged =
        s3Dao.listObjects(bucket, Optional.empty(), Optional.of(2)).collect(Collectors.toSet());
    for (var object : paged) {
      object.setLastModified(now);
    }
    assertEquals(expected, paged);
  }

  /**
   * Verify that RuntimeExceptions are returned unchanged and other exceptions are wrapped in a
   * RuntimeException.
   */
  @Test
  void shouldUnwrapCompletionExceptionsCorrectly() {
    Exception cause = new IOException("oops");
    RuntimeException unwrapped =
        s3Dao.extractCompletionExceptionCause(new CompletionException(cause));
    assertThat(unwrapped).isInstanceOf(UncheckedIOException.class).hasCause(cause);

    cause = new InterruptedException("whoops");
    unwrapped = s3Dao.extractCompletionExceptionCause(new CompletionException(cause));
    assertThat(unwrapped).isInstanceOf(RuntimeException.class).hasCause(cause);

    cause = new RuntimeException("whoops");
    unwrapped = s3Dao.extractCompletionExceptionCause(new CompletionException(cause));
    assertSame(cause, unwrapped);
  }

  /** Verify that a DAO with no endpoint override will refuse to create a test bucket. */
  @Test
  void shouldRejectBucketCreationWithNoEndpointOverride() {
    var mockClient = mock(S3Client.class);
    var mockAsyncClient = mock(S3AsyncClient.class);
    var mockTransferManager = mock(S3TransferManager.class);
    var mockedDao = new S3Dao(mockClient, mockAsyncClient, mockTransferManager);

    // force our mock to see a non-overridden endpoint configuration
    var mockConfiguration = mock(S3ServiceClientConfiguration.class);
    doReturn(Optional.empty()).when(mockConfiguration).endpointOverride();
    doReturn(mockConfiguration).when(mockClient).serviceClientConfiguration();

    // with no endpoint override the attempt to create a bucket should throw
    assertThatThrownBy(() -> mockedDao.createTestBucket())
        .isInstanceOf(BadCodeMonkeyException.class)
        .hasMessageContaining("s3Client has no endpoint override");
  }

  /** Verify that deleting a bucket that does not have the proper name prefix throws. */
  @Test
  void shouldRejectDeleteNonTestBucket() {
    assertThatThrownBy(() -> s3Dao.deleteTestBucket("my-bucket"))
        .isInstanceOf(BadCodeMonkeyException.class)
        .hasMessageContaining("only buckets created by this class can be deleted");
  }

  /**
   * Verify that copying a file within the same bucket works correctly.
   *
   * @throws IOException pass through
   */
  @Test
  void shouldCopyFileInSameBucket() throws IOException {
    // ensure copy requires two parts to be transferred
    final var originalBytes = new byte[MIN_PART_SIZE_FOR_TESTING + 1000];
    ThreadLocalRandom.current().nextBytes(originalBytes);

    final var originalKey = "/a/original";
    final var duplicateKey = "/c/duplicate";
    final var metaData = Map.of("a", "acorn", "c", "carrot");

    s3Dao.putObject(bucket, originalKey, originalBytes, metaData);
    assertEquals(true, s3Dao.objectExists(bucket, originalKey));
    assertEquals(false, s3Dao.objectExists(bucket, duplicateKey));

    s3Dao.copyObject(bucket, originalKey, bucket, duplicateKey);
    assertEquals(true, s3Dao.objectExists(bucket, duplicateKey));

    final byte[] duplicateBytes;
    try (var stream = s3Dao.readObject(bucket, duplicateKey)) {
      duplicateBytes = ByteStreams.toByteArray(stream);
    }
    assertThat(duplicateBytes).isEqualTo(originalBytes);

    // copying should preserve meta data
    assertEquals(metaData, s3Dao.readObjectMetaData(bucket, duplicateKey).getMetaData());
  }

  /**
   * Verify that copying a file between two different buckets works correctly.
   *
   * @throws IOException pass through
   */
  @Test
  void shouldCopyFileBetweenBuckets() throws IOException {
    final String destBucket = s3Dao.createTestBucket();
    try {
      // ensure copy requires two parts to be transferred
      final var originalBytes = new byte[MIN_PART_SIZE_FOR_TESTING + 1000];
      ThreadLocalRandom.current().nextBytes(originalBytes);

      final var originalKey = "/a/original";
      final var duplicateKey = "/c/duplicate";
      final var metaData = Map.of("a", "acorn", "c", "carrot");

      s3Dao.putObject(bucket, originalKey, originalBytes, metaData);
      assertEquals(true, s3Dao.objectExists(bucket, originalKey));
      assertEquals(false, s3Dao.objectExists(destBucket, duplicateKey));

      s3Dao.copyObject(bucket, originalKey, destBucket, duplicateKey);
      assertEquals(true, s3Dao.objectExists(destBucket, duplicateKey));

      final byte[] duplicateBytes;
      try (var stream = s3Dao.readObject(destBucket, duplicateKey)) {
        duplicateBytes = ByteStreams.toByteArray(stream);
      }
      assertThat(duplicateBytes).isEqualTo(originalBytes);

      // copying should preserve meta data
      assertEquals(metaData, s3Dao.readObjectMetaData(destBucket, duplicateKey).getMetaData());
    } finally {
      s3Dao.deleteTestBucket(destBucket);
    }
  }
}
