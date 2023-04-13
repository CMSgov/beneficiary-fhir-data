package gov.cms.bfd.pipeline.rda.grpc.server;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteSource;
import com.google.common.io.MoreFiles;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import gov.cms.bfd.pipeline.rda.grpc.MultiCloser;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import javax.annotation.Nonnull;
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
public class S3DirectoryDao implements AutoCloseable {
  /** Prefix added to cache file name to indicate it is a data file. */
  private static final String DataFilePrefix = "s3-";
  /** Suffix added to cache file name to indicate it is a data file. */
  private static final String DataFileSuffix = ".dat";
  /** Inserted between cache file name and suffix when building a data file name. */
  public static final String EtagSeparator = "-";
  /** Arbitrary prefix used to generate temp file name when a file is downloaded. */
  private static final String TempPrefix = "s3d";

  /**
   * Base regex to match valid S3 keys. The {@link #s3DirectoryPath} is added to the start of this
   * string to build the final regex.
   */
  private static final String S3FileNameRegex = "[_a-z0-9][-_.a-z0-9]+";

  /** Value returned by {@link AmazonS3Exception#getStatusCode} to indicate object not found. */
  private static final int AWS_NOT_FOUND_STATUS_CODE = 404;

  /** The client for interacting with AWS S3 buckets and files. */
  private final AmazonS3 s3Client;
  /** The S3 bucket containing source files. */
  @Getter private final String s3BucketName;
  /** The directory path within bucket containing source files. */
  @Getter private final String s3DirectoryPath;

  /** Used to determine if an S3 key is valid. */
  private final Pattern validKeyRegex;

  /** The local directory containing downloaded files. Initialized on first use. */
  private final Path cacheDirectory;

  /**
   * When this is true the close method will try to delete all files from the {@link
   * #cacheDirectory} and then delete the directory itself.
   */
  private final boolean deleteOnExit;

