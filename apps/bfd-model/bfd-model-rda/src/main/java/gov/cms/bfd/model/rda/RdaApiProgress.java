package gov.cms.bfd.model.rda;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/** JPA class for the RdaApiProgress table */
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Table(name = "rda_api_progress", schema = "rda")
public class RdaApiProgress {
  /** Represents the enum ClaimType. */
  public enum ClaimType {
    /** A FISS claim. */
    FISS,
    /** A MCS claim. */
    MCS
  }

  @Id
  @Enumerated(EnumType.STRING)
  @Column(name = "claim_type", length = 20, nullable = false)
  private ClaimType claimType;

  @Column(name = "last_sequence_number", nullable = false)
  private Long lastSequenceNumber;

  @Column(name = "last_updated")
  private Instant lastUpdated;
}
