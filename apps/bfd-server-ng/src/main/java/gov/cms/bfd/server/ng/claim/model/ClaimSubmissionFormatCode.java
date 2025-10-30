package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Optional;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;

@Embeddable
public class ClaimSubmissionFormatCode {
  @Column(name = "clm_sbmt_frmt_cd")
  private Optional<String> claimFormatCode;

  Optional<Extension> toFhir() {
    return claimFormatCode.map(
        c ->
            new Extension()
                .setUrl(SystemUrls.BLUE_BUTTON_STRUCTURE_DEFINITION_CLAIM_FORMAT_CODE)
                .setValue(
                    new Coding()
                        .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_CLAIM_FORMAT_CODE)
                        .setCode(c)));
  }
}
