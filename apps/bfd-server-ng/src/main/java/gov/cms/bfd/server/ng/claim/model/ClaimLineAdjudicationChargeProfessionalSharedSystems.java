package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.List;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@Embeddable
class ClaimLineAdjudicationChargeProfessionalSharedSystems {

  @Column(name = "clm_line_alowd_chrg_amt")
  private BigDecimal allowedChargeAmount;

  @Column(name = "clm_line_sbmt_chrg_amt")
  private BigDecimal submittedChargeAmount;

  @Column(name = "clm_line_prvdr_pmt_amt")
  private BigDecimal providerPaymentAmount;

  @Column(name = "clm_line_bene_pd_amt")
  private BigDecimal benePaidAmount;

  @Column(name = "clm_line_cvrd_pd_amt")
  private BigDecimal coveredPaidAmount;

  @Column(name = "clm_line_mdcr_ddctbl_amt")
  private BigDecimal deductibleAmount;

  @Column(name = "clm_line_otaf_amt")
  private BigDecimal providerObligationToAcceptFullAmount;

  @Column(name = "clm_line_carr_psych_ot_lmt_amt")
  private BigDecimal therapyAmountAppliedToLimit;

  @Column(name = "clm_line_prfnl_dme_price_amt")
  private BigDecimal purchasePriceAmount;

  List<ExplanationOfBenefit.AdjudicationComponent> toFhir() {
    return List.of(
        AdjudicationChargeType.LINE_ALLOWED_CHARGE_AMOUNT.toFhirAdjudication(allowedChargeAmount),
        AdjudicationChargeType.LINE_MEDICARE_DEDUCTIBLE_AMOUNT.toFhirAdjudication(deductibleAmount),
        AdjudicationChargeType.LINE_PROVIDER_OBLIGATION_FULL_AMOUNT.toFhirAdjudication(
            providerObligationToAcceptFullAmount),
        AdjudicationChargeType.LINE_BENE_PAID_AMOUNT.toFhirAdjudication(benePaidAmount),
        AdjudicationChargeType.LINE_PROVIDER_PAYMENT_AMOUNT.toFhirAdjudication(
            providerPaymentAmount),
        AdjudicationChargeType.LINE_COVERED_PAID_AMOUNT.toFhirAdjudication(coveredPaidAmount),
        AdjudicationChargeType.LINE_PROFESSIONAL_PURCHASE_PRICE_AMOUNT.toFhirAdjudication(
            purchasePriceAmount),
        AdjudicationChargeType.LINE_PROFESSIONAL_THERAPY_LMT_AMOUNT.toFhirAdjudication(
            therapyAmountAppliedToLimit),
        AdjudicationChargeType.LINE_SUBMITTED_CHARGE_AMOUNT.toFhirAdjudication(
            submittedChargeAmount));
  }
}
