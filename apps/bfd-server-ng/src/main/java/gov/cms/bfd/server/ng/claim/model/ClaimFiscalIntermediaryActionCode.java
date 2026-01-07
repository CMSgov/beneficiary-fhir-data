package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import java.util.Optional;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;

// This is the fiscal intermediary action code, not final action!
class ClaimFiscalIntermediaryActionCode {
  @Column(name = "clm_fi_actn_cd")
  private Optional<String> fiActionCode;

  Optional<Extension> toFhir() {
    return fiActionCode.map(
        s ->
            new Extension()
                .setUrl(SystemUrls.BLUE_BUTTON_STRUCTURE_DEFINITION_FISCAL_INTERMEDIARY_ACTION_CODE)
                .setValue(
                    new Coding()
                        .setSystem(
                            SystemUrls.BLUE_BUTTON_CODE_SYSTEM_FISCAL_INTERMEDIARY_ACTION_CODE)
                        .setCode(s)));
  }
}
