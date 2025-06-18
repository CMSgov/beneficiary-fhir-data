package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;

@Embeddable
class ClaimRecordTypeCode {
  @Column(name = "clm_nrln_ric_cd")
  private String claimRecordType;

  Extension toFhir() {
    return new Extension()
        .setUrl(SystemUrls.BLUE_BUTTON_STRUCTURE_DEFINITION_CLAIM_RECORD_TYPE)
        .setValue(
            new Coding()
                .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_CLAIM_RECORD_TYPE)
                .setCode(claimRecordType));
  }
}
