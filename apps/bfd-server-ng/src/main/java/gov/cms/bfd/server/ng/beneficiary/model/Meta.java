package gov.cms.bfd.server.ng.beneficiary.model;

import gov.cms.bfd.server.ng.util.DateUtil;
import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.ZonedDateTime;
import java.util.List;

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
  public org.hl7.fhir.r4.model.Meta toFhirPatient(String profile) {
    return toFhirProfile(profile,
            List.of(SystemUrls.PROFILE_C4BB_PATIENT_2_1_0, SystemUrls.PROFILE_US_CORE_PATIENT_6_1_0));
  }

  /**
   * Returns meta information for the Coverage resource.
   *
   * @param profile optional FHIR profile URL to add; if null, defaults are used
   * @return meta
   */
  public org.hl7.fhir.r4.model.Meta toFhirCoverage(String profile) {
    return toFhirProfile(profile,
            List.of(SystemUrls.PROFILE_C4BB_COVERAGE_2_1_0, SystemUrls.PROFILE_US_CORE_COVERAGE_6_1_0));
  }

  private org.hl7.fhir.r4.model.Meta toFhirProfile(String profile, List<String> defaultProfiles) {
    var meta = new org.hl7.fhir.r4.model.Meta()
            .setLastUpdated(DateUtil.toDate(updatedTimestamp));

    if (profile != null) {
      meta.addProfile(profile);
    } else {
      defaultProfiles.forEach(meta::addProfile);
    }

    return meta;
  }
}
