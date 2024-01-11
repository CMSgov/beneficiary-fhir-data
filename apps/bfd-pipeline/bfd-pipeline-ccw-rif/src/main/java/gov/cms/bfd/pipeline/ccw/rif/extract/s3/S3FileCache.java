package gov.cms.bfd.pipeline.ccw.rif.extract.s3;

import com.google.common.io.ByteSource;
import com.google.common.io.MoreFiles;
import gov.cms.bfd.pipeline.sharedutils.MultiCloser;
import gov.cms.bfd.pipeline.sharedutils.s3.S3Dao;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.Data;

public class S3FileCache {
  static final String TEMP_FILE_PREFIX = "S3Download-";
  static final String DEFAULT_TEMP_FILE_SUFFIX = ".dat";
  private static final Pattern KEY_SUFFIX_REGEX =
      Pattern.compile("\\.[a-z]+$", Pattern.CASE_INSENSITIVE);

  private final S3Dao s3Dao;
  private final String s3Bucket;
  private final Map<String, Path> s3KeyToLocalFile;

  public S3FileCache(S3Dao s3Dao, String s3Bucket) {
    this.s3Dao = s3Dao;
    this.s3Bucket = s3Bucket;
    s3KeyToLocalFile = new HashMap<>();
  }

  public DownloadedFile downloadFile(String s3Key) throws IOException {
    final Path existingPath = getPathForS3Key(s3Key);
    if (existingPath != null) {
      return new DownloadedFile(s3Key, existingPath);
    }

    final Path downloadPath = tempFileForS3Key(s3Key);
    s3Dao.downloadObject(s3Bucket, s3Key, downloadPath);

    // The added path might be different if the file has already been registered by another
    // thread while we were downloading the file.  If that happens simply delete our download
    // and use the existing file instead.
    final Path addedPath = addPathForS3Key(s3Key, existingPath);
    if (!addedPath.equals(downloadPath)) {
      Files.delete(downloadPath);
    }
    return new DownloadedFile(s3Key, addedPath);
  }

  public void deleteFile(String s3Key) throws IOException {
    Path path = removePathForS3Key(s3Key);
    if (path != null) {
      Files.deleteIfExists(path);
    }
  }

  public void deleteAllFiles() throws Exception {
    var paths = removeAllPaths();
    MultiCloser closer = new MultiCloser();
    paths.forEach(path -> closer.close(() -> Files.deleteIfExists(path)));
    closer.finish();
  }

  private Path tempFileForS3Key(String s3Key) throws IOException {
    return Files.createTempFile(TEMP_FILE_PREFIX, suffixFromS3Key(s3Key));
  }

  private String suffixFromS3Key(String s3Key) {
    final var matcher = KEY_SUFFIX_REGEX.matcher(s3Key);
    if (matcher.find()) {
      return matcher.group(1).toLowerCase();
    } else {
      return DEFAULT_TEMP_FILE_SUFFIX;
    }
  }

  private synchronized Path getPathForS3Key(String s3Key) {
    return s3KeyToLocalFile.get(s3Key);
  }

  private synchronized Path addPathForS3Key(String s3Key, Path newPath) {
    final var oldPath = s3KeyToLocalFile.get(s3Key);
    if (oldPath != null) {
      return oldPath;
    } else {
      s3KeyToLocalFile.put(s3Key, newPath);
      return newPath;
    }
  }

  private synchronized Path removePathForS3Key(String s3Key) {
    return s3KeyToLocalFile.remove(s3Key);
  }

  private synchronized List<Path> removeAllPaths() {
    final var paths = List.copyOf(s3KeyToLocalFile.values());
    s3KeyToLocalFile.clear();
    return paths;
  }

  @Data
  public class DownloadedFile {
    private final String s3Key;
    private final ByteSource bytes;

    DownloadedFile(String s3Key, Path path) {
      this.s3Key = s3Key;
      bytes = MoreFiles.asByteSource(path);
    }

    public void delete() throws IOException {
      deleteFile(s3Key);
    }
  }
}
