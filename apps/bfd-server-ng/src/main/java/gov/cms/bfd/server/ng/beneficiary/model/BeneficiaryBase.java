package gov.cms.bfd.server.ng.beneficiary.model;

import gov.cms.bfd.server.ng.model.ProfileType;
import gov.cms.bfd.server.ng.util.DateUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.Getter;
import org.hl7.fhir.r4.model.Patient;

/**
 * Base class for beneficiary entities. This is used to prevent excessive fetching for patient
 * information.
 */
@Getter
@MappedSuperclass
public abstract class BeneficiaryBase {
  @Id
  @Column(name = "bene_sk")
  protected long beneSk;

  @Column(name = "bene_xref_efctv_sk_computed")
  protected long xrefSk;

  @Column(name = "bene_brth_dt")
  protected LocalDate birthDate;

  @Column(name = "bene_race_cd")
  protected RaceCode raceCode;

  @Column(name = "bene_sex_cd")
  protected Optional<SexCode> sexCode;

  @Column(name = "cntct_lang_cd")
  protected LanguageCode languageCode;

  @Column(name = "idr_trans_obslt_ts")
  protected ZonedDateTime obsoleteTimestamp;

  @Embedded protected Name beneficiaryName;
  @Embedded protected Address address;
  @Embedded protected Meta meta;
  @Embedded protected DeathDate deathDate;
  @Embedded protected CurrentIdentifier identifier;
  @Transient protected String id = UUID.randomUUID().toString();

  /**
   * Determines if this beneficiary has been merged into another.
   *
   * @return whether the beneficiary is merged
   */
  public boolean isMergedBeneficiary() {
    return beneSk != xrefSk;
  }

  /**
   * Convenience method to convert to FHIR Patient with a specific profile.
   *
   * @param profileType the FHIR profile type
   * @return patient record
   */
  public Patient toFhir(ProfileType profileType) {
    var patient = new Patient();

    if (profileType == ProfileType.C4DIC) {
      patient.setId(id);
    } else {
      patient.setId(String.valueOf(beneSk));
    }

    // Only return a skeleton resource or merged beneficiaries
    if (isMergedBeneficiary()) {
      return patient;
    }

    patient.setCommunication(List.of(languageCode.toFhir()));
    deathDate.toFhir().ifPresent(patient::setDeceased);
    patient.setName(List.of(beneficiaryName.toFhir()));
    patient.setBirthDate(DateUtil.toDateAndSanitize(birthDate));
    address.toFhir().ifPresent(a -> patient.setAddress(List.of(a)));
    sexCode.ifPresent(
        s -> {
          patient.setGender(s.toFhirAdministrativeGender());
          patient.addExtension(s.toFhirSexExtension());
        });
    patient.addExtension(raceCode.toFhir());
    patient.setMeta(meta.toFhirPatient(profileType));

    return patient;
  }
}
