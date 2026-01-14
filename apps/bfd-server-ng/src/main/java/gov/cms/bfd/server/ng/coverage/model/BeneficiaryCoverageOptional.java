package gov.cms.bfd.server.ng.coverage.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import java.util.Optional;

@Embeddable
public class BeneficiaryCoverageOptional {
  // HORRIBLE HACK: this is required for this to be non-null
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

  public Optional<BeneficiaryStatus> getBeneficiaryStatus() {
    return Optional.ofNullable(beneficiaryStatus);
  }

  public Optional<BeneficiaryDualEligibility> getBeneficiaryDualEligibility() {
    return Optional.ofNullable(beneficiaryDualEligibility);
  }

  public Optional<BeneficiaryEntitlementReason> getBeneficiaryEntitlementReason() {
    return Optional.ofNullable(beneficiaryEntitlementReason);
  }
}
