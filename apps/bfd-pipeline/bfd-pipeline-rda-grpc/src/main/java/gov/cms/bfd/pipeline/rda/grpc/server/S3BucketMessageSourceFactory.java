package gov.cms.bfd.pipeline.rda.grpc.server;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;

/**
 * To save on the number of unnecessary calls to S3 to retrieve files and to allow the incremental
 * addition of files over time this implementation of {@link MessageSource.Factory} checks an S3
 * bucket for files matching a pattern that consists of a prefix, the range of sequence numbers in
 * the file, and a suffix. If no files matching this pattern are found but a file has a name equal
 * to the prefix followed by the suffix then that file is considered to hold all valid claims. When
 * one or more files match they are sorted by range and only those files containing sequence numbers
 * greater than or equal to the desired starting number are read. The matching files are then served
 * one after another in sequence number order.
 *
 * <p>NOTE: The files can have overlapping sequence numbers and/or sequence numbers that don't
 * correspond to the values in the object key. No effort is made to compensate for configuration
 * errors. As a consequence if the files contain data that differs from their file names or if they
 * have records out of order within the file the resulting stream of records might not be in
 * increasing order by sequence number.
 */
public class S3BucketMessageSourceFactory<T> implements MessageSource.Factory<T> {
  /** The client for interacting with AWS S3 buckets and files. */
  private final S3Client s3Client;
  /** The bucket to use for S3 interactions. */
  private final String bucketName;
  /** The directory path to save files to. */
  private final String directoryPath;
  /** A function for getting the message factory to transform the response. */
  private final Function<String, MessageSource<T>> actualFactory;
  /** A function for obtaining the sequence number. */
  private final Function<T, Long> sequenceNumberGetter;
  /** The pattern to use to find files from S3. */
  private final Pattern matchPattern;

  /**
   * Instantiates a new S3 bucket message source factory.
   *
   * @param s3Client the s3 client to do operations with
   * @param bucketName the bucket name to look for files in
   * @param directoryPath the directory path to download files to
   * @param filePrefix the file prefix
   * @param fileSuffix the file suffix
   * @param actualFactory the source factory creation function
   * @param sequenceNumberGetter the function to get the sequence number
   */
  public S3BucketMessageSourceFactory(
      S3Client s3Client,
      String bucketName,
      String directoryPath,
      String filePrefix,
      String fileSuffix,
      Function<String, MessageSource<T>> actualFactory,
      Function<T, Long> sequenceNumberGetter) {
    this.s3Client = s3Client;
    this.bucketName = bucketName;
    this.directoryPath = directoryPath;
    this.actualFactory = actualFactory;
    this.sequenceNumberGetter = sequenceNumberGetter;
    matchPattern =
        Pattern.compile(
            String.format(
                "^%s%s(-(\\d+)-(\\d+))?\\.%s(\\.gz)?$", directoryPath, filePrefix, fileSuffix),
            Pattern.CASE_INSENSITIVE);
  }

  /**
   * Creates a valid object key with the given information about the file. The key will contain the
   * min/max sequence numbers to assist with filtering files.
   *
   * @param filePrefix prefix for the S3 object key
   * @param fileSuffix suffix for the S3 object key
   * @param minSeq lowest sequence number in the file
   * @param maxSeq highest sequence number in the file
   * @return an object key that will match the expected pattern for a S3BucketMessageSourceFactory
   */
  public static String createValidObjectKey(
      String filePrefix, String fileSuffix, long minSeq, long maxSeq) {
    return String.format("%s-%d-%d.%s", filePrefix, minSeq, maxSeq, fileSuffix);
  }

  /**
   * Creates a valid object key with the given information about the file.
   *
   * @param filePrefix prefix for the S3 object key
   * @param fileSuffix suffix for the S3 object key
   * @return an object key that will match the expected pattern for a S3BucketMessageSourceFactory
   */
  public static String createValidObjectKey(String filePrefix, String fileSuffix) {
    return String.format("%s.%s", filePrefix, fileSuffix);
  }

  /**
   * Gets all available NDJSON files in the bucket that contain the specified sequence number and
   * creates a {@link MessageSource} that will return their messages.
   *
   * @param sequenceNumber minimum sequence number desired by the caller
   * @return a MessageSource pulling records from the bucket
   */
  @Override
  public MessageSource<T> apply(long sequenceNumber) throws Exception {
    List<FileEntry> entries = listFiles(sequenceNumber);
    return new MultiS3MessageSource(entries)
        .filter(record -> sequenceNumberGetter.apply(record) >= sequenceNumber);
  }

