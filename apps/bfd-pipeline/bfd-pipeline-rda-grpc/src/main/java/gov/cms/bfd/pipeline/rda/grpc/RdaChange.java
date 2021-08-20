package gov.cms.bfd.pipeline.rda.grpc;

import java.time.Instant;
import lombok.Getter;

/**
 * RDA API wraps each claim in a `ClaimChange` object that indicates the type of change. This class
 * wraps that concept into an immutable bean used by the sink classes to optimize how they write
 * claims to the database.
 *
 * @param <T> The database entity type for the change.
 */
@Getter
public class RdaChange<T> {
  public static final long MIN_SEQUENCE_NUM = 0;

  public enum Type {
    INSERT,
    UPDATE,
    DELETE
  }

  private final long sequenceNumber;
  private final Type type;
  private final T claim;
  private final Instant timestamp;

  public RdaChange(long sequenceNumber, Type type, T claim, Instant timestamp) {
    this.sequenceNumber = sequenceNumber;
    this.type = type;
    this.claim = claim;
    this.timestamp = timestamp;
  }
}
