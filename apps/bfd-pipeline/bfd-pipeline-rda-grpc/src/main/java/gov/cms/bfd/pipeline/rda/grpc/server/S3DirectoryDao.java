package gov.cms.bfd.pipeline.rda.grpc.server;

import static gov.cms.bfd.pipeline.sharedutils.s3.SharedS3Utilities.waitForObjectToExist;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteSource;
import com.google.common.io.MoreFiles;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Front end for downloading files from an S3 bucket/directory and caching them locally. Once
 * downloaded a file can be accessed locally without network latency or errors. The cache is simply
 * a directory containing one data file per S3 file. Within the directory the file name of every
 * cached file consists of a prefix, the name from S3, the eTag (version) of the file, and a suffix.
 * The prefix, name, and etag are separated from each other using a dash.
 *
 * <p>This DAO imposes restrictions on S3 keys. In order to be accessed using this DAO an S3 object
 * must have a key consisting solely of alpha, numeric, dash, underscore, or period.
 *
 * <p>The class is intended to be as thread safe as file operations allow. Files are downloaded
 * using unique temporary names and then renamed (atomically if file system allows, as linux
 * generally does) to their final name. Downloaded file names include the eTag from the object so
 * downloading a new version does not modify a previous version. It is possible for two threads or
 * processes to download the same file simultaneously but the downloaded files would be identical
 * and they are never accessible while being downloaded so this should not produce any conflicts.
 * Methods are provided for clearing obsolete files as well as clearing entire disk cache.
 */
@Slf4j
public class S3DirectoryDao {
  /** Prefix added to file name to indicate it is a data file. */
  private static final String DataFilePrefix = "s3-";
  /** Suffix added to file name to indicate it is a data file. */
  private static final String DataFileSuffix = ".dat";
  /** Inserted between file name and suffix when building a data file name. */
  public static final String EtagSeparator = "-";
  /** Arbitrary prefix used to generate temp file name when a file is downloaded. */
  private static final String TempPrefix = "s3d";

  /**
   * Base regex to match validate S3 keys. The {@link #s3DirectoryPath} is added to the start of
   * this string to build the final regex.
   */
  private static final String S3FileNameRegex = "[_a-z0-9][.-_a-z0-9_]+";

  /** The client for interacting with AWS S3 buckets and files. */
  private final AmazonS3 s3Client;
  /** The S3 bucket to to mirror. */
  @Getter private final String s3BucketName;
  /** The directory path within bucket to mirror. */
  @Getter private final String s3DirectoryPath;

  /** Used to determine if an S3 key is valid. */
  private final Pattern validKeyRegex;

  /** The local directory containing mirrored files. Initialized on first use. */
  private final Path cacheDirectory;

  /**
   * Creates an instance.
   *
   * @param s3Client used to access S3
   * @param s3BucketName the bucket to read from
   * @param s3DirectoryPath the directory inside the bucket to read from
   * @param cacheDirectory the local directory to store cached files in
   */
  public S3DirectoryDao(
      AmazonS3 s3Client, String s3BucketName, String s3DirectoryPath, Path cacheDirectory) {
    this.s3Client = Preconditions.checkNotNull(s3Client);
    this.s3BucketName = Preconditions.checkNotNull(s3BucketName);
    this.s3DirectoryPath = normalizeDirectoryPath(s3DirectoryPath);
    this.cacheDirectory = Preconditions.checkNotNull(cacheDirectory);
    validKeyRegex =
        Pattern.compile(this.s3DirectoryPath + S3FileNameRegex, Pattern.CASE_INSENSITIVE);
  }

  /**
   * Scan the S3 bucket/directory for objects and return simple file names corresponding to each
   * one. Does not actually download any files.
   *
   * @return list of file names
   * @throws RuntimeException various exceptions might be thrown by the AWS API
   */
  public List<String> readFileNames() {
    return readFileNamesFromS3();
  }

