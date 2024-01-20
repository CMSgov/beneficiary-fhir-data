package gov.cms.bfd.pipeline.ccw.rif.extract.s3;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.codahale.metrics.MetricRegistry;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import gov.cms.bfd.model.rif.samples.StaticRifResource;
import gov.cms.bfd.pipeline.AbstractLocalStackS3Test;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Integration tests for {@link S3FileManager}. */
public class S3FileManagerIT extends AbstractLocalStackS3Test {
  /** Name of the temporary S3 bucket used in test. */
  private String bucketName;

  /** The file manager being tested. */
  private S3FileManager fileManager;

  /**
   * Creates a bucket and a {@link S3FileManager}.
   *
   * @throws IOException pass through
   */
  @BeforeEach
  void setUp() throws IOException {
    bucketName = s3Dao.createTestBucket();
    fileManager = new S3FileManager(new MetricRegistry(), s3Dao, bucketName);
  }

  /**
   * Closes the {@link S3FileManager} and deletes the bucket.
   *
   * @throws Exception pass through
   */
  @AfterEach
  void tearDown() throws Exception {
    fileManager.close();
    s3Dao.deleteTestBucket(bucketName);
  }

  /**
   * Constructs a S3 hierarchy and verifies keys are fetched correctly for each level.
   *
   * @throws Exception pass through
   */
  @Test
  public void testFetchKeysWithPrefix() throws Exception {
    final var fileData = new byte[8];
    final var metaData = Map.<String, String>of();
    final var allKeys = Set.of("a.dat", "1/b.dat", "1/c.dat", "1/2/d.dat", "1/2/e.dat");

    for (String key : allKeys) {
      s3Dao.putObject(bucketName, key, fileData, metaData);
    }

    assertEquals(Set.of(), fileManager.fetchKeysWithPrefix("Z"));
    assertEquals(allKeys, fileManager.fetchKeysWithPrefix(""));
    assertEquals(
        allKeys.stream().filter(k -> k.startsWith("1/")).collect(Collectors.toSet()),
        fileManager.fetchKeysWithPrefix("1/"));
    assertEquals(
        allKeys.stream().filter(k -> k.startsWith("1/2/")).collect(Collectors.toSet()),
        fileManager.fetchKeysWithPrefix("1/2/"));
  }

  /**
   * Upload a file directly, download it, and spot check the result matches the original file.
   *
   * @throws IOException pass through
   */
  @Test
  void testDownloadFile() throws IOException {
    final var s3Key = "here/i/am.dat";
    final var fileData = "this is some data for the file";
    final var fileBytes = fileData.getBytes(StandardCharsets.UTF_8);
    final var metaData =
        Map.of(
            "alpha", "AAAA",
            "bravo", "BBBB");
    s3Dao.putObject(bucketName, s3Key, fileBytes, metaData);
    final var downloadResult = fileManager.downloadFile(s3Key);
    assertEquals(s3Key, downloadResult.getS3Key());
    assertEquals(fileData, new String(downloadResult.getBytes().read(), StandardCharsets.UTF_8));
    assertEquals(metaData, downloadResult.getS3Details().getMetaData());
  }

  /** Verify that {@link FileNotFoundException} is thrown for a missing file. */
  @Test
  void testDownloadFileThatDoesNotExist() {
    assertThatThrownBy(() -> fileManager.downloadFile("i/dont/exist.dat"))
        .isInstanceOf(FileNotFoundException.class);
  }

  /**
   * Verify that a missing MD5 in the meta data yields a {@link S3FileManager.MD5Result#NONE}
   * result.
   *
   * @throws IOException pass through
   */
  @Test
  void testFileWithNoMD5() throws IOException {
    s3Dao.putObject(
        bucketName, "test.dat", StaticRifResource.SAMPLE_A_BENES.getResourceUrl(), Map.of());
    var downloadedFile = fileManager.downloadFile("test.dat");
    S3FileManager.MD5Result md5result = fileManager.checkMD5(downloadedFile, "md5-check");
    assertEquals(S3FileManager.MD5Result.NONE, md5result);
  }

  /**
   * Verify that an incorrect MD5 in the meta data yields a {@link S3FileManager.MD5Result#MISMATCH}
   * result.
   *
   * @throws IOException pass through
   */
  @Test
  void testFileWithBadMD5() throws IOException {
    s3Dao.putObject(
        bucketName,
        "test.dat",
        StaticRifResource.SAMPLE_A_BENES.getResourceUrl(),
        Map.of("md5-check", "this is not an md5!"));
    var downloadedFile = fileManager.downloadFile("test.dat");
    S3FileManager.MD5Result md5result = fileManager.checkMD5(downloadedFile, "md5-check");
    assertEquals(S3FileManager.MD5Result.MISMATCH, md5result);
  }

  /**
   * Verify that a correct MD5 in the meta data yields a {@link S3FileManager.MD5Result#MATCH}
   * result.
   *
   * @throws IOException pass through
   */
  @Test
  void testFileWithGoodMD5() throws IOException {
    String realMD5 =
        S3FileManager.computeMD5CheckSum(
            Resources.asByteSource(StaticRifResource.SAMPLE_A_BENES.getResourceUrl()));
    s3Dao.putObject(
        bucketName,
        "test.dat",
        StaticRifResource.SAMPLE_A_BENES.getResourceUrl(),
        Map.of("md5-check", realMD5));
    var downloadedFile = fileManager.downloadFile("test.dat");
    var md5result = fileManager.checkMD5(downloadedFile, "md5-check");
    assertEquals(S3FileManager.MD5Result.MATCH, md5result);
  }

  /** Test that prefix is correctly extracted from various keys. */
  @Test
  void testExtractPrefixFromS3Key() {
    assertEquals("", S3FileManager.extractPrefixFromS3Key(""));
    assertEquals("", S3FileManager.extractPrefixFromS3Key("alpha"));
    assertEquals("alpha/", S3FileManager.extractPrefixFromS3Key("alpha/"));
    assertEquals("alpha/", S3FileManager.extractPrefixFromS3Key("alpha/bravo"));
    assertEquals("alpha/bravo/", S3FileManager.extractPrefixFromS3Key("alpha/bravo/charlie"));
  }

  /**
   * Test to ensure the MD5ChkSum of the downloaded S3 file matches the generated MD5ChkSum value.
   */
  @Test
  void testComputeMD5CheckSum() throws Exception {
    // Using a Random with fixed seed because data can be anything at all.
    // Array is multiple buffers long but not so big that test is slow.
    byte[] randomBytes = new byte[33_000];
    new Random(0).nextBytes(randomBytes);

    // using Guava's MD5 implementation as known good value
    byte[] expectedMD5Bytes = Hashing.md5().hashBytes(randomBytes).asBytes();
    String expectedMD5 = Base64.getEncoder().encodeToString(expectedMD5Bytes);

    // dump the bytes into a file and compute the MD5 using our code
    File localTempFile = File.createTempFile("data-pipeline-s3-temp", ".rif");
    Files.write(randomBytes, localTempFile);
    String computedMD5 = S3FileManager.computeMD5CheckSum(Files.asByteSource(localTempFile));
    localTempFile.delete();

    // they better match
    assertEquals(expectedMD5, computedMD5);
  }
}
