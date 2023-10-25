package gov.cms.bfd.model.rif;

import com.codahale.metrics.MetricRegistry;
import java.util.Objects;

/** Models a single {@link RifFile} within a {@link RifFilesEvent}. */
public final class RifFileEvent {
  /**
   * The {@link MetricRegistry} that should be used to record the work done to process this {@link
   * RifFileRecords}.
   */
  private final MetricRegistry eventMetrics;

  /** The {@link RifFilesEvent} that this {@link RifFileEvent} is a part of. */
  private final RifFilesEvent parentFilesEvent;

  /** The {@link RifFile} represented by this {@link RifFileEvent}. */
  private final RifFile file;

  /**
   * Constructs a new {@link RifFileEvent} instance.
   *
   * @param parentFilesEvent the value to use for {@link #getParentFilesEvent()}
   * @param file the value to use for {@link #getFile()}
   */
  RifFileEvent(RifFilesEvent parentFilesEvent, RifFile file) {
    Objects.requireNonNull(parentFilesEvent);
    Objects.requireNonNull(file);

    this.eventMetrics = new MetricRegistry();

    this.parentFilesEvent = parentFilesEvent;
    this.file = file;
  }

  /**
   * Gets the {@link #eventMetrics}.
   *
   * @return the {@link MetricRegistry} that should be used to record the work done to process this
   *     {@link RifFilesEvent} and its {@link RifRecordEvent}s
   */
  public MetricRegistry getEventMetrics() {
    return eventMetrics;
  }

  /**
   * Gets the {@link #parentFilesEvent}.
   *
   * @return the {@link RifFilesEvent} that this {@link RifFileEvent} is a part of
   */
  public RifFilesEvent getParentFilesEvent() {
    return parentFilesEvent;
  }

  /**
   * Gets the {@link #file}.
   *
   * @return the {@link RifFile} represented by this {@link RifFileEvent}
   */
  public RifFile getFile() {
    return file;
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("RifFileEvent [file=");
    builder.append(file);
    builder.append(", parentFilesEvent.timestamp=");
    builder.append(parentFilesEvent.getTimestamp());
    builder.append("]");
    return builder.toString();
  }
}
