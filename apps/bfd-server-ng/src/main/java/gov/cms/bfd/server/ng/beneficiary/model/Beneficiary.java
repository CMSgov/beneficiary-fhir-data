package gov.cms.bfd.server.ng.beneficiary.model;

import gov.cms.bfd.server.ng.DateUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.Patient;

/** Main entity representing the beneficiary table. */
@Entity
@Getter
@Table(name = "beneficiary", schema = "idr")
public class Beneficiary {
  @Id
  @Column(name = "bene_sk", nullable = false)
  private long beneSk;

  @Column(name = "bene_xref_efctv_sk_computed", nullable = false)
  private long xrefSk;

  @Column(name = "bene_mbi_id", nullable = false)
  private String mbi;

  @Column(name = "bene_brth_dt", nullable = false)
  private LocalDate birthDate;

  @Column(name = "bene_race_cd", nullable = false)
  private RaceCode raceCode;

  @Column(name = "bene_sex_cd")
  private Optional<SexCode> sexCode;

  @Column(name = "cntct_lang_cd", nullable = false)
  private LanguageCode languageCode;

  @Embedded private Name beneficiaryName;
  @Embedded private Address address;
  @Embedded private Meta meta;
  @Embedded private DeathDate deathDate;

  /**
   * Transforms the beneficiary record to its FHIR representation.
   *
   * @return patient record
   */
  public Patient toFhir() {
    var patient = new Patient();
    patient.setId(String.valueOf(beneSk));

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
    patient.setMeta(meta.toFhir());

    return patient;
  }

  /**
   * Transforms this Beneficiary entity into a FHIR Coverage resource for a specific coverage part.
   *
   * @param partIdentifier The identifier for the coverage part (e.g., "part-a", "part-b").
   * @param fullCompositeId The full ID to be assigned to the FHIR Coverage resource (e.g.,
   *     "part-a-12345").
   * @return An {@link Optional} containing the {@link Coverage} resource if applicable for the
   *     given part, otherwise {@link Optional#empty()}.
   */
  public Optional<Coverage> toFhirCoverage(String partIdentifier, String fullCompositeId) {
    Coverage coverage = new Coverage();
    // Continue here
    return Optional.of(coverage);
  }
}
