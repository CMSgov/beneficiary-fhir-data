package gov.cms.bfd.server.ng.beneficiary.model;

import gov.cms.bfd.server.ng.DateUtil;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.List;
import lombok.Getter;
import org.hl7.fhir.r4.model.Patient;

/** Main entity representing the beneficiary table. */
@Entity
@Getter
@Table(name = "valid_beneficiary", schema = "idr")
public class Beneficiary extends BeneficiaryBase {

  /**
   * Transforms the beneficiary record to its FHIR representation.
   *
   * @return patient record
   */
  public Patient toFhir() {
    var patient = new Patient();
    patient.setId(String.valueOf(beneSk));

    // Only return a skeleton resource for merged beneficiaries
    if (isMergedBeneficiary()) {
      return patient;
    }

    patient.setName(List.of(beneficiaryName.toFhir()));
    patient.setBirthDate(DateUtil.toDate(birthDate));
    address.toFhir().ifPresent(a -> patient.setAddress(List.of(a)));
    sexCode.ifPresent(
        s -> {
          patient.setGender(s.toFhirAdministrativeGender());
          patient.addExtension(s.toFhirSexExtension());
        });

    patient.setCommunication(List.of(languageCode.toFhir()));
    deathDate.toFhir().ifPresent(patient::setDeceased);
    patient.addExtension(raceCode.toFhir());
    patient.setMeta(meta.toFhirPatient());

    return patient;
  }
}
