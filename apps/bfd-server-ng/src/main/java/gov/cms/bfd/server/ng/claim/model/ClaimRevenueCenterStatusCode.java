package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import java.util.Optional;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;

/**
 * Represents the "Revenue Center Status Code" for a claim. This code identifies the status of a
 * revenue center line item within the claim.
 */
public class ClaimRevenueCenterStatusCode {
  @Column(name = "clm_rev_cntr_stus_cd")
  private Optional<String> revenueCenterStatusCode;

  Optional<Extension> toFhir() {
    return revenueCenterStatusCode.map(
        s ->
            new Extension()
                .setUrl(SystemUrls.BLUE_BUTTON_STRUCTURE_DEFINITION_REVENUE_CENTER_STATUS_CODE)
                .setValue(
                    new Coding()
                        .setSystem(SystemUrls.BLUE_BUTTON_CODE_SYSTEM_REVENUE_CENTER_STATUS_CODE)
                        .setCode(s)));
  }
}
