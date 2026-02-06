package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.List;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@Embeddable
class ClaimLineAdjudicationChargeInstitutionalSharedSystems {
  @Column(name = "clm_line_ncvrd_chrg_amt")
  private BigDecimal noncoveredChargeAmount;

  @Column(name = "clm_line_ncvrd_pd_amt")
  private BigDecimal noncoveredProductPaidAmount;

  @Column(name = "clm_line_alowd_chrg_amt")
  private BigDecimal allowedChargeAmount;

  @Column(name = "clm_line_sbmt_chrg_amt")
  private BigDecimal submittedChargeAmount;

  @Column(name = "clm_line_prvdr_pmt_amt")
  private BigDecimal providerPaymentAmount;

  @Column(name = "clm_line_bene_pmt_amt")
  private BigDecimal benePaymentAmount;

  @Column(name = "clm_line_bene_pd_amt")
  private BigDecimal benePaidAmount;

  @Column(name = "clm_line_cvrd_pd_amt")
  private BigDecimal coveredPaidAmount;

  @Column(name = "clm_line_mdcr_ddctbl_amt")
  private BigDecimal deductibleAmount;

  @Column(name = "clm_line_otaf_amt")
  private BigDecimal providerObligationToAcceptFullAmount;

  @Column(name = "clm_line_othr_tp_pd_amt")
  private BigDecimal otherThirdPartyPaidAmount;

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
        AdjudicationChargeType.LINE_ALLOWED_CHARGE_AMOUNT.toFhirAdjudication(allowedChargeAmount),
        AdjudicationChargeType.LINE_MEDICARE_DEDUCTIBLE_AMOUNT.toFhirAdjudication(deductibleAmount),
        AdjudicationChargeType.LINE_PROVIDER_OBLIGATION_FULL_AMOUNT.toFhirAdjudication(
            providerObligationToAcceptFullAmount),
        AdjudicationChargeType.LINE_BENE_PAID_AMOUNT.toFhirAdjudication(benePaidAmount),
        AdjudicationChargeType.LINE_BENE_PAYMENT_AMOUNT.toFhirAdjudication(benePaymentAmount),
        AdjudicationChargeType.LINE_NONCOVERED_CHARGE_AMOUNT.toFhirAdjudication(
            noncoveredChargeAmount),
        AdjudicationChargeType.LINE_PROVIDER_PAYMENT_AMOUNT.toFhirAdjudication(
            providerPaymentAmount),
        AdjudicationChargeType.LINE_COVERED_PAID_AMOUNT.toFhirAdjudication(coveredPaidAmount),
        AdjudicationChargeType.LINE_NONCOVERED_PRODUCT_PAID_AMOUNT.toFhirAdjudication(
            noncoveredProductPaidAmount),
        AdjudicationChargeType.LINE_OTHER_THIRD_PARTY_PAID_AMOUNT.toFhirAdjudication(
            otherThirdPartyPaidAmount),
        AdjudicationChargeType.LINE_SUBMITTED_CHARGE_AMOUNT.toFhirAdjudication(
            submittedChargeAmount),
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
