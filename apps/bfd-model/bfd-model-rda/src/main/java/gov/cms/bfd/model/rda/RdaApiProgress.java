package gov.cms.bfd.model.rda;

import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
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
  public enum ClaimType {
    FISS,
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
