package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

/** Institutional claims table. */
@Getter
@Entity
@Table(name = "claim_institutional", schema = "idr")
public class ClaimInstitutional extends ClaimInstitutionalBase {
  @Id
  @Column(name = "clm_uniq_id")
  private long claimUniqueId;
}
