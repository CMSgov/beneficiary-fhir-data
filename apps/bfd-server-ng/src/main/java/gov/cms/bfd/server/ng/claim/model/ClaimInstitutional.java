package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Entity
@Table(name = "claim_institutional", schema = "idr")
public class ClaimInstitutional {
  @Id
  @Column(name = "clm_uniq_id")
  private long claimUniqueId;

  @OneToOne(mappedBy = "claimInstitutional")
  private Claim claim;

  @Embedded private ClaimInstitutionalSupportingInfo supportingInfo;
  @Embedded private PpsDrgWeight ppsDrgWeight;
  @Embedded private ClaimInstitutionalExtensions extensions;
  @Embedded private BenefitBalanceInstitutional benefitBalanceInstitutional;
}
