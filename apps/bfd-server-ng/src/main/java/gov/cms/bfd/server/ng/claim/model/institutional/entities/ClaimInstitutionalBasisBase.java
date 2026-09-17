package gov.cms.bfd.server.ng.claim.model.institutional.entities;

import gov.cms.bfd.server.ng.claim.model.common.ClaimPaymentComponent;
import gov.cms.bfd.server.ng.claim.model.common.ClaimPaymentComponentBase;
import gov.cms.bfd.server.ng.claim.model.institutional.DateSupportingInfo;
import gov.cms.bfd.server.ng.claim.model.institutional.InstitutionalSupportingInfo;
import jakarta.persistence.Embedded;
import jakarta.persistence.MappedSuperclass;
import javax.annotation.processing.Generated;
import lombok.Getter;

/** the institutional claim basis profile base. */
@Getter
@MappedSuperclass
@Generated("TODO - Remove after query optimization implementation")
public abstract class ClaimInstitutionalBasisBase extends ClaimInstitutionalBase {

  @Embedded private InstitutionalSupportingInfo supportingInfo;
  @Embedded private DateSupportingInfo dateSupportingInfo;
  @Embedded private ClaimPaymentComponent paymentComponent;

  @Override
  public ClaimPaymentComponentBase getPaymentComponent() {
    return paymentComponent;
  }
}
