package gov.cms.bfd.pipeline.ccw.rif.load;

import gov.cms.bfd.model.rif.RifRecordEvent;

/**
 * Represents the results of successful {@link RifLoader} operations on individual {@link
 * RifRecordEvent}s.
 */
public final class RifRecordLoadResult {
  private final RifRecordEvent<?> rifRecordEvent;
  private final LoadAction loadAction;

  /**
   * Constructs a new {@link RifRecordLoadResult}.
   *
   * @param rifRecordEvent the value to use for {@link #getRifRecordEvent()}
   * @param loadAction the value to use for {@link #getLoadAction()}
   */
  public RifRecordLoadResult(RifRecordEvent<?> rifRecordEvent, LoadAction loadAction) {
    if (rifRecordEvent == null) throw new IllegalArgumentException();
    if (loadAction == null) throw new IllegalArgumentException();

    this.rifRecordEvent = rifRecordEvent;
    this.loadAction = loadAction;
  }

  /** @return the {@link RifRecordEvent} that was loaded */
  public RifRecordEvent<?> getRifRecordEvent() {
    return rifRecordEvent;
  }

  /** @return the {@link LoadAction} that indicates the outcome of the load */
  public LoadAction getLoadAction() {
    return loadAction;
  }

  /** @see java.lang.Object#toString() */
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

  /** Enumerates the types of actions that a load operation may have resulted in on the database. */
  public static enum LoadAction {
    /** Indicates that the record(s) were successfully added to the database. */
    INSERTED,

    /** Indicates that the record(s) were successfully updated to the database. */
    UPDATED,

    /**
     * Indicates that the record(s) were queued for insertion into the database as part of a larger
     * batch. The status of that batch is indeterminate.
     */
    QUEUED,

    /**
     * Indicates that the record(s) were skipped, presumably because they were already present in
     * the database.
     */
    DID_NOTHING;
  }
}
