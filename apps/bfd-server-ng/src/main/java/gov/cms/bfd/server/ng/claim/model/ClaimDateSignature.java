package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.ZonedDateTime;
import lombok.Getter;

/** Claim date signature table. */
@Getter
@Entity
@Table(name = "claim_date_signature", schema = "idr")
public class ClaimDateSignature {
  @Id
  @Column(name = "clm_dt_sgntr_sk")
  private long claimDateSignatureSk;

  @Embedded private ClaimDateSupportingInfo supportingInfo;
  @Embedded private ClaimProcessDate claimProcessDate;

  @OneToOne(mappedBy = "claimDateSignature")
  private Claim claim;

  @Column(name = "bfd_updated_ts", nullable = false)
  private ZonedDateTime bfdUpdatedTimestamp;
}
