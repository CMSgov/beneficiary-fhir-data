package gov.cms.bfd.server.ng.claim.model.institutional;

import gov.cms.bfd.server.ng.claim.model.common.AdjudicationChargeType;
import gov.cms.bfd.server.ng.claim.model.common.AdjudicationEmbedded;
import gov.cms.bfd.server.ng.converter.NonZeroBigDecimalConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** clm_line adjudication information for Institutional-CMS-NCH. */
@Embeddable
public class ClaimLineAdjudicationInstitutionalCmsNch implements AdjudicationEmbedded {

  @Embedded ClaimLineAdjudicationInstitutionalNch baseAdjudicationCharge;

  @Column(name = "clm_line_prvdr_pmt_amt")
  private BigDecimal providerPaymentAmount;

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

  /**
   * Compiles a list of AdjudicationComponents from columns.
   *
   * @return list of adjudication components
   */
  @Override
  public List<ExplanationOfBenefit.AdjudicationComponent> toFhirAdjudication() {
    return Stream.of(
            baseAdjudicationCharge.toFhirAdjudication().stream(),
            Stream.of(
                AdjudicationChargeType.LINE_PROVIDER_PAYMENT_AMOUNT.toFhirAdjudication(
                    providerPaymentAmount),
                AdjudicationChargeType.LINE_INSTITUTIONAL_1ST_MSP_PAID_AMOUNT.toFhirAdjudication(
                    msp1PaidAmount),
                AdjudicationChargeType.LINE_INSTITUTIONAL_2ND_PAID_AMOUNT.toFhirAdjudication(
                    msp2PaidAmount)),
            Stream.of(
                    AdjudicationChargeType.LINE_INSTITUTIONAL_RATE_AMOUNT
                        .toFhirAdjudicationOptional(rateAmount),
                    AdjudicationChargeType.LINE_INSTITUTIONAL_ADD_ON_PAYMENT_AMOUNT
                        .toFhirAdjudicationOptional(addOnPaymentAmount),
                    AdjudicationChargeType
                        .LINE_INSTITUTIONAL_TRANSITIONAL_DRG_ADD_ON_PAYMENT_ADJUSTMENT
                        .toFhirAdjudicationOptional(transitionalDrugAddOnPaymentAmount),
                    AdjudicationChargeType.LINE_INSTITUTIONAL_ADJUSTED_AMOUNT
                        .toFhirAdjudicationOptional(adjustedAmount),
                    AdjudicationChargeType.LINE_INSTITUTIONAL_REDUCED_AMOUNT
                        .toFhirAdjudicationOptional(reducedAmount),
                    AdjudicationChargeType.LINE_BLOOD_DEDUCTIBLE_AMOUNT.toFhirAdjudicationOptional(
                        bloodDeductibleAmount))
                .flatMap(Optional::stream))
        .flatMap(Function.identity())
        .toList();
  }
}
