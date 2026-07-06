package gov.cms.bfd.server.ng.claim.model.common;

import gov.cms.bfd.server.ng.ClaimFilterOptions;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Observation;

/** Interface for Claim Line items. */
@SuppressWarnings("checkstyle:MissingJavadocMethod")
public interface ClaimLineBase {

  Optional<Observation> toFhirObservation(int bfdRowId);

  Optional<ExplanationOfBenefit.ItemComponent> toFhirItemComponent(ClaimFilterOptions options);

  List<ExplanationOfBenefit.SupportingInformationComponent> toFhirSupportingInfo(
      SupportingInfoFactory supportingInfoFactory);

  Optional<RenderingCareTeamLine> getClaimLineRenderingProvider();

  Optional<Integer> getClaimLineNumber();

  Optional<String> getClaimLineDiagnosisCode();
}
