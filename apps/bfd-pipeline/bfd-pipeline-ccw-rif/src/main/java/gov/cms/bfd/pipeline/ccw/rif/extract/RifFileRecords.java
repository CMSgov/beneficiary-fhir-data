package gov.cms.bfd.pipeline.ccw.rif.extract;

import gov.cms.bfd.model.rif.RifFile;
import gov.cms.bfd.model.rif.RifFileEvent;
import gov.cms.bfd.model.rif.RifRecordEvent;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.Getter;
import reactor.core.publisher.Flux;

/**
 * Models a {@link Flux} of {@link RifRecordEvent}s, produced from a single {@link RifFileEvent}.
 */
@Getter
public final class RifFileRecords {
  /** The {@link RifFileEvent} that the {@link #getRecords()} {@link Stream} was produced from. */
  private final RifFileEvent sourceEvent;

  /** The {@link Flux} that publishes {@link RifRecordEvent}s from the {@link RifFile}. */
  private final Flux<RifRecordEvent<?>> records;

  /**
   * Constructs a new {@link RifFileRecords} instance.
   *
   * @param sourceEvent the value to use for {@link #sourceEvent}
   * @param records the value to use for {@link #records}
   */
  public RifFileRecords(RifFileEvent sourceEvent, Flux<RifRecordEvent<?>> records) {
    Objects.requireNonNull(sourceEvent);
    Objects.requireNonNull(records);

    this.sourceEvent = sourceEvent;
    this.records = records;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("RifFileRecords [sourceEvent=");
    builder.append(sourceEvent);
    builder.append("]");
    return builder.toString();
  }
}
