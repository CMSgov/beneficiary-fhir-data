package gov.cms.bfd.server.ng.claim.model.common;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** The claim date for institutional or professional claims. */
@Embeddable
public class DateClaimOcrncSupportingInfo implements SupportingInfoComponentBase {

  @Embedded private BenefitsExhaustedDate benefitsExhaustedDate;
  @Embedded private ActiveCareThroughDate activeCareThroughDate;
  @Embedded private NoncoveredFromDate noncoveredFromDate;
  @Embedded private NoncoveredThroughDate noncoveredThroughDate;
  @Embedded private QualifyStayFromDate qualifyStayFromDate;
  @Embedded private QualifyStayThroughDate qualifyStayThroughDate;

  @Override
  public List<ExplanationOfBenefit.SupportingInformationComponent> toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return Stream.of(
            benefitsExhaustedDate.toFhir(supportingInfoFactory),
            activeCareThroughDate.toFhir(supportingInfoFactory),
            noncoveredFromDate.toFhir(supportingInfoFactory),
            noncoveredThroughDate.toFhir(supportingInfoFactory),
            qualifyStayFromDate.toFhir(supportingInfoFactory),
            qualifyStayThroughDate.toFhir(supportingInfoFactory))
        .flatMap(Optional::stream)
        .toList();
  }
}
