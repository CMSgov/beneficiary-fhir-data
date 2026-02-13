package gov.cms.bfd.server.ng.coverage.model;

import gov.cms.bfd.server.ng.beneficiary.model.BeneficiaryBase;
import gov.cms.bfd.server.ng.beneficiary.model.OrganizationFactory;
import gov.cms.bfd.server.ng.beneficiary.model.RelationshipFactory;
import gov.cms.bfd.server.ng.input.CoverageCompositeId;
import gov.cms.bfd.server.ng.input.CoveragePart;
import gov.cms.bfd.server.ng.model.ProfileType;
import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.UUID;
import lombok.Getter;
import org.hl7.fhir.r4.model.Annotation;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.MarkdownType;
import org.hl7.fhir.r4.model.Reference;

/** Entity representing the beneficiary table with coverage info. */
@Entity
@Getter
@Table(name = "valid_beneficiary", schema = "idr")
public class BeneficiaryCoverage extends BeneficiaryBase {
  @OneToMany(fetch = FetchType.EAGER)
  @JoinColumn(name = "bene_sk")
  private SortedSet<BeneficiaryEntitlement> beneficiaryEntitlements;

  @OneToMany(fetch = FetchType.EAGER)
  @JoinColumn(name = "bene_sk")
  private SortedSet<BeneficiaryThirdParty> beneficiaryThirdParties;

  @OneToMany(fetch = FetchType.EAGER)
  @JoinColumn(name = "bene_sk")
  private SortedSet<BeneficiaryPartCDEnrollment> beneficiaryPartCDEnrollments;

  @OneToMany(fetch = FetchType.EAGER)
  @JoinColumn(name = "bene_sk")
  private SortedSet<BeneficiaryLowIncomeSubsidy> beneficiaryLowIncomeSubsidies;

  @Embedded private BeneficiaryCoverageOptional coverageOptional;

  @Embedded private Meta meta;

  /**
   * Value for C4DIC Additional Insurance Card Information Extension <a
   * href="http://hl7.org/fhir/us/insurance-card/StructureDefinition/C4DIC-AdditionalCardInformation-extension">
   * C4DIC Additional Insurance Card Information </a>.
   */
  public static final String C4DIC_ADD_INFO =
      """
      You may be asked to show this card when you get health care services. Only give your personal Medicare \
      information to health care providers, or people you trust who work with Medicare on your behalf. \
      WARNING: Intentionally misusing this card may be considered fraud and/or other violation of \
      federal law and is punishable by law.

      Es posible que le pidan que muestre esta tarjeta cuando reciba servicios de cuidado médico. \
      Solamente dé su información personal de Medicare a los proveedores de salud, sus aseguradores o \
      personas de su confianza que trabajan con Medicare en su nombre. ¡ADVERTENCIA! El mal uso \
      intencional de esta tarjeta puede ser considerado como fraude y/u otra violación de la ley \
      federal y es sancionada por la ley.\
      """;

  /** Patient reference. */
  public static final String PATIENT_REF = "Patient/";

  /** Organization reference. */
  public static final String ORGANIZATION_REF = "Organization/";

  /**
   * Finds the enrollment record for a given coverage part.
   *
   * @param coveragePart The coverage part
   * @return Optional containing the matching enrollment, or empty if not found.
   */
  private Optional<BeneficiaryPartCDEnrollment> getEnrollment(CoveragePart coveragePart) {
    return beneficiaryPartCDEnrollments.stream()
        .filter(
            e ->
                e.getId()
                    .getEnrollmentProgramTypeCode()
                    .map(type -> type.supports(coveragePart))
                    .orElse(false))
        .findFirst();
  }

  // when we support multiple coverages this will have to be changed to return multiple rows.
  private Optional<BeneficiaryLowIncomeSubsidy> getLowIncomeSubsidy() {
    return beneficiaryLowIncomeSubsidies.stream().findFirst();
  }

