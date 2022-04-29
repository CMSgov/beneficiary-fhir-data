package gov.cms.bfd.model.rda;

import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.PrePersist;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/** JPA class for the MessageError table */
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Table(name = "message_error", schema = "rda")
public class MessageError {
  private static final String SequenceName = "rda_api_claim_message_errors_id_seq";

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = SequenceName)
  @SequenceGenerator(
      name = SequenceName,
      schema = "rda",
      sequenceName = SequenceName,
      allocationSize = 25)
  @EqualsAndHashCode.Include
  private Long id;

  /** The message sequence number. */
  @Column(name = "sequence_number", nullable = false)
  @EqualsAndHashCode.Include
  private Long sequenceNumber;

  /** Either dcn (FISS) or idrClmHdIcn (MCS). */
  @Column(name = "claim_id", length = 25, nullable = false)
  private String claimId;

  /** Either F (FISS) or M (MCS). */
  @Column(name = "claim_type", nullable = false)
  @EqualsAndHashCode.Include
  private Character claimType;

  /** Timestamp when we inserted the claim from this message into our database. */
  @Column(name = "received_date")
  private Instant receivedDate;

  @Column(name = "errors")
  @Lob
  private String errors;

  /** The original message that was received, represented as a json string */
  @Column(name = "message")
  @Lob
  private String message;

  @PrePersist
  protected void onCreate() {
    receivedDate = Instant.now();
  }
}
