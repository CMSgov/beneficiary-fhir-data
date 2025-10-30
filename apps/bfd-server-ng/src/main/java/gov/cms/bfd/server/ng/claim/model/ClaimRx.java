package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Embeddable
class ClaimRx {
  @Id
  @Column(name = "clm_uniq_id")
  private long claimUniqueId;

  @OneToOne(mappedBy = "claimRx")
  private Claim claim;

  @Embedded private ClaimRxSupportingInfo supportingInfo;
}