  /**
   * Look for an object in our S3 bucket/directory that corresponds to the given simple file name
   * (as returned by {@link #readFileNames}. If one is found downloads it and returns a {@link
   * ByteSource} that can be used to read the file. If no such object exists or the object could not
   * be successfully downloaded, this method will return null.
   *
   * @param fileName simple file name as returned in previous call to {@link #readFileNames}
   * @return null or a valid {@link ByteSource} for reading the file
   * @throws IOException various exceptions might be thrown by the Java or AWS API
   */
  @Nullable
  public ByteSource downloadFile(String fileName) throws IOException {
    final var s3Key = s3DirectoryPath + fileName;
    var s3MetaData = s3Client.getObjectMetadata(s3BucketName, s3Key);
    if (s3MetaData == null) {
      log.debug("file does not exist in S3: {}", s3Key);
      return null;
    }

    String eTag = s3MetaData.getETag();
    var dataFile = dataFilePath(fileName, eTag);
    if (Files.isRegularFile(dataFile)) {
      return MoreFiles.asByteSource(dataFile);
    }

    final var tempDataFile = createTempFile();
    try {
      final var downloadRequest = new GetObjectRequest(s3BucketName, s3Key);
      s3MetaData = s3Client.getObject(downloadRequest, tempDataFile.toFile());
      if (s3MetaData == null) {
        log.debug("file does not exist in S3 or download failed: {}", s3Key);
        return null;
      }

      // it's possible that we received a different version than we expected
      eTag = s3MetaData.getETag();

      // In linux renaming a file in a local (not network shared) directory is atomic and will
      // replace any existing file with same name. It is possible (though unlikely) for the
      // rename to fail, in which case we return null and delete the temp file.
      dataFile = dataFilePath(fileName, eTag);

      try {
        Files.move(tempDataFile, dataFile);
      } catch (FileAlreadyExistsException ex) {
        // It is possible for two processes or threads to perform this rename at the same time.
        // If that happens both files would be identical so we can simply use the one that
        // already existed when we attempted our rename.
      }
      return MoreFiles.asByteSource(dataFile);
    } finally {
      // Ensure temp file is deleted if the rename was not performed.
      Files.deleteIfExists(tempDataFile);
    }
  }

