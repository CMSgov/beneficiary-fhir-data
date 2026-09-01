package gov.cms.bfd.server.ng.claim.model.institutional;

import gov.cms.bfd.server.ng.claim.model.common.AdjudicationChargeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.List;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@Embeddable
class ClaimLineAdjudicationChargeInstitutionalSharedSystems {
  @Column(name = "clm_line_ncvrd_chrg_amt") // REGULAR CMS
  private BigDecimal noncoveredChargeAmount;

  @Column(name = "clm_line_ncvrd_pd_amt")
  private BigDecimal noncoveredProductPaidAmount;

  @Column(name = "clm_line_alowd_chrg_amt") // REGULAR CMS
  private BigDecimal allowedChargeAmount;

  @Column(name = "clm_line_sbmt_chrg_amt") // REGULAR CMS
  private BigDecimal submittedChargeAmount;

  @Column(name = "clm_line_prvdr_pmt_amt") // CMS
  private BigDecimal providerPaymentAmount;

  @Column(name = "clm_line_bene_pmt_amt") // REGULAR CMS
  private BigDecimal benePaymentAmount;

  @Column(name = "clm_line_bene_pd_amt") // REGULAR CMS
  private BigDecimal benePaidAmount;

  @Column(name = "clm_line_cvrd_pd_amt") // REGULAR CMS
  private BigDecimal coveredPaidAmount;

  @Column(name = "clm_line_mdcr_ddctbl_amt") // REGULAR CMS
  private BigDecimal deductibleAmount;

  @Column(name = "clm_line_otaf_amt") // CMS
  private BigDecimal providerObligationToAcceptFullAmount;

  @Column(name = "clm_line_othr_tp_pd_amt") // CMS
  private BigDecimal otherThirdPartyPaidAmount;

  @Column(name = "clm_line_instnl_adjstd_amt") // CMS
  private BigDecimal adjustedAmount;

  @Column(name = "clm_line_instnl_rdcd_amt") // CMS
  private BigDecimal reducedAmount;

  @Column(name = "clm_line_instnl_msp1_pd_amt") // CMS
  private BigDecimal msp1PaidAmount;

  @Column(name = "clm_line_instnl_msp2_pd_amt") // CMS
  private BigDecimal msp2PaidAmount;

  @Column(name = "clm_line_instnl_rate_amt") // CMS
  private BigDecimal rateAmount;

  @Column(name = "clm_line_add_on_pymt_amt") // CMS
  private BigDecimal addOnPaymentAmount;

  @Column(name = "clm_line_non_ehr_rdctn_amt") // CMS
  private BigDecimal nonEHRReductionAmount;

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
            nonEHRReductionAmount));
  }
}
