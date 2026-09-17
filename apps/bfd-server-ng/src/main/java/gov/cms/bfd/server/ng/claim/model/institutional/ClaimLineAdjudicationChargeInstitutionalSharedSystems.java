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
class ClaimLineAdjudicationChargeInstitutionalSharedSystems {
  @Column(name = "clm_line_ncvrd_chrg_amt")
  private BigDecimal noncoveredChargeAmount;

  @Column(name = "clm_line_ncvrd_pd_amt")
  @Convert(converter = NonZeroBigDecimalConverter.class)
  private Optional<BigDecimal> noncoveredProductPaidAmount;

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
  @Convert(converter = NonZeroBigDecimalConverter.class)
  private Optional<BigDecimal> providerObligationToAcceptFullAmount;

  @Column(name = "clm_line_othr_tp_pd_amt")
  @Convert(converter = NonZeroBigDecimalConverter.class)
  private Optional<BigDecimal> otherThirdPartyPaidAmount;

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

  @Column(name = "clm_line_non_ehr_rdctn_amt")
  @Convert(converter = NonZeroBigDecimalConverter.class)
  private Optional<BigDecimal> nonEHRReductionAmount;

  List<ExplanationOfBenefit.AdjudicationComponent> toFhir() {
    return Stream.concat(
            Stream.of(
                AdjudicationChargeType.LINE_ALLOWED_CHARGE_AMOUNT.toFhirAdjudicationNonOptional(
                    allowedChargeAmount),
                AdjudicationChargeType.LINE_MEDICARE_DEDUCTIBLE_AMOUNT
                    .toFhirAdjudicationNonOptional(deductibleAmount),
                AdjudicationChargeType.LINE_BENE_PAID_AMOUNT.toFhirAdjudicationNonOptional(
                    benePaidAmount),
                AdjudicationChargeType.LINE_BENE_PAYMENT_AMOUNT.toFhirAdjudicationNonOptional(
                    benePaymentAmount),
                AdjudicationChargeType.LINE_NONCOVERED_CHARGE_AMOUNT.toFhirAdjudicationNonOptional(
                    noncoveredChargeAmount),
                AdjudicationChargeType.LINE_PROVIDER_PAYMENT_AMOUNT.toFhirAdjudicationNonOptional(
                    providerPaymentAmount),
                AdjudicationChargeType.LINE_COVERED_PAID_AMOUNT.toFhirAdjudicationNonOptional(
                    coveredPaidAmount),
                AdjudicationChargeType.LINE_SUBMITTED_CHARGE_AMOUNT.toFhirAdjudicationNonOptional(
                    submittedChargeAmount),
                AdjudicationChargeType.LINE_INSTITUTIONAL_1ST_MSP_PAID_AMOUNT
                    .toFhirAdjudicationNonOptional(msp1PaidAmount),
                AdjudicationChargeType.LINE_INSTITUTIONAL_2ND_PAID_AMOUNT
                    .toFhirAdjudicationNonOptional(msp2PaidAmount)),
            Stream.of(
                    AdjudicationChargeType.LINE_PROVIDER_OBLIGATION_FULL_AMOUNT.toFhirAdjudication(
                        providerObligationToAcceptFullAmount),
                    AdjudicationChargeType.LINE_NONCOVERED_PRODUCT_PAID_AMOUNT.toFhirAdjudication(
                        noncoveredProductPaidAmount),
                    AdjudicationChargeType.LINE_OTHER_THIRD_PARTY_PAID_AMOUNT.toFhirAdjudication(
                        otherThirdPartyPaidAmount),
                    AdjudicationChargeType.LINE_INSTITUTIONAL_ADJUSTED_AMOUNT.toFhirAdjudication(
                        adjustedAmount),
                    AdjudicationChargeType.LINE_INSTITUTIONAL_REDUCED_AMOUNT.toFhirAdjudication(
                        reducedAmount),
                    AdjudicationChargeType.LINE_INSTITUTIONAL_RATE_AMOUNT.toFhirAdjudication(
                        rateAmount),
                    AdjudicationChargeType.LINE_INSTITUTIONAL_ADD_ON_PAYMENT_AMOUNT
                        .toFhirAdjudication(addOnPaymentAmount),
                    AdjudicationChargeType.LINE_INSTITUTIONAL_NON_EHR_REDUCTION_AMOUNT
                        .toFhirAdjudication(nonEHRReductionAmount))
                .flatMap(Optional::stream))
        .toList();
  }
}
