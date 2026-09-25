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
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/**
 * Adjudication fields for institutional claim, cms profile, nch system. Note: this leverages the
 * fact that CMS is a superset of REGULAR.
 */
@Embeddable
public class AdjudicationInstitutionalCmsNch implements AdjudicationEmbedded {

  @Embedded AdjudicationInstitutionalRegular adjudicationChargeBase;

  @Column(name = "clm_blood_chrg_amt")
  @Convert(converter = NonZeroBigDecimalConverter.class)
  private Optional<BigDecimal> bloodChargeAmount;

  @Column(name = "clm_blood_lblty_amt")
  @Convert(converter = NonZeroBigDecimalConverter.class)
  private Optional<BigDecimal> bloodLiabilityAmount;

  @Column(name = "clm_blood_ncvrd_chrg_amt")
  @Convert(converter = NonZeroBigDecimalConverter.class)
  private Optional<BigDecimal> bloodNoncoveredChargeAmount;

  @Override
  public List<ExplanationOfBenefit.TotalComponent> toFhirTotal() {
    return adjudicationChargeBase.toFhirTotal();
  }

  @Override
  public List<ExplanationOfBenefit.AdjudicationComponent> toFhirAdjudication() {
    return Stream.concat(
            adjudicationChargeBase.toFhirAdjudication().stream(),
            Stream.of(
                    AdjudicationChargeType.BLOOD_CHARGE_AMOUNT.toFhirAdjudicationOptional(
                        bloodChargeAmount),
                    AdjudicationChargeType.BENE_BLOOD_DEDUCTIBLE_LIABILITY_AMOUNT
                        .toFhirAdjudicationOptional(bloodLiabilityAmount),
                    AdjudicationChargeType.BLOOD_NONCOVERED_CHARGE_AMOUNT
                        .toFhirAdjudicationOptional(bloodNoncoveredChargeAmount))
                .flatMap(Optional::stream))
        .toList();
  }
}
