package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;

/** Institutional claims table. */
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

  /**
   * Accessor for the institutional DRG (dgns_drg_cd) value. Kept here so callers in other packages
   * do not need visibility into the internal supporting info embeddable types.
   *
   * @return optional DRG code (trimmed) if present and non-blank
   */
  public java.util.Optional<String> getDrgCode() {
    var code = supportingInfo.getDiagnosisDrgCode().getDiagnosisDrgCode();
    // code is guaranteed non-null; only filter out blank values
    if (code.isBlank()) {
      return java.util.Optional.empty();
    }
    return java.util.Optional.of(code.trim());
  }
}
