package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Transient;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@Embeddable
class ClaimLineAdjudicationChargeRx {
  @Column(name = "clm_line_ingrdnt_cst_amt")
  private BigDecimal ingredientCostAmount;

  @Column(name = "clm_line_vccn_admin_fee_amt")
  private BigDecimal vaccineAdminFeeAmount;

  @Column(name = "clm_line_srvc_cst_amt")
  private BigDecimal dispensingFeeAmount;

  @Column(name = "clm_line_sls_tax_amt")
  private BigDecimal salesTaxAmount;

  @Column(name = "clm_line_plro_amt")
  private BigDecimal patientLiabReductPaidAmount;

  @Column(name = "clm_line_lis_amt")
  private BigDecimal lowIncomeCostShareSubAmount;

  @Column(name = "clm_line_grs_blw_thrshld_amt")
  private BigDecimal grossCostBelowThresholdAmount;

  @Column(name = "clm_line_grs_above_thrshld_amt")
  private BigDecimal grossCostAboveThresholdAmount;

  @Column(name = "clm_prcng_excptn_cd")
  private Optional<ClaimPricingReasonCode> pricingCode;

  @Column(name = "clm_line_rptd_gap_dscnt_amt")
  private BigDecimal reportedGapDiscountAmount;

  @Transient
  public BigDecimal getTotalDrugCost() {
    return Stream.of(
            ingredientCostAmount, vaccineAdminFeeAmount, dispensingFeeAmount, salesTaxAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  ArrayList<ExplanationOfBenefit.AdjudicationComponent> toFhir() {
    var adjudicationComponent =
        new ArrayList<>(
            List.of(
                AdjudicationChargeType.PATIENT_LIABILITY_REDUCT_AMOUNT.toFhirAdjudication(
                    patientLiabReductPaidAmount),
                AdjudicationChargeType.LOW_INCOME_COST_SHARE_SUB_AMOUNT.toFhirAdjudication(
                    lowIncomeCostShareSubAmount),
                AdjudicationChargeType.GROSS_DRUG_COST_BLW_THRESHOLD_AMOUNT.toFhirAdjudication(
                    grossCostBelowThresholdAmount),
                AdjudicationChargeType.GROSS_DRUG_COST_ABOVE_THRESHOLD_AMOUNT.toFhirAdjudication(
                    grossCostAboveThresholdAmount),
                AdjudicationChargeType.LINE_RX_REPORTED_GAP_DISCOUNT_AMOUNT.toFhirAdjudication(
                    reportedGapDiscountAmount),
                AdjudicationChargeType.TOTAL_DRUG_COST_AMOUNT.toFhirAdjudication(
                    getTotalDrugCost())));
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
