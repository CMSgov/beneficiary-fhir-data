package gov.cms.bfd.server.ng.claim.model.institutional;

import gov.cms.bfd.server.ng.claim.model.ActiveCareThroughDate;
import gov.cms.bfd.server.ng.claim.model.AdmissionPeriod;
import gov.cms.bfd.server.ng.claim.model.common.BenefitsExhaustedDate;
import gov.cms.bfd.server.ng.claim.model.ClaimProcessDate;
import gov.cms.bfd.server.ng.claim.model.common.ClaimSubmissionDate;
import gov.cms.bfd.server.ng.claim.model.NchWeeklyProcessingDate;
import gov.cms.bfd.server.ng.claim.model.NoncoveredFromDate;
import gov.cms.bfd.server.ng.claim.model.NoncoveredThroughDate;
import gov.cms.bfd.server.ng.claim.model.QualifyStayFromDate;
import gov.cms.bfd.server.ng.claim.model.QualifyStayThruDate;
import gov.cms.bfd.server.ng.claim.model.SupportingInfoComponentBase;
import gov.cms.bfd.server.ng.claim.model.common.SupportingInfoFactory;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@Embeddable
class ClaimDateSupportingInfo implements SupportingInfoComponentBase {
  @Embedded private AdmissionPeriod admissionPeriod;
  @Embedded private ClaimSubmissionDate claimSubmissionDate;
  @Embedded private NchWeeklyProcessingDate nchWeeklyProcessingDate;
  @Embedded private ActiveCareThroughDate activeCareThroughDate;
  @Embedded private NoncoveredFromDate noncoveredFromDate;
  @Embedded private NoncoveredThroughDate noncoveredThroughDate;
  @Embedded private BenefitsExhaustedDate benefitsExhaustedDate;
  @Embedded private QualifyStayFromDate qualifyStayFromDate;
  @Embedded private QualifyStayThruDate qualifyStayThruDate;
  @Embedded private ClaimProcessDate claimProcessDate;

  @Override
  public List<ExplanationOfBenefit.SupportingInformationComponent> toFhir(
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
            qualifyStayThruDate.toFhir(supportingInfoFactory),
            claimProcessDate.toFhir(supportingInfoFactory))
        .flatMap(Optional::stream)
        .toList();
  }
}
