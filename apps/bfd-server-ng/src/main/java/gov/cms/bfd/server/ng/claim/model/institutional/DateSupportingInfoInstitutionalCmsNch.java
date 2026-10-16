package gov.cms.bfd.server.ng.claim.model.institutional;

import gov.cms.bfd.server.ng.claim.model.common.ClaimProcessDate;
import gov.cms.bfd.server.ng.claim.model.common.NchWeeklyProcessingDate;
import gov.cms.bfd.server.ng.claim.model.common.SupportingInfoComponentBase;
import gov.cms.bfd.server.ng.claim.model.common.SupportingInfoFactory;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** CMS profile specific information for claim date supporting info. */
@Embeddable
public class DateSupportingInfoInstitutionalCmsNch implements SupportingInfoComponentBase {

  @Embedded private DateSupportingInfoInstitutional dateSupportingInfoInstitutional;

  @Embedded
  private DateClaimOcrncSupportingInfoInstitutionalNch dateClaimOcrncSupportingInfoInstitutionalNch;

  @Embedded private NchWeeklyProcessingDate nchWeeklyProcessingDate;
  @Embedded private ClaimProcessDate claimProcessDate;
  @Embedded private NoncoveredFromDate noncoveredFromDate;
  @Embedded private NoncoveredThroughDate noncoveredThroughDate;

  @Override
  public List<ExplanationOfBenefit.SupportingInformationComponent> toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return Stream.concat(
            Stream.concat(
                Stream.of(
                        nchWeeklyProcessingDate.toFhir(supportingInfoFactory),
                        noncoveredFromDate.toFhir(supportingInfoFactory),
                        noncoveredThroughDate.toFhir(supportingInfoFactory),
                        claimProcessDate.toFhir(supportingInfoFactory))
                    .flatMap(Optional::stream),
                dateSupportingInfoInstitutional.toFhir(supportingInfoFactory).stream()),
            dateClaimOcrncSupportingInfoInstitutionalNch.toFhir(supportingInfoFactory).stream())
        .toList();
  }
}
