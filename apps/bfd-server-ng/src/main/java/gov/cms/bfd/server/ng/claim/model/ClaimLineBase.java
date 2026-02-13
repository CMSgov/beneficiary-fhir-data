package gov.cms.bfd.server.ng.claim.model;

import java.util.Optional;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Observation;

interface ClaimLineBase {

  Optional<Observation> toFhirObservation(int bfdRowId);

  Optional<ExplanationOfBenefit.ItemComponent> toFhirItemComponent();

  Optional<ExplanationOfBenefit.SupportingInformationComponent> toFhirSupportingInfo(
      SupportingInfoFactory supportingInfoFactory);

  RenderingProviderLineHistory getClaimLineRenderingProvider();

  Optional<Integer> getClaimLineNumber();
}
