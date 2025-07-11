package gov.cms.bfd.server.ng.beneficiary.model;

import gov.cms.bfd.server.ng.DateUtil;
import gov.cms.bfd.server.ng.input.CoverageCompositeId;
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
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
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

  @Column(name = "idr_trans_obslt_ts")
  private ZonedDateTime obsoleteTimestamp;

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
   * Determines if this beneficiary has been merged into another.
   *
   * @return whether the beneficiary is merged
   */
  public boolean isMergedBeneficiary() {
    return beneSk != xrefSk;
  }

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

  /**
   * Creates a FHIR Coverage resource.
   *
   * @param coverageCompositeId The full ID for the Coverage resource.
   * @return A FHIR Coverage object.
   */
  public Coverage toFhirCoverage(CoverageCompositeId coverageCompositeId) {
    var coverage = new Coverage();
    coverage.setId(coverageCompositeId.fullId());

    var coveragePart = coverageCompositeId.coveragePart();
    var coverageType = coveragePart.getStandardCode();
    var entitlementOpt =
        beneficiaryEntitlements.stream()
            .filter(
                e -> e.getId().getMedicareEntitlementTypeCode().toUpperCase().equals(coverageType))
            .findFirst();
    // If no entitlement record, bene doesn't have this coverage type
    if (entitlementOpt.isEmpty()) {
      return coverage;
    }
    var entitlement = entitlementOpt.get();

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
    var fhirPeriodOpt = entitlement.toFhirPeriod();
    fhirPeriodOpt.ifPresent(coverage::setPeriod);
    coverage.setStatus(Coverage.CoverageStatus.ACTIVE);

    var beneficiaryThirdParty =
        beneficiaryThirdParties.stream()
            .filter(tp -> tp.getId().getThirdPartyTypeCode().toUpperCase().equals(coverageType))
            .findFirst();

    Stream.of(
            beneficiaryThirdParty.map(BeneficiaryThirdParty::toFhirExtensions),
            Optional.of(entitlement.toFhirExtensions()),
            Optional.of(beneficiaryStatus.toFhirExtensions()),
            Optional.of(beneficiaryEntitlementReason.toFhirExtensions()))
        .flatMap(Optional::stream)
        .flatMap(Collection::stream)
        .forEach(coverage::addExtension);

    return coverage;
  }

  /**
   * Creates a FHIR Coverage resource or None if the beneficiary does not have the matching coverage
   * type.
   *
   * @param coverageCompositeId The full ID for the Coverage resource.
   * @return A FHIR Coverage object.
   */
  public Optional<Coverage> toFhirCoverageIfPresent(CoverageCompositeId coverageCompositeId) {
    var coverage = toFhirCoverage(coverageCompositeId);
    if (coverage.getIdentifier().isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(coverage);
  }
}
