package gov.cms.bfd.pipeline.rda.grpc.server;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.common.annotations.VisibleForTesting;
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

/**
 * To save on the number of unnecessary calls to S3 to retrieve files and to allow the incremental
 * addition of files over time this implementation of {@code MessageSource.Factory} checks an S3
 * bucket for files matching a pattern that contains the claim type and the range of sequence
 * numbers in the file. If no files matching this pattern are found but a file has a name equal to
 * the claim type plus the ndjson suffix then that file is considered to hold all valid claims. When
 * one or more files match they are sorted by range and only those files containing sequence numbers
 * greater than or equal to the desired starting number are read. The matching files are then served
 * one after another in sequence number order.
 *
 * <p>NOTE: The files can have overlapping sequence numbers and/or sequence numbers that don't
 * correspond to the values in the object key. No effort is made to compensate for configuration
 * errors. As a consequence if the files contain data that differs from their file names or if they
 * have records out of order within the file the resulting stream of records might not be in
 * monotonically increasing order by sequence number.
 */
public class S3BucketMessageSourceFactory<T> implements MessageSource.Factory<T> {

  private final AmazonS3 s3Client;
  private final String bucketName;
  private final Function<String, MessageSource<T>> actualFactory;
  private final Function<T, Long> sequenceNumberGetter;
  private final Pattern matchPattern;

  public S3BucketMessageSourceFactory(
      AmazonS3 s3Client,
      String bucketName,
      String filePrefix,
      String fileSuffix,
      Function<String, MessageSource<T>> actualFactory,
      Function<T, Long> sequenceNumberGetter) {
    this.s3Client = s3Client;
    this.bucketName = bucketName;
    this.actualFactory = actualFactory;
    this.sequenceNumberGetter = sequenceNumberGetter;
    matchPattern =
        Pattern.compile(
            String.format("^%s(-(\\d+)-(\\d+))?\\.%s$", filePrefix, fileSuffix),
            Pattern.CASE_INSENSITIVE);
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
   * maximum sequence number is greater than or equal to our starting sequence number of retained.
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
    List<S3ObjectSummary> summaries = s3Client.listObjects(bucketName).getObjectSummaries();
    for (S3ObjectSummary summary : summaries) {
      Matcher matcher = matchPattern.matcher(summary.getKey());
      if (matcher.matches()) {
        FileEntry entry;
        if (matcher.group(1) != null) {
          entry =
              new FileEntry(
                  summary.getKey(),
                  Long.parseLong(matcher.group(2)),
                  Long.parseLong(matcher.group(3)));
        } else {
          entry = new FileEntry(summary.getKey(), RdaChange.MIN_SEQUENCE_NUM, Long.MAX_VALUE);
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
   * sources in the order those sequences are defined. At any given time only one source is being
   * consumed and sources are closed as they are completed.
   */
  private class MultiS3MessageSource implements MessageSource<T> {
    private final List<FileEntry> remaining;
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
     * closes that source and finds the next available source that does have a record.
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
    private final String objectKey;
    private final long minSequenceNumber;
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
