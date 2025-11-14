package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.List;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@Embeddable
class AdjudicationChargeInstitutional {
  @Column(name = "clm_line_instnl_adjstd_amt")
  private double adjustedAmount;

  @Column(name = "clm_line_instnl_rdcd_amt")
  private double reducedAmount;

  @Column(name = "clm_line_instnl_msp1_pd_amt")
  private double msp1PaidAmount;

  @Column(name = "clm_line_instnl_msp2_pd_amt")
  private double msp2PaidAmount;

  @Column(name = "clm_line_instnl_rate_amt")
  private double rateAmount;

  List<ExplanationOfBenefit.AdjudicationComponent> toFhir() {
    return List.of(
        AdjudicationChargeType.LINE_INSTITUTIONAL_ADJUSTED_AMOUNT.toFhirAdjudication(
            adjustedAmount),
        AdjudicationChargeType.LINE_INSTITUTIONAL_REDUCED_AMOUNT.toFhirAdjudication(reducedAmount),
        AdjudicationChargeType.LINE_INSTITUTIONAL_1ST_MSP_PAID_AMOUNT.toFhirAdjudication(
            msp1PaidAmount),
        AdjudicationChargeType.LINE_INSTITUTIONAL_2ND_PAID_AMOUNT.toFhirAdjudication(
            msp2PaidAmount),
        AdjudicationChargeType.LINE_INSTITUTIONAL_RATE_AMOUNT.toFhirAdjudication(rateAmount));
  }
}
