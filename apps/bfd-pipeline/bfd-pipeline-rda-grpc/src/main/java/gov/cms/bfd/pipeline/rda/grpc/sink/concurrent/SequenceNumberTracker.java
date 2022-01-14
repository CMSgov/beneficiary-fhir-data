package gov.cms.bfd.pipeline.rda.grpc.sink.concurrent;

import java.util.Set;
import java.util.TreeSet;
import javax.annotation.concurrent.ThreadSafe;

/**
 * The RDA API assigns increasing sequence numbers to every message it returns. Callers can submit a
 * sequence number value to the API when requesting claims so that clients can resume at the point
 * they left off on previous calls. We track these sequence numbers in the database so that our job
 * knows what sequence number to send.
 *
 * <p>Concurrent writes complicate sequence number tracking since there is no guaranteed ordering of
 * how claims are written to the database. The best we can do is track the highest known sequence
 * number for which there are no unwritten records with a lower sequence number.
 *
 * <p>This thread safe container tracks batches of sequence numbers as they are being written to the
 * database. At any given time the sequence numbers that are currently being written to the database
 * are maintained in a Set. They are removed from the Set as they are written. At all times the next
 * unwritten sequence number is available as the smallest sequence number in the Set. If the Set is
 * empty then there are no batches in progress and instead the highest number that has ever been
 * written is known to be safe.
 */
@ThreadSafe
public class SequenceNumberTracker {
  // Use a TreeSet so that values are sorted in ascending order.
  private final Set<Long> activeSequenceNumbers = new TreeSet<>();
  private long maxSequenceNumber;

  /**
   * Creates an object using the specified startingSequenceNumber. This number will be returned
   * until any batches have been started.
   *
   * @param startingSequenceNumber default value for getNextSequenceNumber()
   */
  public SequenceNumberTracker(long startingSequenceNumber) {
    this.maxSequenceNumber = startingSequenceNumber;
  }

  /**
   * Adds an active sequence number to the set. This number will need to be removed once the record
   * has been successfully written.
   *
   * @param sequenceNumber of a record that has been queued for writing to the database
   */
  public synchronized void addActiveSequenceNumber(long sequenceNumber) {
    activeSequenceNumbers.add(sequenceNumber);
    if (sequenceNumber > maxSequenceNumber) {
      maxSequenceNumber = sequenceNumber;
    }
  }

  /**
   * Removes a sequence number that has been successfully written to the database.
   *
   * @param sequenceNumber of a record that has been successfully stored in the database
   */
  public synchronized void removeWrittenSequenceNumber(long sequenceNumber) {
    activeSequenceNumbers.remove(sequenceNumber);
  }

  /**
   * Gets the current sequence number for which we know there are no unwritten records with a lower
   * sequence number.
   *
   * @return the best sequence number to send to the RDA API when fetching claims
   */
  public synchronized long getSafeResumeSequenceNumber() {
    if (activeSequenceNumbers.size() > 0) {
      // Since the set is ordered the first value is lowest unwritten value.
      // We return one less so that caller knows to resume at our first unwritten value.
      return activeSequenceNumbers.iterator().next() - 1;
    } else {
      // If the set is empty the maximum number we've ever seen is the correct value.
      return maxSequenceNumber;
    }
  }
}
