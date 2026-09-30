package gov.cms.bfd.server.ng.claim.model.professional;

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
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** eob level adjudication information for Professional-CMS-SharedSystems. */
@Embeddable
public class AdjudicationProfessionalCmsSharedSystems implements AdjudicationEmbedded {

  @Embedded private AdjudicationProfessionalSharedSystems baseAdjudication;

  @Column(name = "clm_bene_intrst_pd_amt") // CMS
  @Convert(converter = NonZeroBigDecimalConverter.class)
  private Optional<BigDecimal> beneInterestPaidAmount;

  @Column(name = "clm_blood_lblty_amt")
  @Convert(converter = NonZeroBigDecimalConverter.class)
  private Optional<BigDecimal> bloodLiabilityAmount;

  @Column(name = "clm_cob_ptnt_resp_amt") // CMS
  @Convert(converter = NonZeroBigDecimalConverter.class)
  private Optional<BigDecimal> cobPatientResponsibilityAmount;

  @Column(name = "clm_prvdr_otaf_amt") // CMS
  @Convert(converter = NonZeroBigDecimalConverter.class)
  private Optional<BigDecimal> providerObligationToAcceptAmount;

  @Column(name = "clm_prvdr_rmng_due_amt") // CMS
  @Convert(converter = NonZeroBigDecimalConverter.class)
  private Optional<BigDecimal> remainingAmountToProvider;

  @Column(name = "clm_blood_ncvrd_chrg_amt") // CMS
  @Convert(converter = NonZeroBigDecimalConverter.class)
  private Optional<BigDecimal> bloodNoncoveredChargeAmount;

  @Column(name = "clm_prvdr_intrst_pd_amt") // CMS
  @Convert(converter = NonZeroBigDecimalConverter.class)
  private Optional<BigDecimal> providerInterestPaidAmount;

  @Override
  public List<ExplanationOfBenefit.TotalComponent> toFhirTotal() {
    return baseAdjudication.toFhirTotal();
  }

  @Override
  public List<ExplanationOfBenefit.AdjudicationComponent> toFhirAdjudication() {
    return Stream.of(
            AdjudicationChargeType.BENE_BLOOD_DEDUCTIBLE_LIABILITY_AMOUNT
                .toFhirAdjudicationOptional(bloodLiabilityAmount),
            AdjudicationChargeType.BLOOD_NONCOVERED_CHARGE_AMOUNT.toFhirAdjudicationOptional(
                bloodNoncoveredChargeAmount),
            AdjudicationChargeType.COB_PATIENT_RESPONSIBILITY_AMOUNT.toFhirAdjudicationOptional(
                cobPatientResponsibilityAmount),
            AdjudicationChargeType.PROVIDER_INTEREST_PAID_AMOUNT.toFhirAdjudicationOptional(
                providerInterestPaidAmount),
            AdjudicationChargeType.PROVIDER_OBLIGATION_TO_ACCEPT_AMOUNT.toFhirAdjudicationOptional(
                providerObligationToAcceptAmount),
            AdjudicationChargeType.REMAINING_AMOUNT_TO_PROVIDER.toFhirAdjudicationOptional(
                remainingAmountToProvider),
            AdjudicationChargeType.BENE_INTEREST_PAID_AMOUNT.toFhirAdjudicationOptional(
                beneInterestPaidAmount))
        .flatMap(Optional::stream)
        .toList();
  }
}
