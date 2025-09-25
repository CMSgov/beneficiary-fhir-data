package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.Extension;

@Embeddable
class ClaimExtensions {
  @Embedded private ClaimContractorNumber claimContractorNumber;
  @Embedded private ClaimDispositionCode claimDispositionCode;

  List<Extension> toFhir() {
    return Stream.of(claimContractorNumber.toFhir(), claimDispositionCode.toFhir())
        .flatMap(Optional::stream)
        .toList();
  }
}
