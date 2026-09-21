package gov.cms.bfd.server.ng.claim.model.rx;

import gov.cms.bfd.server.ng.claim.model.common.AdjudicationChargeType;
import gov.cms.bfd.server.ng.converter.NonZeroBigDecimalConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Transient;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@SuppressWarnings({"checkstyle:MissingJavadocMethod", "checkstyle:MissingJavadocType"})
@Embeddable
public class ClaimLineAdjudicationChargeRx {
  @Column(name = "clm_line_ingrdnt_cst_amt")
  private BigDecimal ingredientCostAmount;

  @Column(name = "clm_line_vccn_admin_fee_amt")
  private BigDecimal vaccineAdminFeeAmount;

  @Column(name = "clm_line_srvc_cst_amt")
  private BigDecimal dispensingFeeAmount;

  @Column(name = "clm_line_sls_tax_amt")
  private BigDecimal salesTaxAmount;

  @Column(name = "clm_line_plro_amt")
  @Convert(converter = NonZeroBigDecimalConverter.class)
  private Optional<BigDecimal> patientLiabReductPaidAmount;

  @Column(name = "clm_line_lis_amt")
  @Convert(converter = NonZeroBigDecimalConverter.class)
  private Optional<BigDecimal> lowIncomeCostShareSubAmount;

  @Column(name = "clm_line_grs_blw_thrshld_amt")
  @Convert(converter = NonZeroBigDecimalConverter.class)
  private Optional<BigDecimal> grossCostBelowThresholdAmount;

  @Column(name = "clm_line_grs_above_thrshld_amt")
  @Convert(converter = NonZeroBigDecimalConverter.class)
  private Optional<BigDecimal> grossCostAboveThresholdAmount;

  @Column(name = "clm_line_rptd_gap_dscnt_amt")
  @Convert(converter = NonZeroBigDecimalConverter.class)
  private Optional<BigDecimal> reportedGapDiscountAmount;

  @Transient
  public BigDecimal getTotalDrugCost() {
    return Stream.of(
            ingredientCostAmount, vaccineAdminFeeAmount, dispensingFeeAmount, salesTaxAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  public List<ExplanationOfBenefit.AdjudicationComponent> toFhir() {
    return Stream.concat(
            Stream.of(
                AdjudicationChargeType.TOTAL_DRUG_COST_AMOUNT.toFhirAdjudication(
                    getTotalDrugCost())),
            Stream.of(
                    AdjudicationChargeType.PATIENT_LIABILITY_REDUCT_AMOUNT.toFhirAdjudicationCMS(
                        patientLiabReductPaidAmount),
                    AdjudicationChargeType.LOW_INCOME_COST_SHARE_SUB_AMOUNT.toFhirAdjudicationCMS(
                        lowIncomeCostShareSubAmount),
                    AdjudicationChargeType.GROSS_DRUG_COST_BLW_THRESHOLD_AMOUNT
                        .toFhirAdjudicationCMS(grossCostBelowThresholdAmount),
                    AdjudicationChargeType.GROSS_DRUG_COST_ABOVE_THRESHOLD_AMOUNT
                        .toFhirAdjudicationCMS(grossCostAboveThresholdAmount),
                    AdjudicationChargeType.LINE_RX_REPORTED_GAP_DISCOUNT_AMOUNT
                        .toFhirAdjudicationCMS(reportedGapDiscountAmount))
                .flatMap(Optional::stream))
        .toList();
  }
}
