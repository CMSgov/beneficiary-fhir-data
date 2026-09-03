package gov.cms.bfd.server.ng.claim.model.institutional;

import gov.cms.bfd.server.ng.claim.model.common.ClaimProcessDate;
import gov.cms.bfd.server.ng.claim.model.common.SupportingInfoComponentBase;
import gov.cms.bfd.server.ng.claim.model.common.SupportingInfoFactory;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** The claim date for institutional claims, cms profile, shared system. */
@Embeddable
public class DateSupportingInfoCmsSharedSystems implements SupportingInfoComponentBase {

  @Embedded private DateSupportingInfo dateSupportingInfo;
  @Embedded private QualifyStayFromDate qualifyStayFromDate;
  @Embedded private ClaimProcessDate claimProcessDate;

  @Override
  public List<ExplanationOfBenefit.SupportingInformationComponent> toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return Stream.concat(
            Stream.of(
                    qualifyStayFromDate.toFhir(supportingInfoFactory),
                    claimProcessDate.toFhir(supportingInfoFactory))
                .flatMap(Optional::stream),
            dateSupportingInfo.toFhir(supportingInfoFactory).stream())
        .toList();
  }
}
