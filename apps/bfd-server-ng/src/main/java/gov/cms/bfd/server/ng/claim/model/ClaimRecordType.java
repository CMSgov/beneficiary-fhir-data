package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Reference;

@Embeddable
@Getter
public class ClaimRecordType {
  @Column(name = "clm_nrln_ric_cd")
  private Optional<ClaimNearLineRecordTypeCode> claimNearLineRecordTypeCode;

  @Column(name = "clm_ric_cd")
  private Optional<ClaimRecordTypeCode> claimRecordTypeCode;

  public Optional<Reference> toFhirReference(ClaimTypeCode claimTypeCode) {
    return Stream.of(
            claimRecordTypeCode.map(ClaimRecordTypeCode::getDisplay),
            claimNearLineRecordTypeCode.map(ClaimNearLineRecordTypeCode::getDisplay),
            claimTypeCode.toDisplay())
        .flatMap(Optional::stream)
        .findFirst()
        .map(display -> new Reference().setDisplay(display));
  }

  public Stream<ExplanationOfBenefit.SupportingInformationComponent> toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return Stream.concat(
        claimRecordTypeCode.stream().map(code -> code.toFhir(supportingInfoFactory)),
        claimNearLineRecordTypeCode.stream().map(code -> code.toFhir(supportingInfoFactory)));
  }
}
