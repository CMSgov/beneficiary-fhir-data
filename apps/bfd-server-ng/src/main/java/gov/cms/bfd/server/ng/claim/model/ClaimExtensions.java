package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import org.hl7.fhir.r4.model.Extension;

import java.util.List;

@Embeddable
public class ClaimExtensions {
  @Embedded private ClaimContractorNumber claimContractorNumber;
  @Embedded private ClaimRecordTypeCode claimRecordTypeCode;
  @Embedded private ClaimDispositionCode claimDispositionCode;
  @Embedded private ClaimProcessDate claimProcessDate;
  @Embedded private ClaimNonpaymentReasonCode claimNonpaymentReasonCode;

  List<Extension> toFhir() {
    return List.of(
        claimContractorNumber.toFhir(),
        claimRecordTypeCode.toFhir(),
        claimDispositionCode.toFhir(),
        claimProcessDate.toFhir());
  }
}
