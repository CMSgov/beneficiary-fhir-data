package gov.cms.bfd.server.ng.claim.model.institutional.entities;

import gov.cms.bfd.server.ng.claim.model.common.ClaimPaymentComponent;
import gov.cms.bfd.server.ng.claim.model.common.ClaimPaymentComponentBase;
import jakarta.persistence.Embedded;
import javax.annotation.processing.Generated;

/** the institutional claim basis profile base. */
@Generated("TODO - Remove after query optimization implementation")
public abstract class ClaimInstitutionalBasisBase extends ClaimInstitutionalBase {

  // region PaymentComponent

  // Basis does not have a payment amount, just the date
  @Embedded private ClaimPaymentComponent paymentComponent;

  @Override
  public ClaimPaymentComponentBase getPaymentComponent() {
    return paymentComponent;
  }

  // endregion
}
