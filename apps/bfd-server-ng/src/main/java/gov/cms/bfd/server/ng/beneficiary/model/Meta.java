package gov.cms.bfd.server.ng.beneficiary.model;

import gov.cms.bfd.server.ng.DateUtil;
import gov.cms.bfd.server.ng.SystemUrl;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;

@Embeddable
public class Meta {
  @Column(name = "bfd_updated_ts", nullable = false)
  private LocalDate updatedTimestamp;

  org.hl7.fhir.r4.model.Meta toFhir() {
    return new org.hl7.fhir.r4.model.Meta()
        .setLastUpdated(DateUtil.toDate(updatedTimestamp))
        .addProfile(SystemUrl.PROFILE_C4BB_PATIENT_2_0_0)
        .addProfile(SystemUrl.PROFILE_US_CORE_PATIENT_3_1_1);
  }
}
