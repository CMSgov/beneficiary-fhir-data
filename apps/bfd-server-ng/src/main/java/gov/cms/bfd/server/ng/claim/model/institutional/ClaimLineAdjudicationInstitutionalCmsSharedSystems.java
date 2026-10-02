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
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** clm_line adjudication information for Institutional-CMS-SharedSystems. */
@Embeddable
@Getter
public class ClaimLineAdjudicationInstitutionalCmsSharedSystems implements AdjudicationEmbedded {

  @Embedded ClaimLineAdjudicationInstitutionalSharedSystems baseAdjudicationCharge;

  @Column(name = "clm_line_ncvrd_pd_amt") // CMS
  @Convert(converter = NonZeroBigDecimalConverter.class)
  private Optional<BigDecimal> noncoveredProductPaidAmount;

  @Column(name = "clm_line_prvdr_pmt_amt") // CMS
  private BigDecimal providerPaymentAmount;

  @Column(name = "clm_line_otaf_amt") // CMS
  @Convert(converter = NonZeroBigDecimalConverter.class)
  private Optional<BigDecimal> providerObligationToAcceptFullAmount;

  @Column(name = "clm_line_othr_tp_pd_amt") // CMS
  @Convert(converter = NonZeroBigDecimalConverter.class)
  private Optional<BigDecimal> otherThirdPartyPaidAmount;

  @Column(name = "clm_line_instnl_adjstd_amt") // CMS
  @Convert(converter = NonZeroBigDecimalConverter.class)
  private Optional<BigDecimal> adjustedAmount;

  @Column(name = "clm_line_instnl_rdcd_amt") // CMS
  @Convert(converter = NonZeroBigDecimalConverter.class)
  private Optional<BigDecimal> reducedAmount;

  @Column(name = "clm_line_instnl_msp1_pd_amt") // CMS
  private BigDecimal msp1PaidAmount;

  @Column(name = "clm_line_instnl_msp2_pd_amt") // CMS
  private BigDecimal msp2PaidAmount;

  @Column(name = "clm_line_instnl_rate_amt") // CMS
  @Convert(converter = NonZeroBigDecimalConverter.class)
  private Optional<BigDecimal> rateAmount;

  @Column(name = "clm_line_add_on_pymt_amt") // CMS
  @Convert(converter = NonZeroBigDecimalConverter.class)
  private Optional<BigDecimal> addOnPaymentAmount;

  @Column(name = "clm_line_non_ehr_rdctn_amt") // CMS
  @Convert(converter = NonZeroBigDecimalConverter.class)
  private Optional<BigDecimal> nonEHRReductionAmount;

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
                    AdjudicationChargeType.LINE_NONCOVERED_PRODUCT_PAID_AMOUNT
                        .toFhirAdjudicationOptional(noncoveredProductPaidAmount),
                    AdjudicationChargeType.LINE_INSTITUTIONAL_RATE_AMOUNT
                        .toFhirAdjudicationOptional(rateAmount),
                    AdjudicationChargeType.LINE_INSTITUTIONAL_ADD_ON_PAYMENT_AMOUNT
                        .toFhirAdjudicationOptional(addOnPaymentAmount),
                    AdjudicationChargeType.LINE_INSTITUTIONAL_NON_EHR_REDUCTION_AMOUNT
                        .toFhirAdjudicationOptional(nonEHRReductionAmount),
                    AdjudicationChargeType.LINE_PROVIDER_OBLIGATION_FULL_AMOUNT
                        .toFhirAdjudicationOptional(providerObligationToAcceptFullAmount),
                    AdjudicationChargeType.LINE_OTHER_THIRD_PARTY_PAID_AMOUNT
                        .toFhirAdjudicationOptional(otherThirdPartyPaidAmount),
                    AdjudicationChargeType.LINE_INSTITUTIONAL_ADJUSTED_AMOUNT
                        .toFhirAdjudicationOptional(adjustedAmount),
                    AdjudicationChargeType.LINE_INSTITUTIONAL_REDUCED_AMOUNT
                        .toFhirAdjudicationOptional(reducedAmount))
                .flatMap(Optional::stream))
        .flatMap(Function.identity())
        .toList();
  }
}
