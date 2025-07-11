package gov.cms.bfd.server.ng.beneficiary.model;

import gov.cms.bfd.server.ng.DateUtil;
import gov.cms.bfd.server.ng.input.CoveragePart;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.Getter;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;

/** Main entity representing the beneficiary table. */
@Entity
@Getter
@Table(name = "beneficiary", schema = "idr")
public class Beneficiary {
  @Id
  @Column(name = "bene_sk")
  private long beneSk;

  @Column(name = "bene_xref_efctv_sk")
  private long beneXrefSk;

  @Column(name = "bene_xref_efctv_sk_computed")
  private long xrefSk;

  @Column(name = "bene_brth_dt")
  private LocalDate birthDate;

  @Column(name = "bene_race_cd")
  private RaceCode raceCode;

  @Column(name = "bene_sex_cd")
  private Optional<SexCode> sexCode;

  @Column(name = "cntct_lang_cd")
  private LanguageCode languageCode;

  @Embedded private Name beneficiaryName;
  @Embedded private Address address;
  @Embedded private Meta meta;
  @Embedded private DeathDate deathDate;
  @Embedded private Identity identity;

  @OneToMany(fetch = FetchType.EAGER)
  @JoinColumn(name = "bene_sk")
  private Set<BeneficiaryEntitlement> beneficiaryEntitlements;

  @OneToMany(fetch = FetchType.EAGER)
  @JoinColumn(name = "bene_sk")
  private Set<BeneficiaryThirdParty> beneficiaryThirdParties;

  @OneToOne
  @JoinColumn(name = "bene_sk")
  private BeneficiaryStatus beneficiaryStatus;

  @OneToOne
  @JoinColumn(name = "bene_sk")
  private BeneficiaryEntitlementReason beneficiaryEntitlementReason;

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
    patient.setMeta(meta.toFhirPatient());

    return patient;
  }

  /**
   * Creates an initial, partially populated FHIR Coverage resource using data directly available on
   * this Beneficiary entity. Further enrichment with Extensions will be done by the handler.
   *
   * @param fullCompositeId The full ID for the Coverage resource.
   * @param coveragePart the coverage Part
   * @return A partially populated FHIR Coverage object.
   */
  public Coverage toFhirCoverage(String fullCompositeId, CoveragePart coveragePart) {
    var coverage = new Coverage();

    coverage.setId(fullCompositeId);

    coverage.setMeta(meta.toFhirCoverage());

    coverage.setBeneficiary(new Reference("Patient/" + beneSk));

    coverage.setRelationship(RelationshipFactory.createSelfSubscriberRelationship());

    Organization cmsOrg = OrganizationFactory.createCmsOrganization();
    coverage.addContained(cmsOrg);

    coverage.addPayor(new Reference().setReference("#" + cmsOrg.getIdElement().getIdPart()));

    identity.toFhirMbiIdentifier().ifPresent(coverage::addIdentifier);
    coverage.setSubscriberId(identity.getMbiValue());

    coverage.setType(coveragePart.toFhirTypeCode());
    coverage.addClass_(coveragePart.toFhirClassComponent());
    return coverage;
  }
}
