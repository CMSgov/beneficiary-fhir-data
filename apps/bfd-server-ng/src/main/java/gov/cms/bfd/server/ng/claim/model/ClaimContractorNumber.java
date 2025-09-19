package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Optional;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;

@Embeddable
class ClaimContractorNumber {
  @Column(name = "clm_cntrctr_num")
  private Optional<String> claimContractorNumber;

  Optional<Extension> toFhir() {
    return claimContractorNumber.map(
        c ->
            new Extension()
                .setUrl(SystemUrls.BLUE_BUTTON_STRUCTURE_DEFINITION_CLAIM_CONTRACTOR_NUMBER)
                .setValue(
                    new Coding()
                        .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_CLAIM_CONTRACTOR_NUMBER)
                        .setCode(c)));
  }
}
