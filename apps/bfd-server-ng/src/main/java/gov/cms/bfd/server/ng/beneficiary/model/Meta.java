package gov.cms.bfd.server.ng.beneficiary.model;

import gov.cms.bfd.server.ng.util.DateUtil;
import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.ZonedDateTime;

/** FHIR metadata information. */
@Embeddable
public class Meta {
  @Column(name = "bfd_updated_ts", nullable = false)
  private ZonedDateTime updatedTimestamp;

  /**
   * Returns meta information for the Patient resource.
   *
   * @param profile optional FHIR profile URL to add; if null, defaults are used
   * @return meta
   */
  org.hl7.fhir.r4.model.Meta toFhirPatient(String profile) {
    var meta = new org.hl7.fhir.r4.model.Meta().setLastUpdated(DateUtil.toDate(updatedTimestamp));

    if (profile != null) {
      meta.addProfile(profile);
    } else {
      meta.addProfile(SystemUrls.PROFILE_C4BB_PATIENT_2_1_0)
          .addProfile(SystemUrls.PROFILE_US_CORE_PATIENT_6_1_0);
    }

    return meta;
  }

  /**
   * Returns meta information for the Coverage resource.
   *
   * @param profile optional FHIR profile URL to add; if null, defaults are used
   * @return meta
   */
  public org.hl7.fhir.r4.model.Meta toFhirCoverage(String profile) {
    var meta = new org.hl7.fhir.r4.model.Meta().setLastUpdated(DateUtil.toDate(updatedTimestamp));

    if (profile != null) {
      meta.addProfile(profile);
    } else {
      meta.addProfile(SystemUrls.PROFILE_C4BB_COVERAGE_2_1_0)
          .addProfile(SystemUrls.PROFILE_US_CORE_COVERAGE_6_1_0);
    }

    return meta;
  }
}
