package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;

@Embeddable
public class ClaimContractorNumber {
  @Column(name = "clm_cntrctr_num")
  private String claimContractorNumber;

  Extension toFhir() {
    return new Extension()
        .setUrl(SystemUrls.BLUE_BUTTON_STRUCTURE_DEFINITION_CLAIM_CONTRACTOR_NUMBER)
        .setValue(
            new Coding()
                .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_CLAIM_CONTRACTOR_NUMBER)
                .setCode(claimContractorNumber));
  }
}
