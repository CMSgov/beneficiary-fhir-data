package gov.cms.bfd.model.rda;

import java.io.Serializable;
import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.Type;

/** JPA class for the MessageError table */
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@IdClass(MessageError.PK.class)
@Table(name = "message_errors", schema = "rda")
public class MessageError {
  public enum ClaimType {
    FISS,
    MCS
  }

  /** The message sequence number. */
  @Id
  @Column(name = "sequence_number", nullable = false)
  @EqualsAndHashCode.Include
  private Long sequenceNumber;

  /** Either dcn (FISS) or idrClmHdIcn (MCS). */
  @Id
  @Column(name = "claim_id", length = 25, nullable = false)
  @EqualsAndHashCode.Include
  private String claimId;

  /** Either F (FISS) or M (MCS). */
  @Id
  @Enumerated(EnumType.STRING)
  @Column(name = "claim_type", length = 20, nullable = false)
  @EqualsAndHashCode.Include
  private ClaimType claimType;

  /** Timestamp when we inserted the claim from this message into our database. */
  @Column(name = "received_date")
  private Instant receivedDate;

  @Column(name = "errors")
  @Type(type = "com.vladmihalcea.hibernate.type.json.JsonType")
  private String errors;

  /** The original message that was received, represented as a json string */
  @Column(name = "message")
  @Type(type = "com.vladmihalcea.hibernate.type.json.JsonType")
  private String message;

  @PrePersist
  protected void onCreate() {
    receivedDate = Instant.now();
  }

  @PreUpdate
  protected void onUpdate() {
    receivedDate = Instant.now();
  }

  /** PK class for the MessageError table */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PK implements Serializable {
    private Long sequenceNumber;
    private String claimId;
    private ClaimType claimType;
  }
}
