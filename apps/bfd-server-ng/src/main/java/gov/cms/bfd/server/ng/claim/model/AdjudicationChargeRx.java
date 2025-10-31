package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@Embeddable
class AdjudicationChargeRx {
  @Column(name = "clm_rptd_mftr_dscnt_amt")
  private double gapDiscountAmount;

  @Column(name = "clm_line_vccn_admin_fee_amt")
  private double vaccineAdminFeeAmount;

  @Column(name = "clm_line_troop_tot_amt")
  private double otherTrueOutOfPockPaidAmount;

  @Column(name = "clm_line_srvc_cst_amt")
  private double dispensingFeeAmount;

  @Column(name = "clm_line_sls_tax_amt")
  private double salesTaxAmount;

  @Column(name = "clm_line_plro_amt")
  private double patientLiabReductPaidAmount;

  @Column(name = "clm_line_lis_amt")
  private double lowIncomeCostShareSubAmount;

  @Column(name = "clm_line_ingrdnt_cst_amt")
  private double ingredientCostAmount;

  @Column(name = "clm_line_grs_blw_thrshld_amt")
  private double grossCostBelowThresholdAmount;

  @Column(name = "clm_line_grs_above_thrshld_amt")
  private double grossCostAboveThresholdAmount;

  @Column(name = "clm_prcng_excptn_cd")
  private Optional<ClaimPricingReasonCode> pricingCode;

  List<ExplanationOfBenefit.AdjudicationComponent> toFhir() {
    ArrayList<ExplanationOfBenefit.AdjudicationComponent> adjudicationComponent =
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
                    grossCostAboveThresholdAmount)));
    toAdjudicationComponent().ifPresent(adjudicationComponent::add);

    return Collections.unmodifiableList(adjudicationComponent);
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
                        .setCode("adjustmentreason")
                        .setDisplay("Adjustment Reason")))
            .setReason(reasonCodeableConcept));
  }
}
