package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

import java.util.List;

@Embeddable
public class ClaimDateSupportingInfo {
  @Embedded private AdmissionPeriod admissionPeriod;
  @Embedded private ClaimSubmissionDate claimSubmissionDate;
  @Embedded private NchWeeklyProcessingDate nchWeeklyProcessingDate;
  @Embedded private ActiveCareThroughDate activeCareThroughDate;
  @Embedded private NoncoveredFromDate noncoveredFromDate;
  @Embedded private NoncoveredThroughDate noncoveredThroughDate;
  @Embedded private BenefitsExhaustedDate benefitsExhaustedDate;

  List<ExplanationOfBenefit.SupportingInformationComponent> toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return List.of(
        admissionPeriod.toFhir(supportingInfoFactory),
        claimSubmissionDate.toFhir(supportingInfoFactory),
        nchWeeklyProcessingDate.toFhir(supportingInfoFactory),
        activeCareThroughDate.toFhir(supportingInfoFactory),
        noncoveredFromDate.toFhir(supportingInfoFactory),
        noncoveredThroughDate.toFhir(supportingInfoFactory),
        benefitsExhaustedDate.toFhir(supportingInfoFactory));
  }
}
