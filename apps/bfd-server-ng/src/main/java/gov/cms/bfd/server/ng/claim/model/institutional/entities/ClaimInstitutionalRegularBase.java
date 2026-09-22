package gov.cms.bfd.server.ng.claim.model.institutional.entities;

import gov.cms.bfd.server.ng.claim.model.common.AdjudicationChargeType;
import gov.cms.bfd.server.ng.claim.model.common.PaymentComponentAmount;
import gov.cms.bfd.server.ng.claim.model.common.PaymentComponentBase;
import gov.cms.bfd.server.ng.claim.model.institutional.SupportingInfoDateInstitutional;
import gov.cms.bfd.server.ng.claim.model.institutional.SupportingInfoInstitutional;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.MappedSuperclass;
import java.math.BigDecimal;
import java.util.List;
import javax.annotation.processing.Generated;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** The institutional claim, regular profile base class. */
@Getter
@MappedSuperclass
@Generated("TODO - Remove after query optimization implementation")
public abstract class ClaimInstitutionalRegularBase extends ClaimInstitutionalBase {

  @Column(name = "clm_mdcr_instnl_bene_pd_amt")
  private BigDecimal benePaidAmount;

  @Embedded private SupportingInfoDateInstitutional supportingInfoDateInstitutional;
  @Embedded private SupportingInfoInstitutional supportingInfo; /**/

  // region PaymentComponent
  @Embedded private PaymentComponentAmount paymentComponent;

  @Override
  public PaymentComponentBase getPaymentComponent() {
    return paymentComponent;
  }

  // endregion

  @Override
  protected void addSubclassAdjudication(ExplanationOfBenefit eob) {
    eob.addTotal(AdjudicationChargeType.BENE_PAID_AMOUNT.toFhirTotal(getBenePaidAmount()));
  }

  @Override
  protected List<ExplanationOfBenefit.SupportingInformationComponent>
      buildSubclassSupportingInfo() {
    return List.of();
  }
}
