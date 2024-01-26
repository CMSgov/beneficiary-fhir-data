package gov.cms.bfd.pipeline.rda.grpc.server;

import com.google.common.annotations.VisibleForTesting;
import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import gov.cms.bfd.pipeline.sharedutils.s3.S3DirectoryDao;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * To save on the number of unnecessary calls to S3 to retrieve files and to allow the incremental
 * addition of files over time this factory class checks an S3 bucket for files matching a pattern
 * that consists of a prefix, the range of sequence numbers in the file, and a suffix. If no files
 * matching this pattern are found but a file has a name equal to the prefix followed by the suffix
 * then that file is considered to hold all valid claims. When one or more files match they are
 * sorted by range and only those files containing sequence numbers greater than or equal to the
 * desired starting number are read. The matching files are then served one after another in
 * sequence number order.
 *
 * <p>NOTE: The files can have overlapping sequence numbers and/or sequence numbers that don't
 * correspond to the values in the object key. No effort is made to compensate for configuration
 * errors. As a consequence if the files contain data that differs from their file names or if they
 * have records out of order within the file the resulting stream of records might not be in
 * increasing order by sequence number.
 *
 * @param <T> type of objects contained in files stored in the S3 bucket
 */
public class S3BucketMessageSourceFactory<T> {
  /** Used to access data from S3 bucket. */
  private final S3DirectoryDao s3Dao;

  /**
   * A function that, when passed an S3 object key, produces a {@link MessageSource} that parses the
   * S3 object to produce messages.
   */
  private final Function<String, MessageSource<T>> s3ObjectParser;

  /** The pattern to use to find files from S3. */
  private final Pattern matchPattern;

  /**
   * Instantiates a new S3 bucket message source factory.
   *
   * @param s3Dao used to access data from S3 bucket
   * @param filePrefix the file prefix
   * @param fileSuffix the file suffix
   * @param s3ObjectParser used to turn S3 object keys into {@link MessageSource}s
   */
  public S3BucketMessageSourceFactory(
      S3DirectoryDao s3Dao,
      String filePrefix,
      String fileSuffix,
      Function<String, MessageSource<T>> s3ObjectParser) {
    this.s3Dao = s3Dao;
    this.s3ObjectParser = s3ObjectParser;
    matchPattern =
        Pattern.compile(
            String.format("^%s(-(\\d+)-(\\d+))?\\.%s(\\.gz)?$", filePrefix, fileSuffix),
            Pattern.CASE_INSENSITIVE);
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
   * Creates a {@link MessageSource} that produces messages with sequence number greater than or
   * equal to the provided one.
   *
   * @param sequenceNumber minimum sequence number desired by the caller
   * @return a MessageSource pulling records from the bucket
   * @throws Exception if the source could not be created
   */
  public MessageSource<T> createMessageSource(long sequenceNumber) throws Exception {
    List<FileEntry> entries = listFiles(sequenceNumber);
    return new MultiS3MessageSource(entries).skipTo(sequenceNumber);
  }

  /**
   * Searches the S3 bucket for objects whose key matches our regular expression. Only files whose
   * maximum sequence number is greater than or equal to our starting sequence number are retained.
   * The resulting list of entries is sorted by sequence number order.
   *
   * @param startingSequenceNumber smallest sequence number that the caller is interested in
   *     processing
   * @return a list of matching {@link FileEntry}s sorted by sequence number
   */
  @VisibleForTesting
  List<FileEntry> listFiles(long startingSequenceNumber) {
    List<FileEntry> entries = new ArrayList<>();
    List<String> fileNames = s3Dao.readFileNames();
    for (String fileName : fileNames) {
      Matcher matcher = matchPattern.matcher(fileName);
      if (matcher.matches()) {
        FileEntry entry;
        if (matcher.group(1) != null) {
          long firstSeqNum = Long.parseLong(matcher.group(2));
          long lastSeqNum = Long.parseLong(matcher.group(3));
          entry = new FileEntry(fileName, firstSeqNum, lastSeqNum);
        } else {
          entry = new FileEntry(fileName, RdaChange.MIN_SEQUENCE_NUM, Long.MAX_VALUE);
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
     * Calls {@link MessageSource#skipTo} on each source in turn until if finds one that still has
     * messages to return or it runs out of sources to try.
     *
     * <p>{@inheritDoc}
     */
    @Override
    public synchronized MessageSource<T> skipTo(long startingSequenceNumber) throws Exception {
      current.skipTo(startingSequenceNumber);
      while (remaining.size() > 0 && !current.hasNext()) {
        current.close();
        current = s3ObjectParser.apply(remaining.remove(0).objectKey);
        current.skipTo(startingSequenceNumber);
      }
      return this;
    }

    /**
     * Checks the current source for more records. If the current source has no more records it
     * closes that source and finds the next available source that does have a record. Sources are
     * closed along the way to ensure only the current source is open at any given time.
     *
     * <p>{@inheritDoc}
     */
    @Override
    public synchronized boolean hasNext() throws Exception {
      if (current.hasNext()) {
        return true;
      }
      while (remaining.size() > 0 && !current.hasNext()) {
        current.close();
        current = s3ObjectParser.apply(remaining.remove(0).objectKey);
      }
      return current.hasNext();
    }

    @Override
    public synchronized T next() throws Exception {
      return current.next();
    }

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
