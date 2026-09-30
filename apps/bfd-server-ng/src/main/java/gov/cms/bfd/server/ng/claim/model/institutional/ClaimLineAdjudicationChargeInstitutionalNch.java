package gov.cms.bfd.server.ng.claim.model.institutional;

import gov.cms.bfd.server.ng.claim.model.common.AdjudicationChargeType;
import gov.cms.bfd.server.ng.converter.NonZeroBigDecimalConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@Embeddable
class ClaimLineAdjudicationChargeInstitutionalNch {
  @Column(name = "clm_line_ncvrd_chrg_amt")
  private BigDecimal noncoveredChargeAmount;

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

  @Column(name = "clm_line_blood_ddctbl_amt")
  @Convert(converter = NonZeroBigDecimalConverter.class)
  private Optional<BigDecimal> bloodDeductibleAmount;

  @Column(name = "clm_line_instnl_adjstd_amt")
  @Convert(converter = NonZeroBigDecimalConverter.class)
  private Optional<BigDecimal> adjustedAmount;

  @Column(name = "clm_line_instnl_rdcd_amt")
  @Convert(converter = NonZeroBigDecimalConverter.class)
  private Optional<BigDecimal> reducedAmount;

  @Column(name = "clm_line_instnl_msp1_pd_amt")
  private BigDecimal msp1PaidAmount;

  @Column(name = "clm_line_instnl_msp2_pd_amt")
  private BigDecimal msp2PaidAmount;

  @Column(name = "clm_line_instnl_rate_amt")
  @Convert(converter = NonZeroBigDecimalConverter.class)
  private Optional<BigDecimal> rateAmount;

  @Column(name = "clm_line_add_on_pymt_amt")
  @Convert(converter = NonZeroBigDecimalConverter.class)
  private Optional<BigDecimal> addOnPaymentAmount;

  @Column(name = "clm_rev_cntr_tdapa_amt")
  @Convert(converter = NonZeroBigDecimalConverter.class)
  private Optional<BigDecimal> transitionalDrugAddOnPaymentAmount;

  List<ExplanationOfBenefit.AdjudicationComponent> toFhir() {
    return Stream.concat(
            Stream.of(
                AdjudicationChargeType.LINE_MEDICARE_DEDUCTIBLE_AMOUNT.toFhirAdjudication(
                    deductibleAmount),
                AdjudicationChargeType.LINE_BENE_PAID_AMOUNT.toFhirAdjudication(benePaidAmount),
                AdjudicationChargeType.LINE_BENE_PAYMENT_AMOUNT.toFhirAdjudication(
                    benePaymentAmount),
                AdjudicationChargeType.LINE_NONCOVERED_CHARGE_AMOUNT.toFhirAdjudication(
                    noncoveredChargeAmount),
                AdjudicationChargeType.LINE_PROVIDER_PAYMENT_AMOUNT.toFhirAdjudication(
                    providerPaymentAmount),
                AdjudicationChargeType.LINE_COVERED_PAID_AMOUNT.toFhirAdjudication(
                    coveredPaidAmount),
                AdjudicationChargeType.LINE_SUBMITTED_CHARGE_AMOUNT.toFhirAdjudication(
                    submittedChargeAmount),
                AdjudicationChargeType.LINE_INSTITUTIONAL_1ST_MSP_PAID_AMOUNT.toFhirAdjudication(
                    msp1PaidAmount),
                AdjudicationChargeType.LINE_INSTITUTIONAL_2ND_PAID_AMOUNT.toFhirAdjudication(
                    msp2PaidAmount)),
            Stream.of(
                    AdjudicationChargeType.LINE_BLOOD_DEDUCTIBLE_AMOUNT.toFhirAdjudicationOptional(
                        bloodDeductibleAmount),
                    AdjudicationChargeType.LINE_INSTITUTIONAL_ADJUSTED_AMOUNT
                        .toFhirAdjudicationOptional(adjustedAmount),
                    AdjudicationChargeType.LINE_INSTITUTIONAL_REDUCED_AMOUNT
                        .toFhirAdjudicationOptional(reducedAmount),
                    AdjudicationChargeType.LINE_INSTITUTIONAL_RATE_AMOUNT
                        .toFhirAdjudicationOptional(rateAmount),
                    AdjudicationChargeType.LINE_INSTITUTIONAL_ADD_ON_PAYMENT_AMOUNT
                        .toFhirAdjudicationOptional(addOnPaymentAmount),
                    AdjudicationChargeType
                        .LINE_INSTITUTIONAL_TRANSITIONAL_DRG_ADD_ON_PAYMENT_ADJUSTMENT
                        .toFhirAdjudicationOptional(transitionalDrugAddOnPaymentAmount))
                .flatMap(Optional::stream))
        .toList();
  }
}