  /**
   * Finds the entitlement record for a given coverage part.
   *
   * @param coveragePart The coverage part to find.
   * @return Optional containing the matching entitlement, or empty if not found.
   */
  private Optional<BeneficiaryEntitlement> findEntitlement(CoveragePart coveragePart) {
    var coverageType = coveragePart.getStandardCode();
    return beneficiaryEntitlements.stream()
        .filter(e -> e.getId().getMedicareEntitlementTypeCode().toUpperCase().equals(coverageType))
        .findFirst();
  }

  /**
   * Sets up the base coverage resource with common fields.
   *
   * @param coverageCompositeId The full ID for the Coverage resource.
   * @param profileType Whether this is a C4DIC profile coverage.
   * @return A base Coverage object with common fields populated.
   */
  private Coverage setupBaseCoverage(
      CoverageCompositeId coverageCompositeId, ProfileType profileType) {
    var coverage = new Coverage();
    coverage.setRelationship(RelationshipFactory.createSelfSubscriberRelationship());
    coverage.setSubscriberId(identifier.getMbi());

    var coveragePart = coverageCompositeId.coveragePart();
    coverage.setType(coveragePart.toFhirTypeCode());
    coverage.addClass_(coveragePart.toFhirClassComponent());

    if (profileType == ProfileType.C4DIC) {
      coverage.setMeta(meta.toFhir(profileType, coveragePart));
      coverage.setId(UUID.randomUUID().toString());
      coverage.setBeneficiary(new Reference(PATIENT_REF + id));
      coverage.setSubscriber(new Reference(PATIENT_REF + id));
    } else {
      coverage.setId(coverageCompositeId.fullId());
      coverage.setMeta(meta.toFhir(profileType, coveragePart));
      coverage.setBeneficiary(new Reference(PATIENT_REF + beneSk));
    }

    return coverage;
  }

  /**
   * Creates a FHIR Coverage resource or None if the beneficiary does not have the matching coverage
   * type.
   *
   * @param coverageCompositeId The full ID for the Coverage resource.
   * @return A FHIR Coverage object.
   */
  public Optional<Coverage> toFhirCoverageIfPresent(CoverageCompositeId coverageCompositeId, Instant clock) {
    return Optional.of(toFhir(coverageCompositeId, clock)).filter(c -> !c.getIdentifier().isEmpty());
  }

  /**
   * Creates a FHIR Coverage resource or None if the beneficiary does not have the matching coverage
   * type.
   *
   * @param coverageCompositeId The full ID for the Coverage resource.
   * @param orgId The organization reference ID (only used if isC4DIC is true).
   * @return A FHIR Coverage object.
   */
  public Optional<Coverage> toFhirCoverageIfPresentC4DIC(
      CoverageCompositeId coverageCompositeId, String orgId, Instant clock) {
    return Optional.of(toFhirC4DIC(coverageCompositeId, orgId, clock))
        .filter(c -> !c.getIdentifier().isEmpty());
  }

  /**
   * Creates a FHIR Coverage resource.
   *
   * @param coverageCompositeId The full ID for the Coverage resource.
   * @return A FHIR Coverage object.
   */
  public Coverage toFhir(CoverageCompositeId coverageCompositeId, Instant clock) {
    var coverage = setupBaseCoverage(coverageCompositeId, ProfileType.C4BB);
    coverage.setId(coverageCompositeId.fullId());

    coverage.setBeneficiary(new Reference(PATIENT_REF + beneSk));
    coverage.setRelationship(RelationshipFactory.createSelfSubscriberRelationship());

    coverage.setSubscriberId(identifier.getMbi());
    var coveragePart = coverageCompositeId.coveragePart();

    return switch (coveragePart) {
      case PART_A, PART_B -> mapCoverageAB(coverage, coveragePart, ProfileType.C4BB, "", clock);
      case PART_C -> mapCoverageC(coverage, coveragePart, ProfileType.C4BB, "");
      case PART_D -> mapCoverageD(coverage, coveragePart, ProfileType.C4BB, "");
      case DUAL -> mapCoverageDual(coverage, ProfileType.C4BB, "", clock);
    };
  }

