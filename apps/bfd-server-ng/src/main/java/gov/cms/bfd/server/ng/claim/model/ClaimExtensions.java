package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.util.List;
import org.hl7.fhir.r4.model.Extension;

@Embeddable
class ClaimExtensions {
  @Embedded private ClaimContractorNumber claimContractorNumber;
  @Embedded private ClaimRecordTypeCode claimRecordTypeCode;
  @Embedded private ClaimDispositionCode claimDispositionCode;

  List<Extension> toFhir() {
    return List.of(
        claimContractorNumber.toFhir(),
        claimRecordTypeCode.toFhir(),
        claimDispositionCode.toFhir());
  }
}
