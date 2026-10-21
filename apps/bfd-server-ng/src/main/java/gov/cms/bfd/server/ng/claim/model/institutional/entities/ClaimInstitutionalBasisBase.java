package gov.cms.bfd.server.ng.claim.model.institutional.entities;

import gov.cms.bfd.server.ng.claim.EobProfileType;
import gov.cms.bfd.server.ng.claim.model.common.PaymentComponent;
import gov.cms.bfd.server.ng.claim.model.common.PaymentComponentBase;
import gov.cms.bfd.server.ng.claim.model.institutional.DateSupportingInfoInstitutional;
import gov.cms.bfd.server.ng.claim.model.institutional.SupportingInfoInstitutional;
import jakarta.persistence.Embedded;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

/** the Institutional-Basis base class. */
@Getter
@MappedSuperclass
abstract class ClaimInstitutionalBasisBase extends ClaimInstitutionalBase {

  @Embedded private SupportingInfoInstitutional supportingInfo;
  @Embedded private DateSupportingInfoInstitutional dateSupportingInfoInstitutional;
  @Embedded private PaymentComponent paymentComponent;

  // region Overrides

  @Override
  public PaymentComponentBase getPaymentComponent() {
    return paymentComponent;
  }

  @Override
  public EobProfileType getClaimProfile() {
    return EobProfileType.BASIS;
  }

  // endregion
}
