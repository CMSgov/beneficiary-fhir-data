package gov.cms.bfd.server.ng.claim.model.common;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** The Latest Claim Occurence dates for institutional or professional claims. */
@Embeddable
public class DateClaimOcrncSupportingInfo implements SupportingInfoComponentBase {

  @Embedded private BenefitsExhaustedDate benefitsExhaustedDate;
  @Embedded private ActiveCareThroughDate activeCareThroughDate;
  @Embedded private QualifyStayFromDate qualifyStayFromDate;
  @Embedded private QualifyStayThroughDate qualifyStayThroughDate;
  @Embedded private NoncoveredDatesNormalized noncoveredDatesNormalized;

  @Override
  public List<ExplanationOfBenefit.SupportingInformationComponent> toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return Stream.concat(
            noncoveredDatesNormalized.toFhir(supportingInfoFactory).stream(),
            Stream.of(
                    benefitsExhaustedDate.toFhir(supportingInfoFactory),
                    activeCareThroughDate.toFhir(supportingInfoFactory),
                    qualifyStayFromDate.toFhir(supportingInfoFactory),
                    qualifyStayThroughDate.toFhir(supportingInfoFactory))
                .flatMap(Optional::stream))
        .toList();
  }
}
