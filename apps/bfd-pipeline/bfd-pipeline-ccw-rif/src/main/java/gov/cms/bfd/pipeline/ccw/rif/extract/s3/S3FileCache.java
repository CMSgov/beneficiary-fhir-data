package gov.cms.bfd.pipeline.ccw.rif.extract.s3;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Strings;
import com.google.common.io.ByteSource;
import com.google.common.io.MoreFiles;
import gov.cms.bfd.pipeline.sharedutils.MultiCloser;
import gov.cms.bfd.pipeline.sharedutils.s3.S3Dao;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;

public class S3FileCache {
  private static final Pattern KEY_SUFFIX_REGEX =
      Pattern.compile("\\.[a-z]+$", Pattern.CASE_INSENSITIVE);
  static final String TEMP_FILE_PREFIX = "S3Download-";
  static final String DEFAULT_TEMP_FILE_SUFFIX = ".dat";
  static final String TIMER_DOWNLOAD_FILE = MetricRegistry.name(S3FileCache.class, "downloadFile");
  static final String TIMER_COMPUTE_MD5 = MetricRegistry.name(S3FileCache.class, "computeMd5");

  /** The metric registry. */
  private final MetricRegistry appMetrics;

  private final S3Dao s3Dao;
  private final String s3Bucket;
  private final Map<String, DownloadedFile> s3KeyToLocalFile;

  public S3FileCache(MetricRegistry appMetrics, S3Dao s3Dao, String s3Bucket) {
    this.appMetrics = appMetrics;
    this.s3Dao = s3Dao;
    this.s3Bucket = s3Bucket;
    s3KeyToLocalFile = new HashMap<>();
  }

  public static String extractPrefixFromS3Key(String s3Key) {
    int lastSlashOffset = s3Key.lastIndexOf('/');
    if (lastSlashOffset < 0) {
      return "";
    } else {
      return s3Key.substring(0, lastSlashOffset + 1);
    }
  }

  public static String extractNameFromS3Key(String s3Key) {
    int lastSlashOffset = s3Key.lastIndexOf('/');
    if (lastSlashOffset < 0) {
      return s3Key;
    } else {
      return s3Key.substring(lastSlashOffset + 1);
    }
  }

  public Set<String> fetchFileNamesWithPrefix(String s3Prefix) {
    return s3Dao
        .listObjects(s3Bucket, s3Prefix)
        .map(S3Dao.S3ObjectSummary::getKey)
        .map(S3FileCache::extractNameFromS3Key)
        .collect(Collectors.toUnmodifiableSet());
  }

  public DownloadedFile downloadFile(String s3Key) throws IOException {
    try (var ignored = appMetrics.timer(TIMER_DOWNLOAD_FILE).time()) {
      final DownloadedFile existingFile = getFileForS3Key(s3Key);
      if (existingFile != null) {
        return existingFile;
      }

      final Path downloadPath = tempPathForS3Key(s3Key);
      final var s3Details = s3Dao.downloadObject(s3Bucket, s3Key, downloadPath);
      final var downloadedFile = new DownloadedFile(s3Key, s3Details, downloadPath);

      // The added file might be different if the file has already been registered by another
      // thread while we were downloading the file.  If that happens simply delete our download
      // and use the existing file instead.
      final DownloadedFile addedFile = addFileForS3Key(s3Key, downloadedFile);
      if (addedFile != downloadedFile) {
        Files.delete(downloadPath);
      }

      return addedFile;
    }
  }

  public void deleteFile(String s3Key) throws IOException {
    DownloadedFile file = removeFileForS3Key(s3Key);
    if (file != null) {
      Files.deleteIfExists(file.path);
    }
  }

  public void deleteAllFiles() throws Exception {
    var files = removeAllFiles();
    MultiCloser closer = new MultiCloser();
    files.forEach(file -> closer.close(() -> Files.deleteIfExists(file.path)));
    closer.finish();
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

  private Path tempPathForS3Key(String s3Key) throws IOException {
    return Files.createTempFile(TEMP_FILE_PREFIX, suffixFromS3Key(s3Key));
  }

  private String suffixFromS3Key(String s3Key) {
    final var matcher = KEY_SUFFIX_REGEX.matcher(s3Key);
    if (matcher.find()) {
      return matcher.group(0).toLowerCase();
    } else {
      return DEFAULT_TEMP_FILE_SUFFIX;
    }
  }

  private synchronized DownloadedFile getFileForS3Key(String s3Key) {
    return s3KeyToLocalFile.get(s3Key);
  }

  private synchronized DownloadedFile addFileForS3Key(String s3Key, DownloadedFile newFile) {
    final var oldFile = s3KeyToLocalFile.get(s3Key);
    if (oldFile != null) {
      return oldFile;
    } else {
      s3KeyToLocalFile.put(s3Key, newFile);
      return newFile;
    }
  }

  private synchronized DownloadedFile removeFileForS3Key(String s3Key) {
    return s3KeyToLocalFile.remove(s3Key);
  }

  private synchronized List<DownloadedFile> removeAllFiles() {
    final var files = List.copyOf(s3KeyToLocalFile.values());
    s3KeyToLocalFile.clear();
    return files;
  }

  @AllArgsConstructor
  public class DownloadedFile {
    @Getter private final String s3Key;
    @Getter private final S3Dao.S3ObjectDetails s3Details;
    private final Path path;

    public ByteSource getBytes() {
      return MoreFiles.asByteSource(path);
    }

    public void delete() throws IOException {
      deleteFile(s3Key);
    }

    public String getAbsolutePath() {
      return path.toAbsolutePath().toString();
    }
  }
}
