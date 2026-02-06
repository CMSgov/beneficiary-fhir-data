package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@Embeddable
class ClaimLineAdjudicationChargeRxInfo {
  @Column(name = "clm_rptd_mftr_dscnt_amt")
  private BigDecimal gapDiscountAmount;

  @Column(name = "clm_line_vccn_admin_fee_amt")
  private BigDecimal vaccineAdminFeeAmount;

  @Column(name = "clm_line_troop_tot_amt")
  private BigDecimal otherTrueOutOfPockPaidAmount;

  @Column(name = "clm_line_srvc_cst_amt")
  private BigDecimal dispensingFeeAmount;

  @Column(name = "clm_line_sls_tax_amt")
  private BigDecimal salesTaxAmount;

  @Column(name = "clm_line_plro_amt")
  private BigDecimal patientLiabReductPaidAmount;

  @Column(name = "clm_line_lis_amt")
  private BigDecimal lowIncomeCostShareSubAmount;

  @Column(name = "clm_line_ingrdnt_cst_amt")
  private BigDecimal ingredientCostAmount;

  @Column(name = "clm_line_grs_blw_thrshld_amt")
  private BigDecimal grossCostBelowThresholdAmount;

  @Column(name = "clm_line_grs_above_thrshld_amt")
  private BigDecimal grossCostAboveThresholdAmount;

  @Column(name = "clm_prcng_excptn_cd")
  private Optional<ClaimPricingReasonCode> pricingCode;

  @Column(name = "clm_line_grs_cvrd_cst_tot_amt")
  private BigDecimal grossCoveredCostAmount;

  @Column(name = "clm_cms_calcd_mftr_dscnt_amt")
  private BigDecimal manufacturerDiscountAmount;

  @Column(name = "clm_line_rebt_passthru_pos_amt")
  private BigDecimal rebatePassthroughPOSAmount;

  @Column(name = "clm_phrmcy_price_dscnt_at_pos_amt")
  private BigDecimal priceAmount;

  @Column(name = "clm_line_ncvrd_pd_amt")
  private BigDecimal noncoveredProductPaidAmount;

  @Column(name = "clm_line_bene_pmt_amt")
  private BigDecimal benePaymentAmount;

  @Column(name = "clm_line_cvrd_pd_amt")
  private BigDecimal coveredPaidAmount;

  @Column(name = "clm_line_othr_tp_pd_amt")
  private BigDecimal otherThirdPartyPaidAmount;

  ArrayList<ExplanationOfBenefit.AdjudicationComponent> toFhir() {
    var adjudicationComponent =
        new ArrayList<>(
            List.of(
                AdjudicationChargeType.GAP_DISCOUNT_AMOUNT.toFhirAdjudication(gapDiscountAmount),
                AdjudicationChargeType.VACCINATION_ADMIN_FEE.toFhirAdjudication(
                    vaccineAdminFeeAmount),
                AdjudicationChargeType.OTHER_AMOUNT.toFhirAdjudication(
                    otherTrueOutOfPockPaidAmount),
                AdjudicationChargeType.DISPENSING_FEE.toFhirAdjudication(dispensingFeeAmount),
                AdjudicationChargeType.SALES_TAX_AMOUNT.toFhirAdjudication(salesTaxAmount),
                AdjudicationChargeType.PATIENT_LIABILITY_REDUCT_AMOUNT.toFhirAdjudication(
                    patientLiabReductPaidAmount),
                AdjudicationChargeType.LOW_INCOME_COST_SHARE_SUB_AMOUNT.toFhirAdjudication(
                    lowIncomeCostShareSubAmount),
                AdjudicationChargeType.INGREDIENT_COST_AMOUNT.toFhirAdjudication(
                    ingredientCostAmount),
                AdjudicationChargeType.GROSS_DRUG_COST_BLW_THRESHOLD_AMOUNT.toFhirAdjudication(
                    grossCostBelowThresholdAmount),
                AdjudicationChargeType.GROSS_DRUG_COST_ABOVE_THRESHOLD_AMOUNT.toFhirAdjudication(
                    grossCostAboveThresholdAmount),
                AdjudicationChargeType.LINE_RX_GROSS_COVERED_COST_AMOUNT.toFhirAdjudication(
                    grossCoveredCostAmount),
                AdjudicationChargeType.LINE_RX_MANUFACTURER_DISCOUNT_AMOUNT.toFhirAdjudication(
                    manufacturerDiscountAmount),
                AdjudicationChargeType.LINE_RX_REBATE_PASSTHROUGH_POS_AMOUNT.toFhirAdjudication(
                    rebatePassthroughPOSAmount),
                AdjudicationChargeType.LINE_RX_PRICE_AMOUNT.toFhirAdjudication(priceAmount),
                AdjudicationChargeType.LINE_BENE_PAYMENT_AMOUNT.toFhirAdjudication(
                    benePaymentAmount),
                AdjudicationChargeType.LINE_COVERED_PAID_AMOUNT.toFhirAdjudication(
                    coveredPaidAmount),
                AdjudicationChargeType.LINE_NONCOVERED_PRODUCT_PAID_AMOUNT.toFhirAdjudication(
                    noncoveredProductPaidAmount),
                AdjudicationChargeType.LINE_OTHER_THIRD_PARTY_PAID_AMOUNT.toFhirAdjudication(
                    otherThirdPartyPaidAmount)));
    toAdjudicationComponent().ifPresent(adjudicationComponent::add);

    return adjudicationComponent;
  }

  private Optional<ExplanationOfBenefit.AdjudicationComponent> toAdjudicationComponent() {
    var reasonCode = pricingCode.map(ClaimPricingReasonCode::toFhir);
    var reasonCodeableConcept = new CodeableConcept();
    reasonCode.ifPresent(reasonCodeableConcept::addCoding);

    return Optional.of(
        new ExplanationOfBenefit.AdjudicationComponent()
            .setCategory(
                new CodeableConcept(
                    new Coding()
                        .setSystem(SystemUrls.CARIN_CODE_SYSTEM_ADJUDICATION_DISCRIMINATOR)
                        .setCode("benefitpaymentstatus")
                        .setDisplay("Benefit Payment Status")))
            .setReason(reasonCodeableConcept));
  }
}
