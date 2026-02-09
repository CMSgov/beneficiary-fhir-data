package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Reference;

/**
 * Represents the record type information associated with a claim. This includes both the {@link
 * ClaimRecordTypeCode} and near-line {@link ClaimNearLineRecordTypeCode}. This class provides
 * utilities for converting these internal codes into their FHIR representations.
 */
@Embeddable
@Getter
public class ClaimNearLineRecordType {
  @Column(name = "clm_nrln_ric_cd")
  private Optional<ClaimNearLineRecordTypeCode> claimNearLineRecordTypeCode;

  /**
   * Converts the record type information into a FHIR {@link Reference}.
   *
   * @param claimTypeCode the claim type code used as a fallback display value
   * @return a FHIR {@link Reference} with the chosen display value, or empty if none is available
   */
  public Optional<Reference> toFhirReference(ClaimTypeCode claimTypeCode) {
    return Stream.of(
            claimNearLineRecordTypeCode.map(ClaimNearLineRecordTypeCode::getDisplay),
            claimTypeCode.toDisplay())
        .flatMap(Optional::stream)
        .findFirst()
        .map(display -> new Reference().setDisplay(display));
  }

  /**
   * Converts the record type information into a stream of FHIR {@link
   * ExplanationOfBenefit.SupportingInformationComponent} elements.
   *
   * @param supportingInfoFactory a factory for constructing supporting information elements
   * @return a stream of supporting information components derived from available record type codes
   */
  public Stream<ExplanationOfBenefit.SupportingInformationComponent> toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return claimNearLineRecordTypeCode.stream().map(code -> code.toFhir(supportingInfoFactory));
  }

  /**
   * Gets the part display string.
   *
   * @return the part display string
   */
  public Optional<String> getPartDisplay() {
    return claimNearLineRecordTypeCode.map(ClaimNearLineRecordTypeCode::getPartDisplay);
  }
}
