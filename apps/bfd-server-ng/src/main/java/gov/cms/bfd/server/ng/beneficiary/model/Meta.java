package gov.cms.bfd.server.ng.beneficiary.model;

import gov.cms.bfd.server.ng.model.ProfileType;
import gov.cms.bfd.server.ng.util.DateUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.ZonedDateTime;
import lombok.Getter;

/** FHIR metadata information. */
@Embeddable
@Getter
public class Meta {
  @Column(name = "bfd_patient_updated_ts", nullable = false)
  private ZonedDateTime updatedTimestamp;

  /**
   * Returns meta information for the Patient resource.
   *
   * @param profileType FHIR profile URL to add
   * @return meta
   */
  public org.hl7.fhir.r4.model.Meta toFhir(ProfileType profileType) {
      var meta = new org.hl7.fhir.r4.model.Meta().setLastUpdated(DateUtil.toDate(updatedTimestamp));
      profileType.getCoverageProfiles().forEach(meta::addProfile);
      return meta;
  }

}