  /**
   * Creates an instance.
   *
   * @param s3Client used to access S3
   * @param s3BucketName the bucket to read from
   * @param s3DirectoryPath the directory inside the bucket to read from
   * @param cacheDirectory the local directory to store cached files in
   * @param deleteOnExit causes close to delete all cached files and directory when true
   */
  public S3DirectoryDao(
      AmazonS3 s3Client,
      String s3BucketName,
      String s3DirectoryPath,
      Path cacheDirectory,
      boolean deleteOnExit) {
    this.s3Client = Preconditions.checkNotNull(s3Client);
    this.s3BucketName = Preconditions.checkNotNull(s3BucketName);
    this.s3DirectoryPath = normalizeDirectoryPath(s3DirectoryPath);
    this.cacheDirectory = Preconditions.checkNotNull(cacheDirectory);
    this.deleteOnExit = deleteOnExit;
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
   * be successfully downloaded, this method will throw an exception.
   *
   * @param fileName simple file name as returned in previous call to {@link #readFileNames}
   * @return {@link ByteSource} for reading the file from local directory
   * @throws IOException various exceptions might be thrown by the Java or AWS API
   */
  public ByteSource downloadFile(String fileName) throws IOException {
    final String s3Key = s3DirectoryPath + fileName;
    var s3MetaData = readS3ObjectMetaData(fileName, s3Key);

    String eTag = s3MetaData.getETag();
    Path cacheFile = cacheFilePath(fileName, eTag);
    if (Files.isRegularFile(cacheFile)) {
      log.info(
          "serving existing file from cache: fileName={} s3Key={} cachedFile={}",
          fileName,
          s3Key,
          cacheFile.getFileName());
      return createByteSourceForCachedFile(fileName, cacheFile);
    }

    final Path tempDataFile = Files.createTempFile(cacheDirectory, TempPrefix, null);
    try {
      log.info(
          "downloading file from S3: fileName={} s3Key={} tempFile={}",
          fileName,
          s3Key,
          tempDataFile.getFileName());
      s3MetaData = downloadS3Object(fileName, s3Key, tempDataFile);

      // It is possible that the eTag changed between the time we fetched meta data and the
      // time we downloaded the object.
      eTag = s3MetaData.getETag();
      cacheFile = cacheFilePath(fileName, eTag);

      try {
        log.info(
            "adding downloaded file to cache: fileName={} s3Key={} cacheFile={}",
            fileName,
            s3Key,
            cacheFile.getFileName());
        // In linux renaming a file in a local (not network shared) directory is atomic and will
        // replace any existing file with same name.
        Files.move(tempDataFile, cacheFile);
      } catch (FileAlreadyExistsException ex) {
        // It is possible for two processes or threads to perform this rename at the same time.
        // If that happens both files would be identical so we can simply ignore this exception
        // and use the one that already existed when we attempted our rename.
      }
      log.info(
          "serving downloaded file from cache: fileName={} s3Key={} cachedFile={}",
          fileName,
          s3Key,
          cacheFile.getFileName());
      return createByteSourceForCachedFile(fileName, cacheFile);
    } finally {
      // Ensure temp file is deleted if the rename was not performed for any reason.
      Files.deleteIfExists(tempDataFile);
    }
  }

  /**
   * Deletes cache directory and all of its files if {@link #deleteOnExit} is true. Intended for use
   * when the cache directory is a temp directory.
   *
   * <p>{@inheritDoc}
   *
   * @throws Exception pass through
   */
  @Override
  public void close() throws Exception {
    if (deleteOnExit) {
      log.info("deleting cache: directory={}", cacheDirectory);
      var closer = new MultiCloser();
      closer.close(this::deleteAllFiles);
      closer.close(() -> Files.deleteIfExists(cacheDirectory));
      closer.finish();
    }
  }

  /**
   * Deletes any files in our directory that do not correspond to an object in our S3
   * bucket/directory.
   *
   * @return number of files deleted
   * @throws IOException pass through from any failed operation
   */
  @CanIgnoreReturnValue
  public int deleteObsoleteFiles() throws IOException {
    int deletedCount = 0;
    var allowedFiles = readCacheFileNamesFromS3();
    var actualFiles = readCacheFileNamesFromDirectory();
    for (String file : actualFiles) {
      Path path = Path.of(file);
      if (isCacheFile(path) && !allowedFiles.contains(file)) {
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
   * @throws IOException pass through from any failed operation
   */
  @CanIgnoreReturnValue
  public int deleteAllFiles() throws IOException {
    int deletedCount = 0;
    var actualFiles = readCacheFileNamesFromDirectory();
    for (String file : actualFiles) {
      Path path = Path.of(file);
      if (isCacheFile(path) && Files.deleteIfExists(path)) {
        deletedCount += 1;
      }
    }
    return deletedCount;
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
   * S3 bucket/directory. The simple file name is everything after the S3 directory portion of the
   * object key.
   *
   * @return the list
   */
  private List<String> readFileNamesFromS3() {
    return readS3ObjectListing().getObjectSummaries().stream()
        .map(S3ObjectSummary::getKey)
        .filter(this::isValidS3Key)
        .map(this::convertS3KeyToFileName)
        .collect(ImmutableList.toImmutableList());
  }

  /**
   * Produce a {@link Set} containing an equivalent cache file name for every valid object in our S3
   * bucket/directory.
   *
   * @return the set
   */
  private Set<String> readCacheFileNamesFromS3() {
    return readS3ObjectListing().getObjectSummaries().stream()
        .filter(summary -> isValidS3Key(summary.getKey()))
        .map(this::cacheFilePath)
        .map(Path::toString)
        .collect(ImmutableSet.toImmutableSet());
  }

  /**
   * Produce a {@link Set} containing a file handle for data every file in our directory.
   *
   * @return the set
   */
  private Set<String> readCacheFileNamesFromDirectory() throws IOException {
    try (Stream<Path> filesStream = Files.list(cacheDirectory)) {
      return filesStream
          .filter(Files::isRegularFile)
          .filter(this::isCacheFile)
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
   * Convert a simple file name plus the file's eTag value into a {@link Path} referencing a file in
   * our cache directory.
   *
   * @param fileName simple file name as exposed to clients
   * @param eTag eTag value from S3 for the file
   * @return file handle
   */
  @VisibleForTesting
  Path cacheFilePath(String fileName, String eTag) {
    String cacheFileName = DataFilePrefix + fileName + EtagSeparator + eTag + DataFileSuffix;
    return Path.of(cacheDirectory.toString(), cacheFileName);
  }

  /**
   * Convert a {@link S3ObjectSummary} into a {@link Path} referencing a file in our cache
   * directory.
   *
   * @param summary {@link S3ObjectSummary} for object to store in cache
   * @return file handle
   */
  private Path cacheFilePath(S3ObjectSummary summary) {
    final var fileName = convertS3KeyToFileName(summary.getKey());
    return cacheFilePath(fileName, summary.getETag());
  }

  /**
   * Verifies that the given {@link Path} is one created by us to store an S3 object in our cache
   * directory.
   *
   * @param path file to check
   * @return true if file has a valid data file name
   */
  private boolean isCacheFile(Path path) {
    final var fileName = path.getFileName().toString();
    return Files.isRegularFile(path)
        && fileName.startsWith(DataFilePrefix)
        && fileName.endsWith(DataFileSuffix);
  }

  /**
   * Creates a {@link ByteSource} for reading a cached file. If the file is a gzip file (name ends
   * with .gz) the file will be automatically decompressed when a stream is opened. Otherwise the
   * bytes will be returned as they appear in the file.
   *
   * @param fileName file name from the S3 object key
   * @param path location of the cached file
   * @return the byte source
   */
  private ByteSource createByteSourceForCachedFile(String fileName, Path path) {
    var byteSource = MoreFiles.asByteSource(path);
    if (fileName.endsWith(".gz")) {
      return new ByteSource() {
        @Nonnull
        @Override
        public InputStream openStream() throws IOException {
          return new GZIPInputStream(byteSource.openStream());
        }
      };
    }
    return byteSource;
  }

  /**
   * Read {@link ObjectMetadata} for the given S3 key. Recognize the possible case of object not
   * found (HTTP 404) by throwing more useful {@link FileNotFoundException}.
   *
   * @param fileName the simple file name for the object
   * @param s3Key the S3 object key
   * @return the meta data
   * @throws IOException eith {@link FileNotFoundException} or an AWS runtime exception
   */
  private ObjectMetadata readS3ObjectMetaData(String fileName, String s3Key) throws IOException {
    try {
      return s3Client.getObjectMetadata(s3BucketName, s3Key);
    } catch (AmazonS3Exception ex) {
      if (ex.getStatusCode() == AWS_NOT_FOUND_STATUS_CODE) {
        var fileNotFound = new FileNotFoundException(fileName);
        fileNotFound.addSuppressed(ex);
        throw fileNotFound;
      } else {
        throw ex;
      }
    }
  }

  /**
   * Download S3 object and return its {@link ObjectMetadata}. Recognize the possible case of object
   * not found (HTTP 404) by throwing more useful {@link FileNotFoundException}.
   *
   * @param fileName the simple file name for the object
   * @param s3Key the S3 object key
   * @param tempDataFile where to store the downloaded object
   * @return the meta data
   * @throws IOException eith {@link FileNotFoundException} or an AWS runtime exception
   */
  private ObjectMetadata downloadS3Object(String fileName, String s3Key, Path tempDataFile)
      throws IOException {
    try {
      final var downloadRequest = new GetObjectRequest(s3BucketName, s3Key);
      var metaData = s3Client.getObject(downloadRequest, tempDataFile.toFile());
      if (metaData == null) {
        throw new FileNotFoundException(fileName);
      }
      return metaData;
    } catch (AmazonS3Exception ex) {
      if (ex.getStatusCode() == AWS_NOT_FOUND_STATUS_CODE) {
        throw new FileNotFoundException(convertS3KeyToFileName(s3Key));
      } else {
        throw ex;
      }
    }
  }
}
