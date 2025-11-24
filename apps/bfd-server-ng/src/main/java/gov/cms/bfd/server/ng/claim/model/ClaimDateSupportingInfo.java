package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@Embeddable
class ClaimDateSupportingInfo {
  @Embedded private AdmissionPeriod admissionPeriod;
  @Embedded private ClaimSubmissionDate claimSubmissionDate;
  @Embedded private NchWeeklyProcessingDate nchWeeklyProcessingDate;
  @Embedded private ActiveCareThroughDate activeCareThroughDate;
  @Embedded private NoncoveredFromDate noncoveredFromDate;
  @Embedded private NoncoveredThroughDate noncoveredThroughDate;
  @Embedded private BenefitsExhaustedDate benefitsExhaustedDate;
  @Embedded private QualifyStayFromDate qualifyStayFromDate;
  @Embedded private QualifyStayThruDate qualifyStayThruDate;

  List<ExplanationOfBenefit.SupportingInformationComponent> toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return Stream.of(
            admissionPeriod.toFhir(supportingInfoFactory),
            claimSubmissionDate.toFhir(supportingInfoFactory),
            nchWeeklyProcessingDate.toFhir(supportingInfoFactory),
            activeCareThroughDate.toFhir(supportingInfoFactory),
            noncoveredFromDate.toFhir(supportingInfoFactory),
            noncoveredThroughDate.toFhir(supportingInfoFactory),
            benefitsExhaustedDate.toFhir(supportingInfoFactory),
            qualifyStayFromDate.toFhir(supportingInfoFactory),
            qualifyStayThruDate.toFhir(supportingInfoFactory))
        .filter(Objects::nonNull)
        .toList();
  }
}
