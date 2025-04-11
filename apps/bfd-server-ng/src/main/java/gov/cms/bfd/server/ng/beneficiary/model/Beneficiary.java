package gov.cms.bfd.server.ng.beneficiary.model;

import gov.cms.bfd.server.ng.DateUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.List;
import org.hl7.fhir.r4.model.Patient;

@Entity
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

  @Column(name = "cntct_lang_cd", nullable = false)
  private LanguageCode languageCode;

  @Embedded private Name beneficiaryName;

  @Embedded private Address address;

  @Embedded private Meta meta;

  @Embedded private DeathDate deathDate;

  public Patient toFhir() {
    var patient =
        new Patient()
            .setName(List.of(beneficiaryName.toFhir()))
            .setBirthDate(DateUtil.toDate(birthDate))
            .setAddress(List.of(address.toFhir()))
            .setCommunication(List.of(languageCode.toFhir()));

    deathDate.toFhir().ifPresent(patient::setDeceased);
    patient.addExtension(raceCode.toFhir());
    patient.setMeta(meta.toFhir());

    return patient;
  }
}
