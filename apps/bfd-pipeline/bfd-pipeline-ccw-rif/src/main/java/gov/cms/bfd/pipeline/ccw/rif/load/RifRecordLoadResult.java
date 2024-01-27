package gov.cms.bfd.pipeline.ccw.rif.load;

import gov.cms.bfd.model.rif.RifRecordEvent;
import lombok.Getter;

/**
 * Represents the results of successful {@link RifLoader} operations on individual {@link
 * RifRecordEvent}s.
 */
@Getter
public final class RifRecordLoadResult {
  /** The record that this result corresponds to. */
  private final RifRecordEvent<?> rifRecordEvent;

  /** The action taken with the record. */
  private final LoadAction loadAction;

  /**
   * Constructs a new {@link RifRecordLoadResult}.
   *
   * @param rifRecordEvent the value to use for {@link #rifRecordEvent}
   * @param loadAction the value to use for {@link #loadAction}
   */
  public RifRecordLoadResult(RifRecordEvent<?> rifRecordEvent, LoadAction loadAction) {
    if (rifRecordEvent == null) throw new IllegalArgumentException();
    if (loadAction == null) throw new IllegalArgumentException();

    this.rifRecordEvent = rifRecordEvent;
    this.loadAction = loadAction;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("RifRecordLoadResult [rifRecordEvent=");
    builder.append(rifRecordEvent);
    builder.append(", loadAction=");
    builder.append(loadAction);
    builder.append("]");
    return builder.toString();
  }

  /**
   * Gets the record number of the record that was loaded.
   *
   * @return the record number
   */
  public long getRecordNumber() {
    return rifRecordEvent.getRecordNumber();
  }

  /** Enumerates the types of actions that a load operation may have resulted in on the database. */
  public enum LoadAction {
    /** Indicates that the record(s) were successfully added to the database. */
    INSERTED,

    /** Indicates that the record(s) were successfully updated to the database. */
    UPDATED,

    /**
     * Indicates that the record(s) were skipped, presumably because they were already present in
     * the database.
     */
    DID_NOTHING;
  }
}
