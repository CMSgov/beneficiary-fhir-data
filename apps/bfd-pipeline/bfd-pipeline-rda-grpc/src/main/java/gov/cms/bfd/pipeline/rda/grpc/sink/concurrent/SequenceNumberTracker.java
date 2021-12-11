package gov.cms.bfd.pipeline.rda.grpc.sink.concurrent;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Set;
import java.util.TreeSet;

/**
 * Thread safe container for tracking batches of sequence numbers as they are being written to the
 * database. At any given time the sequence numbers that are currently being written to the database
 * are maintained in a Set. They are removed from the set as they are written. At all times the next
 * unwritten sequence number is available as the smallest sequence number in the Set. If the Set is
 * empty then there are no batches in progress and instead the highest that has been written will be
 * returned.
 */
public class SequenceNumberTracker {
  private final Set<Long> openSequenceNumbers = new TreeSet<>();
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

  public synchronized void addSequenceNumber(Long sequenceNumber) {
    openSequenceNumbers.add(sequenceNumber);
    if (sequenceNumber > maxSequenceNumber) {
      maxSequenceNumber = sequenceNumber;
    }
  }

  @CanIgnoreReturnValue
  public synchronized long removeSequenceNumber(Long sequenceNumber) {
    openSequenceNumbers.remove(sequenceNumber);
    return getNextSequenceNumber();
  }

  public synchronized long getNextSequenceNumber() {
    if (openSequenceNumbers.size() > 0) {
      return openSequenceNumbers.iterator().next();
    } else {
      return maxSequenceNumber;
    }
  }
}