  /**
   * Creates a FHIR Coverage resource.
   *
   * @param coverageCompositeId The full ID for the Coverage resource.
   * @param orgId The organization reference ID.
   * @return A FHIR Coverage object.
   */
  public Coverage toFhirC4DIC(CoverageCompositeId coverageCompositeId, String orgId, Instant clock) {
    var coverage = setupBaseCoverage(coverageCompositeId, ProfileType.C4DIC);
    var coveragePart = coverageCompositeId.coveragePart();

    return switch (coveragePart) {
      case PART_A, PART_B -> mapCoverageAB(coverage, coveragePart, ProfileType.C4DIC, orgId, clock);
      case PART_C -> mapCoverageC(coverage, coveragePart, ProfileType.C4DIC, orgId);
      case PART_D -> mapCoverageD(coverage, coveragePart, ProfileType.C4DIC, orgId);
      case DUAL -> mapCoverageDual(coverage, ProfileType.C4DIC, orgId, clock);
    };
  }

  private Coverage mapCoverageAB(
      Coverage coverage, CoveragePart coveragePart, ProfileType profileType, String orgId, Instant clock) {
    var entitlementOpt = findEntitlement(coveragePart);
    if (entitlementOpt.isEmpty()) {
      return toEmptyResource(coverage);
    }

    identifier.toFhir(orgId).ifPresent(coverage::addIdentifier);

    var entitlement = entitlementOpt.get();
    coverage.setPeriod(entitlement.toFhirPeriod());
    coverage.setStatus(entitlement.toFhirStatus(clock));

    if (profileType == ProfileType.C4DIC) {
      coverage.addPayor(new Reference().setReference(ORGANIZATION_REF + orgId));
      coverage.addExtension(
          new Extension(SystemUrls.C4DIC_ADD_INFO_EXT_URL)
              .setValue(new Annotation(new MarkdownType(C4DIC_ADD_INFO))));
    } else {
      var cmsOrg = OrganizationFactory.createCmsOrganization();
      coverage.addContained(cmsOrg);
      coverage.addPayor(new Reference().setReference("#" + cmsOrg.getIdElement().getIdPart()));
      var coverageType = coveragePart.getStandardCode();
      beneficiaryThirdParties.stream()
          .filter(tp -> tp.getId().getThirdPartyTypeCode().toUpperCase().equals(coverageType))
          .findFirst()
          .flatMap(BeneficiaryThirdParty::toFhir)
          .ifPresent(coverage::addExtension);
      entitlement.toFhirExtensions().forEach(coverage::addExtension);
      coverageOptional
          .getBeneficiaryStatus()
          .map(BeneficiaryStatus::toFhir)
          .orElse(List.of())
          .forEach(coverage::addExtension);
    }

    coverageOptional
        .getBeneficiaryEntitlementReason()
        .flatMap(BeneficiaryEntitlementReason::toFhir)
        .ifPresent(coverage::addExtension);

    return coverage;
  }

  /**
   * Creates an empty coverage resource (when beneficiary doesn't have this coverage type).
   *
   * @param coverage The coverage object with ID set.
   * @return An empty Coverage object with only the ID populated.
   */
  private Coverage toEmptyResource(Coverage coverage) {
    var emptyCoverage = new Coverage();
    emptyCoverage.setId(coverage.getId());
    return emptyCoverage;
  }

  private Coverage mapCoverageDual(Coverage coverage, ProfileType profileType, String orgId, Instant clock) {
    var dualEligibilityOpt = coverageOptional.getBeneficiaryDualEligibility();
    if (dualEligibilityOpt.isEmpty()) {
      return toEmptyResource(coverage);
    }

    var dualEligibility = dualEligibilityOpt.get();
    coverage.setPeriod(dualEligibility.toFhirPeriod());
    coverage.setStatus(dualEligibility.toFhirStatus(clock));
    identifier
        .toFhir(profileType == ProfileType.C4DIC ? orgId : "")
        .ifPresent(coverage::addIdentifier);

    if (profileType == ProfileType.C4DIC) {
      coverage.addPayor(new Reference().setReference(ORGANIZATION_REF + orgId));
      coverage.addExtension(
          new Extension(SystemUrls.C4DIC_ADD_INFO_EXT_URL)
              .setValue(new Annotation(new MarkdownType(C4DIC_ADD_INFO))));
    } else {
      var cmsOrg = OrganizationFactory.createCmsOrganization();
      coverage.addContained(cmsOrg);
      coverage.addPayor(new Reference().setReference("#" + cmsOrg.getIdElement().getIdPart()));
      dualEligibility.toFhirExtensions().forEach(coverage::addExtension);
    }

    return coverage;
  }

