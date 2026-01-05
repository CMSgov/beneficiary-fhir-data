package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.Extension;

@Embeddable
class ClaimInstitutionalExtensions {
  @Embedded private ClaimNonpaymentReasonCode claimNonpaymentReasonCode;
  @Embedded private ClaimFIActionCode claimFIActionCode;

  List<Extension> toFhir() {
    return Stream.of(claimNonpaymentReasonCode.toFhir(), claimFIActionCode.toFhir())
        .flatMap(Optional::stream)
        .toList();
  }
}
