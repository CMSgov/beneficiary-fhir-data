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
public class DateSupportingInfoInstitutionalCmsSharedSystems
    implements SupportingInfoComponentBase {

  @Embedded private DateSupportingInfoInstitutional dateSupportingInfoInstitutional;
  @Embedded private ClaimProcessDate claimProcessDate;

  @Override
  public List<ExplanationOfBenefit.SupportingInformationComponent> toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return Stream.concat(
            Stream.of(claimProcessDate.toFhir(supportingInfoFactory)).flatMap(Optional::stream),
            dateSupportingInfoInstitutional.toFhir(supportingInfoFactory).stream())
        .toList();
  }
}