  /**
   * Searches the S3 bucket for objects whose key matches our regular expression. Only files whose
   * maximum sequence number is greater than or equal to our starting sequence number are retained.
   * The resulting list of entries is sorted by sequence number order.
   *
   * @param startingSequenceNumber smallest sequence number that the caller is interested in
   *     processing
   * @return a List of FileEntries containing the startingSequenceNumber and sorted by sequence
   *     number
   */
  @VisibleForTesting
  List<FileEntry> listFiles(long startingSequenceNumber) {
    List<FileEntry> entries = new ArrayList<>();
    List<S3Object> s3Objects = getObjectListing();
    for (S3Object s3Object : s3Objects) {
      Matcher matcher = matchPattern.matcher(s3Object.key());
      if (matcher.matches()) {
        FileEntry entry;
        if (matcher.group(1) != null) {
          entry =
              new FileEntry(
                  s3Object.key(),
                  Long.parseLong(matcher.group(2)),
                  Long.parseLong(matcher.group(3)));
        } else {
          entry = new FileEntry(s3Object.key(), RdaChange.MIN_SEQUENCE_NUM, Long.MAX_VALUE);
        }
        if (entry.maxSequenceNumber >= startingSequenceNumber) {
          entries.add(entry);
        }
      }
    }
    entries.sort(FileEntry::compareTo);
    return entries;
  }

  /**
   * Gets the object listing.
   *
   * @return the object listing
   */
  private List<S3Object> getObjectListing() {
    ListObjectsV2Request.Builder listObjectsRequest = ListObjectsV2Request.builder().bucket(bucketName);
    if (!Strings.isNullOrEmpty(directoryPath)) {
      listObjectsRequest.prefix(directoryPath);
    } 
      return s3Client.listObjectsV2(listObjectsRequest.build()).contents();
    }
  }

  /**
   * Compound {@link MessageSource} implementation that pulls records from a sequence of other
   * MessageSource. At any given time only one MessageSource is being consumed. MessageSources are
   * closed as they are completed.
   */
  private class MultiS3MessageSource implements MessageSource<T> {
    /** The list of S3 files yet to be downloaded. */
    private final List<FileEntry> remaining;
    /** The current message source stream. */
    private MessageSource<T> current;

    /**
     * Constructs the source for the list of entries. Starts out with an empty source as current and
     * lets the first call to {@code next()} load the first entry lazily.
     *
     * @param remaining list of S3 entries to pull records from
     */
    private MultiS3MessageSource(List<FileEntry> remaining) {
      this.remaining = remaining;
      current = new EmptyMessageSource<>();
    }

    /**
     * Checks the current source for more records. If the current source has no more records it
     * closes that source and finds the next available source that does have a record. Sources are
     * closed along the way to ensure only the current source is open at any given time.
     *
     * @return true if next can be called successfully
     * @throws Exception if any operation fails
     */
    @Override
    public synchronized boolean hasNext() throws Exception {
      if (current.hasNext()) {
        return true;
      }
      while (remaining.size() > 0 && !current.hasNext()) {
        current.close();
        current = actualFactory.apply(remaining.remove(0).objectKey);
      }
      return current.hasNext();
    }

    /** {@inheritDoc} */
    @Override
    public synchronized T next() throws Exception {
      return current.next();
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void close() throws Exception {
      current.close();
    }
  }

  /**
   * Immutable entry for S3 objects that match the key regex. Contains the min/max known sequence
   * number plus the key used to access the specific object in the bucket. Entries have a natural
   * order based on ascending min/max sequence numbers.
   */
  @VisibleForTesting
  @AllArgsConstructor
  @Getter
  @ToString
  @EqualsAndHashCode
  static class FileEntry implements Comparable<FileEntry> {
    /** The file's S3 key. */
    private final String objectKey;
    /** The minimum sequence number. */
    private final long minSequenceNumber;
    /** The maximum sequence number. */
    private final long maxSequenceNumber;

    /** {@inheritDoc} */
    @Override
    public int compareTo(FileEntry o) {
      if (minSequenceNumber < o.minSequenceNumber) {
        return -1;
      } else if (minSequenceNumber > o.minSequenceNumber) {
        return 1;
      } else if (maxSequenceNumber < o.maxSequenceNumber) {
        return -1;
      } else if (maxSequenceNumber > o.maxSequenceNumber) {
        return 1;
      } else {
        return objectKey.compareTo(o.objectKey);
      }
    }
  }
}
