package gov.cms.bfd.server.ng.claim.model.rx;

import gov.cms.bfd.server.ng.ClaimFilterOptions;
import gov.cms.bfd.server.ng.claim.model.common.ClaimLineBase;
import gov.cms.bfd.server.ng.claim.model.common.RenderingCareTeamLine;
import gov.cms.bfd.server.ng.claim.model.common.SupportingInfoFactory;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** The Claim Line for a Rx claim in the CMS profile. */
@Getter
@Embeddable
public class ClaimLineRxCms implements ClaimLineBase {

  @Embedded private ClaimLineRx claimLineBase; // Composition over inheritance :nod:
  @Embedded private ClaimLineAdjudicationChargeRx adjudicationCharge;
  @Embedded private ClaimLineRxSupportingInfoCms claimRxSupportingInfoCms;

  @Override
  public List<ExplanationOfBenefit.SupportingInformationComponent> toFhirSupportingInfo(
          SupportingInfoFactory supportingInfoFactory) {
    return Stream.concat(
                    claimLineBase.getSupportingInfo().toFhir(supportingInfoFactory).stream(),
                    claimRxSupportingInfoCms.toFhir(supportingInfoFactory).stream())
            .toList();
  }

  @Override
  public Optional<ExplanationOfBenefit.ItemComponent> toFhirItemComponent(
      ClaimFilterOptions options) {
    var line = claimLineBase.toFhirItemComponent(options);
    line.ifPresent(
        adjudication -> adjudicationCharge.toFhir().forEach(adjudication::addAdjudication));
    return line;
  }

  @Override
  public Optional<RenderingCareTeamLine> getClaimLineRenderingProvider() {
    return Optional.empty();
  }

  @Override
  public Optional<Integer> getClaimLineNumber() {
    return Optional.empty();
  }
}