  private Coverage mapCoverageC(
      Coverage coverage, CoveragePart coveragePart, ProfileType profileType, String orgId) {
    var enrollmentOpt = getEnrollment(coveragePart);

    identifier.toFhir(orgId).ifPresent(coverage::addIdentifier);

    return enrollmentOpt
        .map(
            enrollment -> mapBaseCoverageCD(coverage, coveragePart, enrollment, profileType, orgId))
        .orElseGet(() -> toEmptyResource(coverage));
  }

  private Coverage mapCoverageD(
      Coverage coverage, CoveragePart coveragePart, ProfileType profileType, String orgId) {
    var enrollmentOpt = getEnrollment(coveragePart);
    if (enrollmentOpt.isEmpty()) {
      return toEmptyResource(coverage);
    }

    var enrollment = enrollmentOpt.get();
    var enrichedCoverage =
        mapBaseCoverageCD(coverage, coveragePart, enrollment, profileType, orgId);

    enrollment
        .getMemberId()
        .ifPresent(
            memberId -> {
              Identifier memberIdentifier = new Identifier();
              memberIdentifier.setType(
                  new CodeableConcept()
                      .addCoding(new Coding(SystemUrls.HL7_IDENTIFIER, "MB", null)));
              memberIdentifier.setValue(memberId);
              enrichedCoverage.addIdentifier(memberIdentifier);
            });
    enrollment.toFhirClassComponents().forEach(enrichedCoverage::addClass_);

    var lowIncomeSubsidy = getLowIncomeSubsidy();
    lowIncomeSubsidy.ifPresent(
        lis -> lis.toFhirExtensions().forEach(enrichedCoverage::addExtension));

    return enrichedCoverage;
  }

  private Coverage mapBaseCoverageCD(
      Coverage coverage,
      CoveragePart coveragePart,
      BeneficiaryPartCDEnrollment enrollment,
      ProfileType profileType,
      String orgId) {

    coverage.setId(createCoverageIdPartCD(coveragePart, enrollment));
    coverage.setPeriod(enrollment.toFhirPeriod());
    coverage.setStatus(enrollment.toFhirStatus());

    if (profileType == ProfileType.C4DIC) {
      coverage.addPayor(new Reference().setReference(ORGANIZATION_REF + orgId));
      coverage.addExtension(
          new Extension(SystemUrls.C4DIC_ADD_INFO_EXT_URL)
              .setValue(new Annotation(new MarkdownType(C4DIC_ADD_INFO))));
    } else {
      var contract = enrollment.getEnrollmentOptional().getEnrollmentContract();
      contract.ifPresent(
          c -> {
            var cmsOrg = OrganizationFactory.createInsurerOrganization(c);
            coverage.addContained(cmsOrg);
            coverage.addPayor(
                new Reference().setReference("#" + cmsOrg.getIdElement().getIdPart()));
          });

      enrollment.toFhirExtensions().forEach(coverage::addExtension);
    }

    return coverage;
  }

  private String createCoverageIdPartCD(
      CoveragePart coveragePart, BeneficiaryPartCDEnrollment enrollment) {
    var coverageType = coveragePart.getStandardSystem();
    var beneSk = enrollment.getId().getBeneSk();
    var contractNum = enrollment.getId().getContractNumber();
    var contractPbpNum = enrollment.getId().getPlanNumber();
    return String.format("%s-%s-%s-%s", coverageType, beneSk, contractNum, contractPbpNum);
  }
}
