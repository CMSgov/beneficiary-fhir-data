package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.SystemUrls;
import jakarta.persistence.Column;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;

import java.util.Optional;

public class ClaimFinalActionCode {
  @Column(name = "clm_fi_actn_cd")
  private Optional<String> finalActionCode;

  Optional<Extension> toFhir() {
    return finalActionCode.map(
        s ->
            new Extension()
                .setUrl(SystemUrls.BLUE_BUTTON_STRUCTURE_DEFINITION_FINAL_ACTION_CODE)
                .setValue(
                    new CodeableConcept(
                        new Coding()
                            .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_FINAL_ACTION_CODE)
                            .setCode(s))));
  }
}
