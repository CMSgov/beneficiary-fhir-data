package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;

/** Fiscal Intermediary Standard System claims table. */
@Getter
@Entity
@Table(name = "claim_fiss", schema = "idr")
public class ClaimFiss {
  @Id
  @Column(name = "clm_uniq_id")
  private long claimUniqueId;

  @OneToOne(mappedBy = "claimFiss")
  private Claim claim;
}
