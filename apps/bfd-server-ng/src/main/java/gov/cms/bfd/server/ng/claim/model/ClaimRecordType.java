package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Optional;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Reference;

/**
 * Represents the record type information associated with a claim. This includes both the {@link
 * ClaimRecordTypeCode} and near-line {@link ClaimRecordTypeCode}. This class provides utilities for
 * converting these internal codes into their FHIR representations.
 */
@Embeddable
@Getter
public class ClaimRecordType {

  @Column private Optional<ClaimRecordTypeCode> claimRecordTypeCode;

  /**
   * Converts the record type information into a FHIR {@link Reference}.
   *
   * @return a FHIR {@link Reference} with the chosen display value, or empty if none is available
   */
  public Optional<Reference> toFhirReference() {
    return claimRecordTypeCode
        .map(ClaimRecordTypeCode::getDisplay)
        .map(display -> new Reference().setDisplay(display));
  }

  /**
   * Converts the record type information into a stream of FHIR {@link
   * ExplanationOfBenefit.SupportingInformationComponent} elements.
   *
   * @param supportingInfoFactory a factory for constructing supporting information elements
   * @return a stream of supporting information components derived from available record type codes
   */
  public Optional<ExplanationOfBenefit.SupportingInformationComponent> toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return claimRecordTypeCode.map(code -> code.toFhir(supportingInfoFactory));
  }
}
