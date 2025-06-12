package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import org.hl7.fhir.r4.model.Extension;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Embeddable
public class ClaimInstitutionalExtensions {
  @Embedded private ClaimNonpaymentReasonCode claimNonpaymentReasonCode;
  @Embedded private ClaimFinalActionCode claimFinalActionCode;

  List<Extension> toFhir() {
    return Stream.of(claimNonpaymentReasonCode.toFhir(), claimFinalActionCode.toFhir())
        .flatMap(Optional::stream)
        .toList();
  }
}
