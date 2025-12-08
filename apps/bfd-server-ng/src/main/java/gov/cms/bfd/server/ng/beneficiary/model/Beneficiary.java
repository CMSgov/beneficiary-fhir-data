package gov.cms.bfd.server.ng.beneficiary.model;

import gov.cms.bfd.server.ng.model.ProfileType;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hl7.fhir.r4.model.Patient;

/** Main entity representing the beneficiary table. */
@Entity
@Getter
@Table(name = "valid_beneficiary", schema = "idr")
public class Beneficiary extends BeneficiaryBase {
  @Embedded
  @AttributeOverride(name = "updatedTimestamp", column = @Column(name = "bfd_patient_updated_ts"))
  Meta meta;

  /**
   * Convenience method to convert to FHIR Patient with a specific profile.
   *
   * @param profileType the FHIR profile type
   * @return patient record
   */
  public Patient toFhir(ProfileType profileType) {
    return super.toFhir(profileType, meta);
  }
}
