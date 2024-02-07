package gov.cms.bfd.pipeline.ccw.rif.load;

import gov.cms.bfd.model.rif.RifFile;
import gov.cms.bfd.pipeline.sharedutils.SequenceNumberTracker;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Uses a {@link SequenceNumberTracker} to track progress of loading records from a {@link RifFile}
 * and write progress updates to the database.
 */
@Slf4j
public class RifFileProgressTracker {
  /** The file being tracked. */
  private final RifFile rifFile;

  /** The record number reported by the file when this object was initialized. */
  @Getter private final long startingRecordNumber;

  /** Tracks active record numbers and tells us the most recent safe resume record number. */
  private final SequenceNumberTracker sequenceNumberTracker;

  /**
   * Initializes an instance for the given file.
   *
   * @param rifFile the file we are tracking
   */
  public RifFileProgressTracker(RifFile rifFile) {
    this.rifFile = rifFile;
    startingRecordNumber = rifFile.getLastRecordNumber();
    sequenceNumberTracker = new SequenceNumberTracker(startingRecordNumber);
  }

  /**
   * Informs us that a record is being processed.
   *
   * @param recordNumber record number being processed
   */
  public synchronized void recordActive(long recordNumber) {
    sequenceNumberTracker.addActiveSequenceNumber(recordNumber);
  }

  /**
   * Informs us that processing of an active record has been completed.
   *
   * @param recordNumber record number being processed
   */
  public synchronized void recordComplete(long recordNumber) {
    sequenceNumberTracker.removeWrittenSequenceNumber(recordNumber);
  }

  /**
   * Writes the current safe resume value to the database by calling {@link
   * RifFile#updateLastRecordNumber}.
   */
  public synchronized void writeProgress() {
    long lastRecordNumber = sequenceNumberTracker.getSafeResumeSequenceNumber();
    if (lastRecordNumber > rifFile.getLastRecordNumber()) {
      rifFile.updateLastRecordNumber(lastRecordNumber);
      log.debug("updated lastRecordNumber to {}", lastRecordNumber);
    } else {
      log.debug("no update because {} <= {}", lastRecordNumber, rifFile.getLastRecordNumber());
    }
  }
}
