package gov.cms.bfd.server.ng.claim.model.institutional.entities;

import gov.cms.bfd.server.ng.claim.model.common.ClaimIdrLoadDate;
import gov.cms.bfd.server.ng.claim.model.common.ClaimPaymentComponentAmount;
import gov.cms.bfd.server.ng.claim.model.common.ClaimPaymentComponentBase;
import jakarta.persistence.Embedded;
import jakarta.persistence.MappedSuperclass;
import java.util.Optional;

/** The institutional claim, full (CMS) profile base class. */
@MappedSuperclass
public abstract class ClaimInstitutionalCmsBase extends ClaimInstitutionalBase {

  // region Claim IDR Load Date
  @Embedded private ClaimIdrLoadDate claimIdrLoadDate;

  @Override
  public Optional<ClaimIdrLoadDate> getClaimIdrLoadDate() {
    return Optional.of(claimIdrLoadDate);
  }

  // endregion

  // region PaymentComponent
  @Embedded private ClaimPaymentComponentAmount paymentComponent;

  @Override
  public ClaimPaymentComponentBase getPaymentComponent() {
    return paymentComponent;
  }

  // endregion
}
