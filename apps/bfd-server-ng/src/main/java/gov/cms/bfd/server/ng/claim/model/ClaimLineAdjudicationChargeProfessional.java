package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.List;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@Embeddable
@Getter
class ClaimLineAdjudicationChargeProfessional {

  @Column(name = "clm_line_carr_clncl_chrg_amt")
  private double carrierClinicalChargeAmount;

  @Column(name = "clm_line_carr_psych_ot_lmt_amt")
  private double therapyAmountAppliedToLimit;

  @Column(name = "clm_line_prfnl_intrst_amt")
  private double professionalInterestAmount;

  @Column(name = "clm_mdcr_prmry_pyr_alowd_amt")
  private double primaryPayerAllowedAmount;

  @Column(name = "clm_bene_prmry_pyr_pd_amt")
  private double primaryPayerPaidAmount;

  @Column(name = "clm_line_dmerc_scrn_svgs_amt")
  private double screenSavingsAmount;

  @Column(name = "clm_line_prfnl_dme_price_amt")
  private double purchasePriceAmount;

  List<ExplanationOfBenefit.AdjudicationComponent> toFhir() {
    return List.of(
        AdjudicationChargeType.LINE_PROFESSIONAL_CARRIER_CLINICAL_CHARGE_AMOUNT.toFhirAdjudication(
            carrierClinicalChargeAmount),
        AdjudicationChargeType.LINE_PROFESSIONAL_THERAPY_LMT_AMOUNT.toFhirAdjudication(
            therapyAmountAppliedToLimit),
        AdjudicationChargeType.LINE_PROFESSIONAL_INTEREST_AMOUNT.toFhirAdjudication(
            professionalInterestAmount),
        AdjudicationChargeType.LINE_PROFESSIONAL_PRIMARY_PAYER_ALLOWED_AMOUNT.toFhirAdjudication(
            primaryPayerAllowedAmount),
        AdjudicationChargeType.LINE_PROFESSIONAL_PRIMARY_PAYER_PAID_AMOUNT.toFhirAdjudication(
            primaryPayerPaidAmount),
        AdjudicationChargeType.LINE_PROFESSIONAL_SCREEN_SAVINGS_AMOUNT.toFhirAdjudication(
            screenSavingsAmount),
        AdjudicationChargeType.LINE_PROFESSIONAL_PURCHASE_PRICE_AMOUNT.toFhirAdjudication(
            primaryPayerAllowedAmount));
  }
}
