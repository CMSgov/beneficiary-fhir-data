package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.SystemUrls;
import jakarta.persistence.Column;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;

class ClaimDispositionCode {
  @Column(name = "clm_disp_cd")
  private String claimDispositionCode;

  Extension toFhir() {
    return new Extension()
        .setUrl(SystemUrls.BLUE_BUTTON_STRUCTURE_DEFINITION_CLAIM_DISPOSITION_CODE)
        .setValue(
            new Coding()
                .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_CLAIM_DISPOSITION_CODE)
                .setCode(claimDispositionCode));
  }
}
