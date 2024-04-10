package gov.cms.bfd.model.rda;

import java.io.Serializable;
import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/** JPA class for the MessageErrors table */
@Entity
@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@IdClass(MessageError.PK.class)
@Table(name = "message_errors", schema = "rda")
public class MessageError {

  /** The message sequence number. */
  @Id
  @Column(name = "sequence_number", nullable = false)
  @EqualsAndHashCode.Include
  private Long sequenceNumber;

  /** Either F (FISS) or M (MCS). */
  @Id
  @Enumerated(EnumType.STRING)
  @Column(name = "claim_type", length = 20, nullable = false)
  @EqualsAndHashCode.Include
  private ClaimType claimType;

  /** Either claimId (FISS) or idrClmHdIcn (MCS). */
  @Column(name = "claim_id", length = 43, nullable = false)
  @EqualsAndHashCode.Include
  private String claimId;

  /**
   * String specifying the source of the data contained in this record. Generally this will be the
   * version string returned by the RDA API server but when populating data from mock server it will
   * also include information about the mode the server was running in.
   */
  @Column(name = "api_source", length = 24, nullable = false)
  private String apiSource;

  /** Timestamp when we first inserted the error into our database. */
  @Column(name = "created_date", nullable = false, updatable = false)
  private Instant createdDate;

  /** Timestamp when we last updated the error in our database. */
  @Column(name = "updated_date", nullable = false)
  private Instant updatedDate;

  /**
   * A list of transformation errors associated with the RDA message, represented as a json list
   * string
   */
  @Column(name = "errors", nullable = false, columnDefinition = "jsonb")
  private String errors;

  /** The original message that was received, represented as a json string */
  @Column(name = "message", nullable = false, columnDefinition = "jsonb")
  private String message;

  /** The status of this record. */
  @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 20, nullable = false)
  private Status status;

  /** Possible values for {@link #claimType}. */
  public enum ClaimType {
    /** A FISS claim. * */
    FISS,
    /** A MCS claim. * */
    MCS
  }

  /** Possible values for {@link #status}. */
  public enum Status {
    /** Starting state for new errors. */
    UNRESOLVED,
    /** The error has been corrected. */
    RESOLVED,
    /** This sequence number is no longer available in RDA API. */
    OBSOLETE
  }

  /**
   * Gets a primary key object for this record.
   *
   * @return the {@link PK}
   */
  public PK getPK() {
    return new PK(sequenceNumber, claimType);
  }

  /** PK class for the MessageError table */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PK implements Serializable {
    /** The sequence number for the primary key. */
    private Long sequenceNumber;

    /** The claim Type for the primary key. */
    private ClaimType claimType;
  }
}
