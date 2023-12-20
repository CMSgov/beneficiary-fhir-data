package gov.cms.bfd.model.rif;

import com.codahale.metrics.MetricRegistry;
import java.util.Objects;
import lombok.Getter;

/** Models a single {@link RifFile} within a {@link RifFilesEvent}. */
@Getter
public final class RifFileEvent {
  /**
   * The {@link MetricRegistry} that should be used to record the work done to process this {@link
   * RifFilesEvent}.
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
  public RifFileEvent(RifFilesEvent parentFilesEvent, RifFile file) {
    Objects.requireNonNull(parentFilesEvent);
    Objects.requireNonNull(file);

    this.eventMetrics = new MetricRegistry();

    this.parentFilesEvent = parentFilesEvent;
    this.file = file;
  }

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