  /**
   * Create a uniquely named temporary file in our directory for use when downloading a new file.
   * Any exceptions are mapped to {@link RuntimeException}s.
   *
   * @return the {@link File} for the new temporary file
   */
  @Nonnull
  private Path createTempFile() {
    try {
      Path result;
      synchronized (this) {
        result = cacheDirectory;
      }
      return Files.createTempFile(result, TempPrefix, null);
    } catch (RuntimeException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * Deletes any files in our directory that do not correspond to an object in our S3
   * bucket/directory.
   *
   * @return number of files deleted
   */
  public int deleteObsoleteFiles() throws IOException {
    int deletedCount = 0;
    var allowedFiles = readFileHandlesFromS3();
    var actualFiles = readFileHandlesFromDirectory();
    for (String file : actualFiles) {
      Path path = Path.of(file);
      if (isDataFile(path) && !allowedFiles.contains(file)) {
        if (Files.deleteIfExists(path)) {
          deletedCount += 1;
        }
      }
    }
    return deletedCount;
  }

  /**
   * Deletes all files in our directory.
   *
   * @return number of files deleted
   */
  public int deleteAllFiles() throws IOException {
    int deletedCount = 0;
    var actualFiles = readFileHandlesFromDirectory();
    for (String file : actualFiles) {
      Path path = Path.of(file);
      if (isDataFile(path) && Files.deleteIfExists(path)) {
        deletedCount += 1;
      }
    }
    return deletedCount;
  }

  /**
   * Uploads a JSON resource to an S3 bucket and waits for it to be fully available. Intended for
   * use in integration tests.
   *
   * @param fileName simple file name for the object
   * @param bytes a {@link ByteSource} referencing the json text
   * @throws IOException if there is an issue opening the input byte source stream
   */
  public void uploadJsonToBucket(String fileName, ByteSource bytes) throws IOException {
    var objectKey = s3DirectoryPath + fileName;
    try (InputStream input = bytes.openStream()) {
      ObjectMetadata metadata = new ObjectMetadata();
      metadata.setContentType("application/json");
      metadata.setContentLength(bytes.size());
      s3Client.putObject(s3BucketName, objectKey, input, metadata);
    }
    waitForObjectToExist(s3Client, s3BucketName, objectKey);
  }

  /**
   * Test a {@link S3ObjectSummary} to determine if it is valid for use with this DAO. Determination
   * is based on compatibility of its key.
   *
   * @param summary {@link S3ObjectSummary} to test
   * @return true if the object key is valid
   */
  public boolean isValidS3Object(S3ObjectSummary summary) {
    return isValidS3Key(summary.getKey());
  }

  /**
   * Test a S3 key to determine if it is valid for use with this DAO. Valid keys start with the S3
   * directory path and have a file name matching the {@link #S3FileNameRegex}.
   *
   * @param key key to test
   * @return true if the key is valid
   */
  public boolean isValidS3Key(String key) {
    return validKeyRegex.matcher(key).matches();
  }

  /**
   * Produce a {@link List} containing an equivalent simple file name for every valid object in our
   * S3 bucket/directory.
   *
   * @return the set
   */
  private List<String> readFileNamesFromS3() {
    return readS3ObjectListing().getObjectSummaries().stream()
        .map(S3ObjectSummary::getKey)
        .filter(this::isValidS3Key)
        .map(this::convertS3KeyToFileName)
        .collect(ImmutableList.toImmutableList());
  }

  /**
   * Produce a {@link Set} containing a file handle equivalent to every valid object in our S3
   * bucket/directory.
   *
   * @return the set
   */
  private Set<String> readFileHandlesFromS3() {
    return readS3ObjectListing().getObjectSummaries().stream()
        .filter(this::isValidS3Object)
        .map(this::dataFilePath)
        .map(Path::toString)
        .collect(ImmutableSet.toImmutableSet());
  }

  /**
   * Produce a {@link Set} containing a file handle for data every file in our directory.
   *
   * @return the set
   */
  private Set<String> readFileHandlesFromDirectory() throws IOException {
    try (Stream<Path> filesStream = Files.list(cacheDirectory)) {
      return filesStream
          .filter(Files::isRegularFile)
          .filter(this::isDataFile)
          .map(Path::toString)
          .collect(ImmutableSet.toImmutableSet());
    }
  }

  /**
   * Converts an S3 object key back into a simple file name by removing its S3 directory path.
   *
   * @param s3Key the S3 key
   * @return equivalent file name
   */
  private String convertS3KeyToFileName(String s3Key) {
    return s3Key.substring(s3DirectoryPath.length());
  }

  /**
   * Reads a list of all files in the S3 bucket/directory.
   *
   * @return the object listing
   */
  private ObjectListing readS3ObjectListing() {
    if (Strings.isNullOrEmpty(s3DirectoryPath)) {
      return s3Client.listObjects(s3BucketName);
    } else {
      return s3Client.listObjects(s3BucketName, s3DirectoryPath);
    }
  }

  /**
   * Ensure that the directory path is either empty or ends with a slash character.
   *
   * @param directoryName s3 directory name
   * @return normalized directory name
   */
  private static String normalizeDirectoryPath(String directoryName) {
    if (directoryName == null) {
      return "";
    } else if (directoryName.isEmpty() || directoryName.endsWith("/")) {
      return directoryName;
    } else {
      return directoryName + "/";
    }
  }

  /**
   * Convert a simple file name plus the file's eTag value into a {@link File} referencing a file in
   * our directory.
   *
   * @param fileName simple file name as exposed to clients
   * @param eTag eTag value from S3 for the file
   * @return file handle
   */
  private Path dataFilePath(String fileName, String eTag) {
    String cacheFileName = DataFilePrefix + fileName + EtagSeparator + eTag + DataFileSuffix;
    Path result;
    synchronized (this) {
      result = cacheDirectory;
    }
    return Path.of(result.toString(), cacheFileName);
  }

  /**
   * Convert a {@link S3ObjectSummary} into a {@link Path} referencing a file in our directory.
   *
   * @param summary {@link S3ObjectSummary} for object to store in cache
   * @return file handle
   */
  private Path dataFilePath(S3ObjectSummary summary) {
    final var fileName = convertS3KeyToFileName(summary.getKey());
    return dataFilePath(fileName, summary.getETag());
  }

  /**
   * Verifies that the given {@link File} is one created by us to store an S3 object.
   *
   * @param path file to check
   * @return true if file has a valid data file name
   */
  private boolean isDataFile(Path path) {
    return Files.isRegularFile(path)
        && path.getFileName().startsWith(DataFilePrefix)
        && path.getFileName().endsWith(DataFileSuffix);
  }
}
