package gov.cms.bfd.server.ng.claim.model.common;

import gov.cms.bfd.server.ng.ClaimFilterOptions;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Observation;

/** Interface for Claim Line items. */
public interface ClaimLineBase {

  /**
   * Transforms the claim line into an eob ItemComponent.
   *
   * @param options filter options
   * @return the eob ItemComponent
   */
  Optional<ExplanationOfBenefit.ItemComponent> toFhirItemComponent(ClaimFilterOptions options);

  /**
   * Transforms the claim line into an eob SupportingInformationComponent.
   *
   * @param supportingInfoFactory the supporting info factory for construction
   * @return the eob SupportingInformationComponent
   */
  default List<ExplanationOfBenefit.SupportingInformationComponent> toFhirSupportingInfo(
      SupportingInfoFactory supportingInfoFactory) {
    return List.of();
  }

  /**
   * Transforms the claim line into an Observation, only relevant for professional NCH claims.
   *
   * @param bfdRowId the row id used as the Observation id
   * @return the Observation
   */
  default Optional<Observation> toFhirObservation(int bfdRowId) {
    return Optional.empty();
  }

  /**
   * Default method for returning the RenderingCareTeamLine, used in professional and institutional.
   *
   * @return the RenderingCareTeamLine
   */
  Optional<RenderingCareTeamLine> getClaimLineRenderingProvider();

  /**
   * Default method for returning the claim line number, used in professional and institutional.
   *
   * @return the claim line number
   */
  Optional<Integer> getClaimLineNumber();

  /**
   * Default method for returning the diagnosis code, used in professional.
   *
   * @return the diagnosis code
   */
  default Optional<String> getClaimLineDiagnosisCode() {
    return Optional.empty();
  }
}
