package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.SystemUrls;
import jakarta.persistence.Column;
import java.util.Optional;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;

/** Represents the "Obligated to Accept as Final One Indicator Code" (OTAF One Indicator Code). */
public class ClaimObligatedToAcceptAsFinalOneIndicatorCode {
  @Column(name = "clm_otaf_one_ind_cd")
  private Optional<String> otafOneIndicatorCode;

  Optional<Extension> toFhir() {
    return otafOneIndicatorCode.map(
        s ->
            new Extension()
                .setUrl(SystemUrls.BLUE_BUTTON_STRUCTURE_DEFINITION_OTAF_ONE_INDICATOR_CODE)
                .setValue(
                    new Coding()
                        .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_OTAF_ONE_INDICATOR_CODE)
                        .setCode(s)));
  }
}
