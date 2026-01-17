package gov.cms.bfd.server.ng.coverage.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import java.util.Optional;

@Embeddable
class BeneficiaryCoverageOptional {
  // Hack to ensure this entity is not null. See DESIGN.md for explanation/rationale.
  @Column(name = "bene_sk", insertable = false, updatable = false)
  private long beneSk;

  @OneToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "bene_sk")
  private BeneficiaryStatus beneficiaryStatus;

  @OneToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "bene_sk")
  private BeneficiaryDualEligibility beneficiaryDualEligibility;

  @OneToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "bene_sk")
  private BeneficiaryEntitlementReason beneficiaryEntitlementReason;

  Optional<BeneficiaryStatus> getBeneficiaryStatus() {
    return Optional.ofNullable(beneficiaryStatus);
  }

  Optional<BeneficiaryDualEligibility> getBeneficiaryDualEligibility() {
    return Optional.ofNullable(beneficiaryDualEligibility);
  }

  Optional<BeneficiaryEntitlementReason> getBeneficiaryEntitlementReason() {
    return Optional.ofNullable(beneficiaryEntitlementReason);
  }
}
