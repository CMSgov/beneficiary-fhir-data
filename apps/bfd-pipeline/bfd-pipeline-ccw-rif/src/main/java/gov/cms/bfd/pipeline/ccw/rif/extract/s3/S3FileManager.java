package gov.cms.bfd.pipeline.ccw.rif.extract.s3;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Strings;
import com.google.common.io.ByteSource;
import gov.cms.bfd.pipeline.sharedutils.s3.S3Dao;
import gov.cms.bfd.pipeline.sharedutils.s3.S3DirectoryDao;
import gov.cms.bfd.pipeline.sharedutils.s3.S3DirectoryDao.DownloadedFile;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class S3FileManager implements AutoCloseable {
  static final String TIMER_DOWNLOAD_FILE =
      MetricRegistry.name(S3FileManager.class, "downloadFile");
  static final String TIMER_COMPUTE_MD5 = MetricRegistry.name(S3FileManager.class, "computeMd5");

  /** The metric registry. */
  private final MetricRegistry appMetrics;

  /** The DAO for interacting with AWS S3 buckets and files. */
  private final S3Dao s3Dao;

  /** The S3 bucket containing source files. */
  @Getter private final String s3BucketName;

  /** Used to manage file download and caching. */
  private final S3DirectoryDao s3DirectoryDao;

  public S3FileManager(MetricRegistry appMetrics, S3Dao s3Dao, String s3Bucket) throws IOException {
    this.appMetrics = appMetrics;
    this.s3Dao = s3Dao;
    this.s3BucketName = s3Bucket;
    final Path cacheDirectory = Files.createTempDirectory("s3cache");
    s3DirectoryDao = new S3DirectoryDao(s3Dao, s3Bucket, "", cacheDirectory, true, true);
  }

  @Override
  public void close() throws Exception {
    s3DirectoryDao.close();
  }

  public Stream<S3Dao.S3ObjectSummary> scanS3ForFiles(String s3KeyPrefix) {
    return s3Dao.listObjects(s3BucketName, s3KeyPrefix);
  }

  public static String extractPrefixFromS3Key(String s3Key) {
    int lastSlashOffset = s3Key.lastIndexOf('/');
    if (lastSlashOffset < 0) {
      return "";
    } else {
      return s3Key.substring(0, lastSlashOffset + 1);
    }
  }

  public Set<String> fetchFileNamesWithPrefix(String s3Prefix) {
    return s3DirectoryDao.readFileNames().stream()
        .filter(s -> s.startsWith(s3Prefix))
        .collect(Collectors.toUnmodifiableSet());
  }

  public DownloadedFile downloadFile(String s3Key) throws IOException {
    try (var ignored = appMetrics.timer(TIMER_DOWNLOAD_FILE).time()) {
      return s3DirectoryDao.fetchFile(s3Key);
    }
  }

  /** Result returned by {@link #checkMD5}. */
  public enum Md5Result {
    /** Computed MD5 matches meta data value. */
    MATCH,
    /** Computed MD5 does not match meta data value. */
    MISMATCH,
    /** No meta data value exists to compare to. */
    NONE
  }

  /**
   * Compute the MD5 checksum of a {@link DownloadedFile} and compare it to the value found in the
   * given meta data field. If the field is not present no computation is done.
   *
   * @param file file to check
   * @param md5MetaDataField field that should contain a checksum
   * @return result of the check
   * @throws IOException error encountered while reading file
   */
  public Md5Result checkMD5(DownloadedFile file, String md5MetaDataField) throws IOException {
    final String metaDataMD5Checksum = file.getS3Details().getMetaData().get(md5MetaDataField);
    if (Strings.isNullOrEmpty(metaDataMD5Checksum)) {
      return Md5Result.NONE;
    }
    final String computedMD5Checksum = computeMD5CheckSum(file.getBytes());
    if (metaDataMD5Checksum.equals(computedMD5Checksum)) {
      return Md5Result.MATCH;
    } else {
      return Md5Result.MISMATCH;
    }
  }

  /**
   * Calculates and returns a Base64 encoded MD5 checksum value for a file.
   *
   * @param fileToCheck the {@link ByteSource} of the file just downloaded from S3
   * @return Base64 encoded md5 value
   * @throws IOException if there is an issue reading or closing the downloaded file
   */
  public String computeMD5CheckSum(ByteSource fileToCheck) throws IOException {
    try (var ignored = appMetrics.timer(TIMER_COMPUTE_MD5).time();
        var inputStream = fileToCheck.openStream()) {
      final MessageDigest md5Digest = MessageDigest.getInstance("MD5");

      byte[] buffer = new byte[8192];
      for (int bytesCount = inputStream.read(buffer);
          bytesCount > 0;
          bytesCount = inputStream.read(buffer)) {
        md5Digest.update(buffer, 0, bytesCount);
      }

      final byte[] digestBytes = md5Digest.digest();
      return Base64.getEncoder().encodeToString(digestBytes);
    } catch (NoSuchAlgorithmException e) {
      // this should never happen so convert it to an unchecked exception
      throw new BadCodeMonkeyException("No MessageDigest instance for MD5", e);
    }
  }
}
