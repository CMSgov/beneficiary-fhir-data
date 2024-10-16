package gov.cms.bfd.pipeline.sharedutils.s3;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteSource;
import com.google.common.io.MoreFiles;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import gov.cms.bfd.pipeline.sharedutils.MultiCloser;
import jakarta.annotation.Nonnull;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

/**
 * Front end for downloading files from an S3 bucket/directory and caching them locally. Once
 * downloaded a file can be accessed locally without network latency or errors. The cache is simply
 * a directory containing one data file per S3 file. Within the directory the file name of every
 * cached file consists of a prefix, the name from S3, the eTag (version) of the file, and a suffix.
 * The prefix, name, and etag are separated from each other using a dash.
 *
 * <p>This DAO imposes restrictions on S3 keys. In order to be accessed using this DAO an S3 object
 * must have a key consisting solely of alpha, numeric, dash, underscore, colon, or period.
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
   * string to build the final regex. Used when recursive flag is false to prevent inclusion of
   * files within sub-directories.
   */
  private static final String S3FileNameRegex = "[_a-z0-9][-._:a-zA-Z0-9]*";

  /**
   * Base regex to match valid S3 keys. The {@link #s3DirectoryPath} is added to the start of this
   * string to build the final regex. Used when recursive flag is true to allow inclusion of files
   * within sub-directories.
   */
  private static final String S3RecursiveFileNameRegex =
      "(" + S3FileNameRegex + "/)*" + S3FileNameRegex;

  /** The DAO for interacting with AWS S3 buckets and files. */
  private final S3Dao s3Dao;

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
   * @param s3Dao used to access S3
   * @param s3BucketName the bucket to read from
   * @param s3DirectoryPath the directory inside the bucket to read from
   * @param cacheDirectory the local directory to store cached files in
   * @param deleteOnExit causes close to delete all cached files and directory when true
   * @param recursive allows objects within sub-directories to be accessed when true
   */
  public S3DirectoryDao(
      S3Dao s3Dao,
      String s3BucketName,
      String s3DirectoryPath,
      Path cacheDirectory,
      boolean deleteOnExit,
      boolean recursive) {
    this.s3Dao = s3Dao;
    this.s3BucketName = Preconditions.checkNotNull(s3BucketName);
    this.s3DirectoryPath = normalizeDirectoryPath(s3DirectoryPath);
    this.cacheDirectory = Preconditions.checkNotNull(cacheDirectory);
    this.deleteOnExit = deleteOnExit;
    final var regex = recursive ? S3RecursiveFileNameRegex : S3FileNameRegex;
    validKeyRegex = Pattern.compile(this.s3DirectoryPath + regex, Pattern.CASE_INSENSITIVE);
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
    return fetchFile(fileName).getBytes();
  }

  /**
   * Look for an object in our S3 bucket/directory that corresponds to the given simple file name
   * (as returned by {@link #readFileNames}. If one is found downloads it and returns a {@link
   * DownloadedFile} that can be used to read the file. If no such object exists or the object could
   * not be successfully downloaded, this method will throw an exception.
   *
   * @param fileName simple file name as returned in previous call to {@link #readFileNames}
   * @return {@link ByteSource} for reading the file from local directory
   * @throws IOException various exceptions might be thrown by the Java or AWS API
   */
  public DownloadedFile fetchFile(String fileName) throws IOException {
    final String s3Key = s3DirectoryPath + fileName;
    S3Dao.S3ObjectDetails objectDetails = readS3ObjectMetaData(fileName, s3Key);
    String eTag = objectDetails.getETag();

    Path cacheFile = cacheFilePath(fileName, eTag);
    Files.createDirectories(cacheFile.getParent());
    if (Files.isRegularFile(cacheFile)) {
      log.info(
          "serving existing file from cache: fileName={} s3Key={} cachedFile={}",
          fileName,
          s3Key,
          cacheDirectory.relativize(cacheFile));
      return new DownloadedFile(fileName, objectDetails, cacheFile);
    }

    final Path tempDataFile = Files.createTempFile(cacheDirectory, TempPrefix, null);
    try {
      log.info(
          "downloading file from S3: fileName={} s3Key={} tempFile={}",
          fileName,
          s3Key,
          tempDataFile.getFileName());

      // It is possible that the eTag changed between the time we fetched meta data and the
      // time we downloaded the object.
      objectDetails = downloadS3Object(s3Key, tempDataFile);
      eTag = objectDetails.getETag();
      cacheFile = cacheFilePath(fileName, eTag);

      try {
        log.info(
            "adding downloaded file to cache: fileName={} s3Key={} cacheFile={}",
            fileName,
            s3Key,
            cacheDirectory.relativize(cacheFile));
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
          cacheDirectory.relativize(cacheFile));
      return new DownloadedFile(fileName, objectDetails, cacheFile);
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
      closer.close(this::deleteAllDirectories);
      closer.close(() -> Files.deleteIfExists(cacheDirectory));
      closer.finish();
    }
  }

  /**
   * Delete all cached files for the given object file name and return number of files actually
   * deleted. This is a purely local operation. It does not affect S3.
   *
   * @param fileName simple file name as returned in previous call to {@link #readFileNames}
   * @return number of cached files actually deleted
   * @throws IOException various exceptions might be thrown by the Java
   */
  @CanIgnoreReturnValue
  public int deleteCachedFiles(String fileName) throws IOException {
    var wildcardPath = cacheFilePath(fileName, "*");
    var matchingPaths = cacheFilePaths(wildcardPath);
    int count = 0;
    for (Path path : matchingPaths) {
      final var deleted = Files.deleteIfExists(path);
      if (deleted) {
        count += 1;
      }
    }
    return count;
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
   * Deletes all directories in our directory.
   *
   * @return number of directories deleted
   * @throws IOException pass through from any failed operation
   */
  @CanIgnoreReturnValue
  public int deleteAllDirectories() throws IOException {
    int deletedCount = 0;
    var directories = readCacheSubDirectoryNamesFromDirectory();
    for (String directory : directories) {
      Path path = Path.of(directory);
      if (isCacheSubDirectory(path) && Files.deleteIfExists(path)) {
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
   * Gets the number of bytes of usable disk space from the file system containing our cache
   * directory.
   *
   * @return number of bytes
   * @throws IOException pass through from file system check
   */
  public long getAvailableDiskSpaceInBytes() throws IOException {
    return Files.getFileStore(cacheDirectory).getUsableSpace();
  }

  /**
   * Produce a {@link List} containing an equivalent simple file name for every valid object in our
   * S3 bucket/directory. The simple file name is everything after the S3 directory portion of the
   * object key.
   *
   * @return the list
   */
  private List<String> readFileNamesFromS3() {
    return readS3ObjectListing().stream()
        .map(S3Dao.S3ObjectSummary::getKey)
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
    return readS3ObjectListing().stream()
        .filter(s3Object -> isValidS3Key(s3Object.getKey()))
        .map(this::cacheFilePath)
        .map(Path::toString)
        .collect(ImmutableSet.toImmutableSet());
  }

  /**
   * Produce a {@link Set} containing the path for every file in our directory.
   *
   * @return the set
   */
  private Set<String> readCacheFileNamesFromDirectory() throws IOException {
    try (Stream<Path> filesStream = Files.walk(cacheDirectory)) {
      return filesStream
          .filter(Files::isRegularFile)
          .filter(this::isCacheFile)
          .map(Path::toString)
          .collect(ImmutableSet.toImmutableSet());
    }
  }

  /**
   * Produce a {@link List} containing the path for every sub-directory in our directory. Directory
   * names are sorted by path length in reverse order so that deeper sub-directories appear before
   * shallower ones for any given directory tree. This simplifies removing the directories.
   *
   * @return the list
   */
  private List<String> readCacheSubDirectoryNamesFromDirectory() throws IOException {
    try (Stream<Path> filesStream = Files.walk(cacheDirectory)) {
      return filesStream
          .filter(Files::isDirectory)
          .map(Path::toString)
          .sorted(Comparator.comparingInt(String::length).reversed())
          .collect(ImmutableList.toImmutableList());
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
  private List<S3Dao.S3ObjectSummary> readS3ObjectListing() {
    if (Strings.isNullOrEmpty(s3DirectoryPath)) {
      return s3Dao.listObjects(s3BucketName).toList();
    } else {
      return s3Dao.listObjects(s3BucketName, s3DirectoryPath).toList();
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
   * Convert a simple file name plus the file's eTag into a {@link Path} referencing a file in our
   * cache directory.
   *
   * @param fileName simple file name as exposed to clients
   * @param eTag eTag value from S3 for the file
   * @return file handle
   */
  @VisibleForTesting
  Path cacheFilePath(String fileName, String eTag) {
    eTag = normalizeEtag(eTag);
    // ':' is not compatible with windows so convert it into something that should work everywhere
    fileName = fileName.replace(":", "_--_");
    var lastSepOffset = fileName.lastIndexOf('/');
    var dirName = lastSepOffset < 0 ? "" : fileName.substring(0, lastSepOffset + 1);
    var baseName = lastSepOffset < 0 ? fileName : fileName.substring(lastSepOffset + 1);
    var cacheFileName = dirName + DataFilePrefix + baseName + EtagSeparator + eTag + DataFileSuffix;
    return Path.of(cacheDirectory.toString(), cacheFileName);
  }

  /**
   * Convert a {@link Path} containing wildcard pattern into a list of all files in the cache that
   * correspond to that file.
   *
   * @param wildcardPath path containing wildcards that should match local files
   * @return list of matching files
   */
  private List<Path> cacheFilePaths(Path wildcardPath) {
    final var directory = wildcardPath.getParent().toFile();
    final FileFilter fileFilter =
        WildcardFileFilter.builder().setWildcards(wildcardPath.getFileName().toString()).get();
    final var matchingFiles = directory.listFiles(fileFilter);
    if (matchingFiles == null || matchingFiles.length == 0) {
      return ImmutableList.of();
    } else {
      return Stream.of(matchingFiles)
          .map(f -> Path.of(directory.toString(), f.getName()))
          .collect(ImmutableList.toImmutableList());
    }
  }

  /**
   * Convert an {@link S3Dao.S3ObjectSummary} into a {@link Path} referencing a file in our cache
   * directory.
   *
   * @param s3Object {@link S3Dao.S3ObjectSummary} for object to store in cache
   * @return file handle
   */
  private Path cacheFilePath(S3Dao.S3ObjectSummary s3Object) {
    final var fileName = convertS3KeyToFileName(s3Object.getKey());
    return cacheFilePath(fileName, s3Object.getETag());
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
   * Verifies that the given {@link Path} is one created by us to store an S3 object in our cache
   * directory.
   *
   * @param path file to check
   * @return true if file has a valid data file name
   */
  private boolean isCacheSubDirectory(Path path) throws IOException {
    final var cachePath = cacheDirectory.toFile().getCanonicalPath();
    final var directoryPath = path.toFile().getCanonicalPath();
    return Files.isDirectory(path) && directoryPath.startsWith(cachePath);
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
   * Read {@link S3Dao.S3ObjectDetails} metadata for the given S3 key. Recognize the possible case
   * of object not found (HTTP 404) by throwing more useful {@link FileNotFoundException}.
   *
   * @param fileName the simple file name for the object
   * @param s3Key the S3 object key
   * @return the meta data
   * @throws IOException with {@link FileNotFoundException} or an AWS runtime exception
   */
  private S3Dao.S3ObjectDetails readS3ObjectMetaData(String fileName, String s3Key)
      throws IOException {
    try {
      return s3Dao.readObjectMetaData(s3BucketName, s3Key);
    } catch (NoSuchKeyException | NoSuchBucketException e) {
      var fileNotFound = new FileNotFoundException(fileName);
      fileNotFound.addSuppressed(e);
      throw fileNotFound;
    }
  }

  /**
   * Download S3 object and return its {@link S3Dao.S3ObjectDetails}. Recognize the possible case of
   * object not found (HTTP 404) by throwing more useful {@link FileNotFoundException}.
   *
   * @param s3Key the S3 object key
   * @param tempDataFile where to store the downloaded object
   * @return the meta data
   * @throws FileNotFoundException if object or key do not exist
   */
  private S3Dao.S3ObjectDetails downloadS3Object(String s3Key, Path tempDataFile)
      throws FileNotFoundException {
    try {
      return s3Dao.downloadObject(s3BucketName, s3Key, tempDataFile);
    } catch (CompletionException e) {
      final var cause = e.getCause();
      if (cause instanceof NoSuchKeyException || cause instanceof NoSuchBucketException) {
        final var fileName = convertS3KeyToFileName(s3Key);
        final var fileNotFound = new FileNotFoundException(fileName);
        fileNotFound.addSuppressed(e);
        throw fileNotFound;
      }
      throw e;
    }
  }

  /**
   * Strips quotes from around an etag to keep cached file paths clean.
   *
   * @param eTag e-tag value as returned by S3
   * @return e-tag without quotes
   */
  @VisibleForTesting
  static String normalizeEtag(String eTag) {
    if (eTag.startsWith("\"")) {
      return eTag.substring(1, eTag.length() - 1);
    } else {
      return eTag;
    }
  }

  /**
   * Contains all of the data for a cache file. Provides helper methods to allow callers to interact
   * with the file without actually having a handle to it.
   */
  @AllArgsConstructor
  public class DownloadedFile {
    /** The S3 key from which file was downloaded. */
    @Getter private final String s3Key;

    /** Details reported by S3 when the file was downloaded. Includes meta data key-value pairs. */
    @Getter private final S3Dao.S3ObjectDetails s3Details;

    /** Path to the file in our cache. */
    private final Path path;

    /**
     * Returns a {@link ByteSource} that can be used to read data from the cached file.
     *
     * @return the byte source
     */
    public ByteSource getBytes() {
      return createByteSourceForCachedFile(s3Key, path);
    }

    /**
     * Deletes the file from the cache.
     *
     * @throws IOException pass through if deletion fails
     */
    public void delete() throws IOException {
      deleteCachedFiles(s3Key);
    }

    /**
     * Absolute path to the file within the cache. Intended for use in logging NOT to allow a
     * backdoor to modifying the file itself.
     *
     * @return the absolute path
     */
    public String getAbsolutePath() {
      return path.toAbsolutePath().toString();
    }
  }
}
