package gov.cms.bfd.server.ng.claim.model.rx;

import gov.cms.bfd.server.ng.claim.model.common.AdjudicationChargeType;
import gov.cms.bfd.server.ng.claim.model.common.AdjudicationEmbedded;
import gov.cms.bfd.server.ng.converter.NonZeroBigDecimalConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@SuppressWarnings({"checkstyle:MissingJavadocMethod", "checkstyle:MissingJavadocType"})
@Embeddable
@Getter
public class ClaimLineAdjudicationRx implements AdjudicationEmbedded {
  @Column(name = "tot_rx_cst_amt")
  @Convert(converter = NonZeroBigDecimalConverter.class)
  private Optional<BigDecimal> totalRxCostAmount;

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

  @Override
  public List<ExplanationOfBenefit.AdjudicationComponent> toFhirAdjudication() {
    return Stream.of(
            AdjudicationChargeType.TOTAL_DRUG_COST_AMOUNT.toFhirAdjudicationOptional(
                totalRxCostAmount),
            AdjudicationChargeType.PATIENT_LIABILITY_REDUCT_AMOUNT.toFhirAdjudicationOptional(
                patientLiabReductPaidAmount),
            AdjudicationChargeType.LOW_INCOME_COST_SHARE_SUB_AMOUNT.toFhirAdjudicationOptional(
                lowIncomeCostShareSubAmount),
            AdjudicationChargeType.GROSS_DRUG_COST_BLW_THRESHOLD_AMOUNT.toFhirAdjudicationOptional(
                grossCostBelowThresholdAmount),
            AdjudicationChargeType.GROSS_DRUG_COST_ABOVE_THRESHOLD_AMOUNT
                .toFhirAdjudicationOptional(grossCostAboveThresholdAmount),
            AdjudicationChargeType.LINE_RX_REPORTED_GAP_DISCOUNT_AMOUNT.toFhirAdjudicationOptional(
                reportedGapDiscountAmount))
        .flatMap(Optional::stream)
        .toList();
  }
}
