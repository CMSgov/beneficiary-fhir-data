package gov.cms.bfd.server.ng.claim.model.institutional.entities;

import gov.cms.bfd.server.ng.claim.model.common.PaymentComponent;
import gov.cms.bfd.server.ng.claim.model.common.PaymentComponentBase;
import gov.cms.bfd.server.ng.claim.model.institutional.SupportingInfoDateInstitutional;
import gov.cms.bfd.server.ng.claim.model.institutional.SupportingInfoInstitutional;
import jakarta.persistence.Embedded;
import jakarta.persistence.MappedSuperclass;
import javax.annotation.processing.Generated;
import lombok.Getter;

/** the Institutional-Basis base class. */
@Getter
@MappedSuperclass
@Generated("TODO - Remove after query optimization implementation")
abstract class ClaimInstitutionalBasisBase extends ClaimInstitutionalBase {

  @Embedded private SupportingInfoInstitutional supportingInfo;
  @Embedded private SupportingInfoDateInstitutional supportingInfoDateInstitutional;
  @Embedded private PaymentComponent paymentComponent;

  @Override
  public PaymentComponentBase getPaymentComponent() {
    return paymentComponent;
  }
}
