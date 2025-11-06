package gov.cms.bfd.server.ng.beneficiary.model;

import gov.cms.bfd.server.ng.util.DateUtil;
import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.Getter;
import lombok.NonNull;

/** FHIR metadata information. */
@Embeddable
@Getter
public class Meta {
  @Column(name = "bfd_updated_ts", nullable = false)
  private ZonedDateTime updatedTimestamp;

  /**
   * Returns meta information for the Patient resource.
   *
   * @param profile optional FHIR profile URL to add; if null, defaults are used
   * @return meta
   */
  public org.hl7.fhir.r4.model.Meta toFhirPatient(@NonNull String profile) {
    return toFhirProfile(
        profile,
        updatedTimestamp,
        List.of(SystemUrls.PROFILE_C4BB_PATIENT_2_1_0, SystemUrls.PROFILE_US_CORE_PATIENT_6_1_0));
  }

  /**
   * Builds Coverage meta using a supplied lastUpdated.
   *
   * @param profile optional FHIR profile URL to add; if null, defaults are used
   * @param overrideLastUpdated last updated value
   * @return meta
   */
  public org.hl7.fhir.r4.model.Meta toFhirCoverage(
      @NonNull String profile, ZonedDateTime overrideLastUpdated) {
    return toFhirProfile(
        profile,
        overrideLastUpdated,
        List.of(SystemUrls.PROFILE_C4BB_COVERAGE_2_1_0, SystemUrls.PROFILE_US_CORE_COVERAGE_6_1_0));
  }

  private org.hl7.fhir.r4.model.Meta toFhirProfile(
      @NonNull String profile, ZonedDateTime overrideLastUpdated, List<String> defaultProfiles) {
    var meta =
        new org.hl7.fhir.r4.model.Meta().setLastUpdated(DateUtil.toDate(overrideLastUpdated));

    if (!profile.isEmpty()) {
      meta.addProfile(profile);
    } else {
      defaultProfiles.forEach(meta::addProfile);
    }

    return meta;
  }
}
