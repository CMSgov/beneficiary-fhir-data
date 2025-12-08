package gov.cms.bfd.server.ng.beneficiary.model;

import gov.cms.bfd.server.ng.model.ProfileType;
import gov.cms.bfd.server.ng.util.DateUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.Getter;

/** FHIR metadata information. */
@Embeddable
@Getter
public class Meta {
  @Column(name = "ignored_by_overrides", nullable = false)
  private ZonedDateTime updatedTimestamp;

  /**
   * Returns meta information for the Patient resource.
   *
   * @param profileType FHIR profile URL to add
   * @return meta
   */
  public org.hl7.fhir.r4.model.Meta toFhirPatient(ProfileType profileType) {
    return toFhirProfile(profileType.getPatientProfiles());
  }

  /**
   * Builds Coverage meta using a supplied lastUpdated.
   *
   * @param profileType FHIR profile URL to add
   * @return meta
   */
  public org.hl7.fhir.r4.model.Meta toFhirCoverage(ProfileType profileType) {
    return toFhirProfile(profileType.getCoverageProfiles());
  }

  private org.hl7.fhir.r4.model.Meta toFhirProfile(List<String> profiles) {
    var meta = new org.hl7.fhir.r4.model.Meta().setLastUpdated(DateUtil.toDate(updatedTimestamp));

    profiles.forEach(meta::addProfile);

    return meta;
  }
}
