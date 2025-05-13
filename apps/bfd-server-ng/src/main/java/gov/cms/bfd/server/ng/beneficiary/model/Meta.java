package gov.cms.bfd.server.ng.beneficiary.model;

import gov.cms.bfd.server.ng.DateUtil;
import gov.cms.bfd.server.ng.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;

/** FHIR metadata information. */
@Embeddable
public class Meta {
  @Column(name = "bfd_updated_ts", nullable = false)
  private LocalDate updatedTimestamp;

  org.hl7.fhir.r4.model.Meta toFhir() {
    return new org.hl7.fhir.r4.model.Meta()
        .setLastUpdated(DateUtil.toDate(updatedTimestamp))
        .addProfile(SystemUrls.PROFILE_C4BB_PATIENT_2_1_0)
        .addProfile(SystemUrls.PROFILE_US_CORE_PATIENT_6_1_0);
  }
}
