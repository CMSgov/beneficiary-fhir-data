package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.List;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@Embeddable
class ClaimLineAdjudicationChargeInstitutional {
  @Column(name = "clm_line_instnl_adjstd_amt")
  private BigDecimal adjustedAmount;

  @Column(name = "clm_line_instnl_rdcd_amt")
  private BigDecimal reducedAmount;

  @Column(name = "clm_line_instnl_msp1_pd_amt")
  private BigDecimal msp1PaidAmount;

  @Column(name = "clm_line_instnl_msp2_pd_amt")
  private BigDecimal msp2PaidAmount;

  @Column(name = "clm_line_instnl_rate_amt")
  private BigDecimal rateAmount;

  @Column(name = "clm_line_add_on_pymt_amt")
  private BigDecimal addOnPaymentAmount;

  @Column(name = "clm_line_non_ehr_rdctn_amt")
  private BigDecimal nonEHRReductionAmount;

  @Column(name = "clm_rev_cntr_tdapa_amt")
  private BigDecimal transitionalDrugAddOnPaymentAmount;

  List<ExplanationOfBenefit.AdjudicationComponent> toFhir() {
    return List.of(
        AdjudicationChargeType.LINE_INSTITUTIONAL_ADJUSTED_AMOUNT.toFhirAdjudication(
            adjustedAmount),
        AdjudicationChargeType.LINE_INSTITUTIONAL_REDUCED_AMOUNT.toFhirAdjudication(reducedAmount),
        AdjudicationChargeType.LINE_INSTITUTIONAL_1ST_MSP_PAID_AMOUNT.toFhirAdjudication(
            msp1PaidAmount),
        AdjudicationChargeType.LINE_INSTITUTIONAL_2ND_PAID_AMOUNT.toFhirAdjudication(
            msp2PaidAmount),
        AdjudicationChargeType.LINE_INSTITUTIONAL_RATE_AMOUNT.toFhirAdjudication(rateAmount),
        AdjudicationChargeType.LINE_INSTITUTIONAL_ADD_ON_PAYMENT_AMOUNT.toFhirAdjudication(
            addOnPaymentAmount),
        AdjudicationChargeType.LINE_INSTITUTIONAL_NON_EHR_REDUCTION_AMOUNT.toFhirAdjudication(
            nonEHRReductionAmount),
        AdjudicationChargeType.LINE_INSTITUTIONAL_TRANSITIONAL_DRG_ADD_ON_PAYMENT_ADJUSTMENT
            .toFhirAdjudication(transitionalDrugAddOnPaymentAmount));
  }
}
