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

/** clm_line adjudication information for Professional-CMS-SharedSystems. */
@Embeddable
public class ClaimLineAdjudicationProfessionalCmsSharedSystems implements AdjudicationEmbedded {

  @Embedded private ClaimLineAdjudicationProfessionalCms cmsCharges;

  @Column(name = "clm_line_otaf_amt")
  @Convert(converter = NonZeroBigDecimalConverter.class)
  private Optional<BigDecimal> providerObligationToAcceptFullAmount;

  @Override
  public List<ExplanationOfBenefit.AdjudicationComponent> toFhirAdjudication() {
    return Stream.concat(
            cmsCharges.toFhirAdjudication().stream(),
            AdjudicationChargeType.LINE_PROVIDER_OBLIGATION_FULL_AMOUNT
                .toFhirAdjudicationOptional(providerObligationToAcceptFullAmount)
                .stream())
        .toList();
  }
}
