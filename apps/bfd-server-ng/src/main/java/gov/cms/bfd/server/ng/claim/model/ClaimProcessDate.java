package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.DateUtil;
import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import java.time.LocalDate;
import java.util.Optional;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Extension;

class ClaimProcessDate {
  @Column(name = "clm_cms_proc_dt")
  private Optional<LocalDate> claimProcessDate;

  Optional<Extension> toFhir() {
    return claimProcessDate.map(
        date ->
            new Extension()
                .setUrl(SystemUrls.BLUE_BUTTON_STRUCTURE_DEFINITION_CLAIM_PROCESS_DATE)
                .setValue(new DateType().setValue(DateUtil.toDate(date))));
  }
}
