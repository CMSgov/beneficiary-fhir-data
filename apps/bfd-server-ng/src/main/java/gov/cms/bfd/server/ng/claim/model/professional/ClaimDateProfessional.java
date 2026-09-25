package gov.cms.bfd.server.ng.claim.model.professional;

import gov.cms.bfd.server.ng.claim.model.common.ActiveCareThroughDate;
import gov.cms.bfd.server.ng.claim.model.common.BenefitsExhaustedDate;
import gov.cms.bfd.server.ng.claim.model.common.NoncoveredFromDate;
import gov.cms.bfd.server.ng.claim.model.common.NoncoveredThroughDate;
import gov.cms.bfd.server.ng.claim.model.common.QualifyStayFromDate;
import gov.cms.bfd.server.ng.claim.model.common.QualifyStayThruDate;
import gov.cms.bfd.server.ng.claim.model.common.SupportingInfoComponentBase;
import gov.cms.bfd.server.ng.claim.model.common.SupportingInfoFactory;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** The claim date for institutional claims in the shared system. */
@Embeddable
public class ClaimDateProfessional implements SupportingInfoComponentBase {

  @Embedded private BenefitsExhaustedDate benefitsExhaustedDate;
  @Embedded private ActiveCareThroughDate activeCareThroughDate;
  @Embedded private NoncoveredFromDate noncoveredFromDate;
  @Embedded private NoncoveredThroughDate noncoveredThroughDate;
  @Embedded private QualifyStayFromDate qualifyStayFromDate;
  @Embedded private QualifyStayThruDate qualifyStayThruDate;

  @Override
  public List<ExplanationOfBenefit.SupportingInformationComponent> toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return Stream.of(
            benefitsExhaustedDate.toFhir(supportingInfoFactory),
            activeCareThroughDate.toFhir(supportingInfoFactory),
            noncoveredFromDate.toFhir(supportingInfoFactory),
            noncoveredThroughDate.toFhir(supportingInfoFactory),
            qualifyStayFromDate.toFhir(supportingInfoFactory),
            qualifyStayThruDate.toFhir(supportingInfoFactory))
        .flatMap(Optional::stream)
        .toList();
  }
}
