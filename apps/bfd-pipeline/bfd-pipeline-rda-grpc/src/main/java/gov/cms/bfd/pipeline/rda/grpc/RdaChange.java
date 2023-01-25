package gov.cms.bfd.pipeline.rda.grpc;

import java.time.Instant;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

/**
 * RDA API wraps each claim in a `ClaimChange` object that indicates the type of change. This class
 * wraps that concept into an immutable bean used by the sink classes to optimize how they write
 * claims to the database.
 *
 * @param <T> The database entity type for the change.
 */
@Getter
@AllArgsConstructor
public class RdaChange<T> {
  /** The minimum sequence number. */
  public static final long MIN_SEQUENCE_NUM = 0;

  /** Represents the change type. */
  public enum Type {
    /** Represents a database insert. */
    INSERT,
    /** Represents a database update. */
    UPDATE,
    /** Represents a database delete. */
    DELETE
  }

  /** The sequence number for the change. */
  private final long sequenceNumber;
  /** The change type. */
  private final Type type;
  /** The claim being changed. */
  private final T claim;
  /** The timestamp of the change. */
  private final Instant timestamp;
  /** The source of the change. */
  private final Source source;

  /** Represents the source of a change. */
  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  @FieldNameConstants
  public static class Source {
    /** The phase. */
    private Short phase;
    /** The phase sequence number. */
    private Short phaseSeqNum;
    /** The extract date. */
    private LocalDate extractDate;
    /** The timestamp of transmission. */
    private Instant transmissionTimestamp;
  }
}
