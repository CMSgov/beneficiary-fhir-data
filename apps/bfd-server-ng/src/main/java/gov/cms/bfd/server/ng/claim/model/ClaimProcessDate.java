package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.DateUtil;
import gov.cms.bfd.server.ng.SystemUrls;
import jakarta.persistence.Column;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Extension;

import java.time.LocalDate;

public class ClaimProcessDate {
  @Column(name = "clm_cms_proc_dt")
  private LocalDate claimProcessDate;

  Extension toFhir() {
    return new Extension()
        .setUrl(SystemUrls.BLUE_BUTTON_STRUCTURE_DEFINITION_CLAIM_PROCESS_DATE)
        .setValue(new DateType().setValue(DateUtil.toDate(claimProcessDate)));
  }
}
