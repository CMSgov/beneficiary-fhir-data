package gov.cms.bfd.model.rif;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * Models a {@link Stream} of {@link RifFileRecords}, produced from a single {@link RifFile} in a
 * {@link RifFilesEvent}.
 */
public final class RifFileRecords {
  /** The {@link RifFileEvent} that the {@link #getRecords()} {@link Stream} was produced from. */
  private final RifFileEvent sourceEvent;

  /** The {@link Stream} of {@link RifRecordEvent}s that was produced from the {@link RifFile}. */
  private final Stream<RifRecordEvent<?>> records;

  /**
   * Constructs a new {@link RifFileRecords} instance.
   *
   * @param sourceEvent the value to use for {@link #getSourceEvent()}
   * @param records the value to use for {@link #getRecords()}
   */
  public RifFileRecords(RifFileEvent sourceEvent, Stream<RifRecordEvent<?>> records) {
    Objects.requireNonNull(sourceEvent);
    Objects.requireNonNull(records);

    this.sourceEvent = sourceEvent;
    this.records = records;
  }

  /**
   * Gets the {@link #sourceEvent}.
   *
   * @return the {@link RifFileEvent} that the {@link #getRecords()} {@link Stream} was produced
   *     from
   */
  public RifFileEvent getSourceEvent() {
    return sourceEvent;
  }

  /**
   * Gets the {@link #records}.
   *
   * @return the {@link Stream} of {@link RifRecordEvent}s that was produced from the {@link
   *     RifFile}
   */
  public Stream<RifRecordEvent<?>> getRecords() {
    return records;
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("RifFileRecords [sourceEvent=");
    builder.append(sourceEvent);
    builder.append("]");
    return builder.toString();
  }
}
