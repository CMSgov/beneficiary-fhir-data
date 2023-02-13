package gov.cms.bfd.pipeline.rda.grpc.sink.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Tests the {@link SequenceNumberTracker}. */
public class SequenceNumberTrackerTest {
  /**
   * Verifies that the sequence number tracker initialized with a starting sequence number will
   * return that number when {@link SequenceNumberTracker#getSafeResumeSequenceNumber} is called.
   */
  @Test
  public void defaultValueReturnedWhenEmpty() {
    final var tracker = new SequenceNumberTracker(100);
    assertEquals(100, tracker.getSafeResumeSequenceNumber());
  }

  /**
   * Verifies that the sequence number tracker will keep track of the highest known written sequence
   * number. Note that sequence numbers are only counted as written once removed.
   */
  @Test
  public void maxValueReturnedOnceEmpty() {
    final var tracker = new SequenceNumberTracker(100);
    tracker.addActiveSequenceNumber(101);
    tracker.addActiveSequenceNumber(102);
    tracker.removeWrittenSequenceNumber(102);
    tracker.removeWrittenSequenceNumber(101);
    assertEquals(102, tracker.getSafeResumeSequenceNumber());
  }

  /**
   * Verifies that the sequence number tracker will only update the highest known sequence number
   * (safeResumeSequenceNumber) when the sequence number is written (removed from the list of active
   * sequence numbers), and only when the new number can make an unbroken sequential chain from the
   * old safeResumeSequenceNumber. If a non-sequential safeResumeSequenceNumber is written, and at a
   * later time another sequence number is written which would then create a sequential chain to the
   * new number, the highest seen number becomes the new safeResumeSequenceNumber.
   */
  @Test
  public void tracksRemovedNumbersProperly() {
    final var tracker = new SequenceNumberTracker(100);
    tracker.addActiveSequenceNumber(101);
    assertEquals(100, tracker.getSafeResumeSequenceNumber());

    tracker.addActiveSequenceNumber(102);
    assertEquals(100, tracker.getSafeResumeSequenceNumber());

    tracker.addActiveSequenceNumber(103);
    assertEquals(100, tracker.getSafeResumeSequenceNumber());

    tracker.removeWrittenSequenceNumber(102);
    assertEquals(100, tracker.getSafeResumeSequenceNumber());

    tracker.removeWrittenSequenceNumber(101);
    assertEquals(102, tracker.getSafeResumeSequenceNumber());

    tracker.addActiveSequenceNumber(104);
    assertEquals(102, tracker.getSafeResumeSequenceNumber());

    tracker.removeWrittenSequenceNumber(103);
    assertEquals(103, tracker.getSafeResumeSequenceNumber());

    tracker.removeWrittenSequenceNumber(104);
    assertEquals(104, tracker.getSafeResumeSequenceNumber());
  }
}
