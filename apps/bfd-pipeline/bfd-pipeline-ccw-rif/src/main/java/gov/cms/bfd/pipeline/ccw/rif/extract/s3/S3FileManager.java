package gov.cms.bfd.pipeline.ccw.rif.extract.s3;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Strings;
import com.google.common.io.ByteSource;
import gov.cms.bfd.pipeline.sharedutils.s3.S3Dao;
import gov.cms.bfd.pipeline.sharedutils.s3.S3DirectoryDao;
import gov.cms.bfd.pipeline.sharedutils.s3.S3DirectoryDao.DownloadedFile;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages the interactions between S3 and a local disk cache of files. Internally uses a {@link
 * S3DirectoryDao} to download and cache files from S3.
 */
public class S3FileManager implements AutoCloseable {
  /** Name of timer used to report S3 file download times. */
  static final String TIMER_DOWNLOAD_FILE =
      MetricRegistry.name(S3FileManager.class, "downloadFile");

  /** Name of timer used to report MD5 computation times. */
  static final String TIMER_COMPUTE_MD5 = MetricRegistry.name(S3FileManager.class, "computeMd5");

  /** The metric registry. */
  private final MetricRegistry appMetrics;

  /** The DAO for interacting with AWS S3 buckets and files. */
  private final S3Dao s3Dao;

  /** The S3 bucket containing source files. */
  private final String s3BucketName;

  /** Used to download files and cache them locally. */
  private final S3DirectoryDao s3DirectoryDao;

  /**
   * Initializes an instance.
   *
   * @param appMetrics used to post metrics
   * @param s3Dao used to interact with S3
   * @param s3Bucket name of S3 the bucket we work with
   * @throws IOException pass through in case of errors
   */
  public S3FileManager(MetricRegistry appMetrics, S3Dao s3Dao, String s3Bucket) throws IOException {
    this.appMetrics = appMetrics;
    this.s3Dao = s3Dao;
    this.s3BucketName = s3Bucket;
    final Path cacheDirectory = Files.createTempDirectory("s3cache");
    s3DirectoryDao = new S3DirectoryDao(s3Dao, s3Bucket, "", cacheDirectory, true, true);
  }

  /**
   * Deletes all cached files.
   *
   * @throws Exception pass through in case of errrors
   */
  @Override
  public void close() throws Exception {
    s3DirectoryDao.close();
  }

  /**
   * Scans the S3 bucket for all files having the given key prefix and yields a {@link
   * S3Dao.S3ObjectSummary} for each file.
   *
   * @param s3KeyPrefix prefix to search for
   * @return stream of the summaries
   */
  public Stream<S3Dao.S3ObjectSummary> scanS3ForFiles(String s3KeyPrefix) {
    return s3Dao.listObjects(s3BucketName, s3KeyPrefix);
  }

  /**
   * Scan S3 bucket for all keys that start with the given prefix and return them in a {@link Set}.
   *
   * @param s3KeyPrefix prefix to search for
   * @return the keys
   */
  public Set<String> fetchKeysWithPrefix(String s3KeyPrefix) {
    return scanS3ForFiles(s3KeyPrefix)
        .map(S3Dao.S3ObjectSummary::getKey)
        .collect(Collectors.toUnmodifiableSet());
  }

  /**
   * Download a file with the given key and cache its file data.
   *
   * @param s3Key identifies the file to download
   * @return download result
   * @throws IOException thrown if download fails
   * @throws FileNotFoundException if no file exists in S3 for the given key
   */
  public DownloadedFile downloadFile(String s3Key) throws IOException {
    try (var ignored = appMetrics.timer(TIMER_DOWNLOAD_FILE).time()) {
      return s3DirectoryDao.fetchFile(s3Key);
    }
  }

  /**
   * Gets the number of bytes of usable disk space from the file system containing our cache
   * directory.
   *
   * @return number of bytes
   * @throws IOException pass through from file system check
   */
  public long getAvailableDiskSpaceInBytes() throws IOException {
    return s3DirectoryDao.getAvailableDiskSpaceInBytes();
  }

  /** Result returned by {@link #checkMD5}. */
  public enum MD5Result {
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
  public MD5Result checkMD5(DownloadedFile file, String md5MetaDataField) throws IOException {
    try (var ignored = appMetrics.timer(TIMER_COMPUTE_MD5).time()) {
      final String metaDataMD5Checksum = file.getS3Details().getMetaData().get(md5MetaDataField);
      if (Strings.isNullOrEmpty(metaDataMD5Checksum)) {
        return MD5Result.NONE;
      }
      final String computedMD5Checksum = computeMD5CheckSum(file.getBytes());
      if (metaDataMD5Checksum.equals(computedMD5Checksum)) {
        return MD5Result.MATCH;
      } else {
        return MD5Result.MISMATCH;
      }
    }
  }

  /**
   * Extracts the full prefix of the s3 key. The prefix is all characters preceding the right most /
   * character plus the slash itself. The prefix for a string containing no slash character is empty
   * string.
   *
   * @param s3Key key to extract prefix from
   * @return the prefix
   */
  public static String extractPrefixFromS3Key(String s3Key) {
    int lastSlashOffset = s3Key.lastIndexOf('/');
    if (lastSlashOffset < 0) {
      return "";
    } else {
      return s3Key.substring(0, lastSlashOffset + 1);
    }
  }

  /**
   * Calculates and returns a Base64 encoded MD5 checksum value for a file.
   *
   * @param bytesToCheck the {@link ByteSource} of the file just downloaded from S3
   * @return Base64 encoded md5 value
   * @throws IOException if there is an issue reading or closing the downloaded file
   */
  @SuppressWarnings("java:S4790")
  public static String computeMD5CheckSum(ByteSource bytesToCheck) throws IOException {
    try (var inputStream = bytesToCheck.openStream()) {
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
