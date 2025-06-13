package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

import java.util.List;

@Embeddable
public class AdjudicationChargeInstitutional {
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
        AdjudicationChargeType.INSTITUTIONAL_ADJUSTED_AMOUNT.toFhir(adjustedAmount),
        AdjudicationChargeType.INSTITUTIONAL_REDUCED_AMOUNT.toFhir(reducedAmount),
        AdjudicationChargeType.INSTITUTIONAL_1ST_MSP_PAID_AMOUNT.toFhir(msp1PaidAmount),
        AdjudicationChargeType.INSTITUTIONAL_2ND_PAID_AMOUNT.toFhir(msp2PaidAmount),
        AdjudicationChargeType.INSTITUTIONAL_RATE_AMOUNT.toFhir(rateAmount));
  }
}
