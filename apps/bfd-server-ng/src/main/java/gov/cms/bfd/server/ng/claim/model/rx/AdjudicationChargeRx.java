package gov.cms.bfd.server.ng.claim.model.rx;

import gov.cms.bfd.server.ng.claim.model.common.AdjudicationChargeBase;
import gov.cms.bfd.server.ng.claim.model.common.AdjudicationChargeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.List;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** The adjudication charge for a pharmacy claim. */
@Embeddable
public class AdjudicationChargeRx implements AdjudicationChargeBase {

  @Column(name = "clm_bene_pmt_amt")
  private BigDecimal benePaymentAmount;

  @Column(name = "clm_othr_tp_pd_amt")
  private BigDecimal otherThirdPartyPayerPaidAmount;

  @Override
  public List<ExplanationOfBenefit.TotalComponent> toFhirTotal() {
    return List.of(
        AdjudicationChargeType.BENE_PAYMENT_AMOUNT.toFhirTotal(benePaymentAmount),
        AdjudicationChargeType.OTHER_THIRD_PARTY_PAYER_PAID_AMOUNT.toFhirTotal(
            otherThirdPartyPayerPaidAmount));
  }

  @Override
  public List<ExplanationOfBenefit.AdjudicationComponent> toFhirAdjudication() {
    return List.of();
  }
}
