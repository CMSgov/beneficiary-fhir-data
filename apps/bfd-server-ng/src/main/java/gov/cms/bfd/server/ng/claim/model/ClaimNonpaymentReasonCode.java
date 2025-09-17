package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import java.util.Optional;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;

class ClaimNonpaymentReasonCode {
  @Column(name = "clm_mdcr_npmt_rsn_cd")
  private Optional<String> nonpaymentReasonCode;

  Optional<Extension> toFhir() {
    return nonpaymentReasonCode.map(
        s ->
            new Extension()
                .setUrl(SystemUrls.BLUE_BUTTON_STRUCTURE_DEFINITION_NONPAYMENT_REASON_CODE)
                .setValue(
                    new Coding()
                        .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_NONPAYMENT_REASON_CODE)
                        .setCode(s)));
  }
}
