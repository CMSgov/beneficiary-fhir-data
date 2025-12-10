package gov.cms.bfd.server.ng.coverage.model;

import gov.cms.bfd.server.ng.beneficiary.model.BeneficiaryBase;
import gov.cms.bfd.server.ng.beneficiary.model.OrganizationFactory;
import gov.cms.bfd.server.ng.beneficiary.model.RelationshipFactory;
import gov.cms.bfd.server.ng.input.CoverageCompositeId;
import gov.cms.bfd.server.ng.input.CoveragePart;
import gov.cms.bfd.server.ng.model.ProfileType;
import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Stream;
import lombok.Getter;
import org.hl7.fhir.r4.model.*;

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

  @OneToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "bene_sk")
  private BeneficiaryStatus beneficiaryStatus;

  @OneToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "bene_sk")
  private BeneficiaryEntitlementReason beneficiaryEntitlementReason;

  @OneToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "bene_sk")
  private BeneficiaryDualEligibility beneficiaryDualEligibility;

  @OneToMany(fetch = FetchType.EAGER)
  @JoinColumn(name = "bene_sk")
  private SortedSet<BeneficiaryMAPartDEnrollment> beneficiaryMAPartDEnrollments;

  @OneToMany(fetch = FetchType.EAGER)
  @JoinColumn(name = "bene_sk")
  private SortedSet<BeneficiaryMAPartDEnrollmentRx> beneficiaryMAPartDEnrollmentsRx;

  @OneToMany(fetch = FetchType.EAGER)
  @JoinColumn(name = "bene_sk")
  private SortedSet<BeneficiaryLowIncomeSubsidy> beneficiaryLowIncomeSubsidies;

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

  /** Beneficiary enrollment program type code 1 denotes Part C. */
  public static final String PART_C_PROGRAM_TYPE_CODE = "1";

  /** Beneficiary enrollment program type code 2 denotes Part D. */
  public static final String PART_D_PROGRAM_TYPE_CODE = "2";

  /** Beneficiary enrollment program type code 3 denotes Parts C and D. */
  public static final String PART_C_AND_D_PROGRAM_TYPE_CODE = "3";

  private Optional<BeneficiaryEntitlementReason> getEntitlementReason() {
    return Optional.ofNullable(beneficiaryEntitlementReason);
  }

  private Optional<BeneficiaryStatus> getStatus() {
    return Optional.ofNullable(beneficiaryStatus);
  }

  private Optional<BeneficiaryDualEligibility> getDualEligibility() {
    return Optional.ofNullable(beneficiaryDualEligibility);
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
   * Finds the enrollment record for a given coverage part.
   *
   * @param coveragePart The coverage part to find.
   * @return Optional containing the matching enrollment, or empty if not found.
   */
  private Optional<BeneficiaryMAPartDEnrollment> findEnrollment(CoveragePart coveragePart) {
    final String standardCode = coveragePart.getStandardCode();
    final LocalDate today = LocalDate.now();
    var matchedCoverage =
        beneficiaryMAPartDEnrollments.stream()
            .filter(
                e -> {
                  var programTypeCode = e.getId().getEnrollmentProgramTypeCode();
                  return switch (standardCode) {
                    case "C" ->
                        Objects.equals(programTypeCode, "1")
                            || Objects.equals(programTypeCode, "3");
                    case "D" ->
                        Objects.equals(programTypeCode, "2")
                            || Objects.equals(programTypeCode, "3");
                    default -> false;
                  };
                })
            .toList();

    Optional<BeneficiaryMAPartDEnrollment> latestActiveEnrollment =
        matchedCoverage.stream()
            .filter(
                e -> !e.getBeneficiaryEnrollmentPeriod().getEnrollmentBeginDate().isAfter(today))
            .findFirst();

    if (latestActiveEnrollment.isPresent()) {
      return latestActiveEnrollment;
    }

    // only future coverage exists
    return matchedCoverage.stream()
        .filter(e -> e.getBeneficiaryEnrollmentPeriod().getEnrollmentBeginDate().isAfter(today))
        .findFirst();
  }

  /**
   * Finds the recent or future rx enrollment record.
   *
   * @return Optional containing the matching enrollment, or empty if not found.
   */
  private Optional<BeneficiaryMAPartDEnrollmentRx> findRxEnrollment() {
    final LocalDate today = LocalDate.now();
    Optional<BeneficiaryMAPartDEnrollmentRx> latestActiveEnrollment =
        beneficiaryMAPartDEnrollmentsRx.stream()
            .filter(e -> !e.getId().getEnrollmentPdpRxInfoBeginDate().isAfter(today))
            .findFirst();

    if (latestActiveEnrollment.isPresent()) {
      return latestActiveEnrollment;
    }

    // only future coverage exists
    return beneficiaryMAPartDEnrollmentsRx.stream()
        .filter(e -> e.getId().getEnrollmentPdpRxInfoBeginDate().isAfter(today))
        .findFirst();
  }

  /**
   * Finds the active beneficiary low income subsidy record.
   *
   * @return Optional containing the matching LIS, or empty if not found.
   */
  private Optional<BeneficiaryLowIncomeSubsidy> findLowIncomeSubsidy() {
    final LocalDate today = LocalDate.now();
    Optional<BeneficiaryLowIncomeSubsidy> latestActiveEnrollment =
        beneficiaryLowIncomeSubsidies.stream()
            .filter(e -> !e.getId().getBenefitRangeBeginDate().isAfter(today))
            .findFirst();

    if (latestActiveEnrollment.isPresent()) {
      return latestActiveEnrollment;
    }

    // only future coverage exists
    return beneficiaryLowIncomeSubsidies.stream()
        .filter(e -> e.getId().getBenefitRangeBeginDate().isAfter(today))
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

    if (profileType == ProfileType.C4DIC) {
      coverage.setMeta(meta.toFhirCoverage(profileType, getMostRecentUpdated()));
      coverage.setId(UUID.randomUUID().toString());
      coverage.setBeneficiary(new Reference(PATIENT_REF + id));
      coverage.setSubscriber(new Reference(PATIENT_REF + id));
    } else {
      coverage.setId(coverageCompositeId.fullId());
      coverage.setMeta(meta.toFhirCoverage(profileType, getMostRecentUpdated()));
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
  public List<Coverage> toFhirCoverageIfPresent(CoverageCompositeId coverageCompositeId) {
    return toFhirCoverages(coverageCompositeId, "").stream()
        .filter(c -> !c.getIdentifier().isEmpty())
        .toList();
  }

  /**
   * Creates a FHIR Coverage resource or None if the beneficiary does not have the matching coverage
   * type.
   *
   * @param orgId The organization reference ID (only used if isC4DIC is true).
   * @param coverageCompositeId The full ID for the Coverage resource.
   * @return A FHIR Coverage object.
   */
  public List<Coverage> toFhirCoverageIfPresentC4DIC(
      CoverageCompositeId coverageCompositeId, String orgId) {
    return toFhirCoverages(coverageCompositeId, orgId).stream()
        .filter(c -> !c.getIdentifier().isEmpty())
        .toList();
  }

  /**
   * Creates a FHIR Coverage resource.
   *
   * @param coverageCompositeId The full ID for the Coverage resource.
   * @return A FHIR Coverage object.
   */
  public Coverage toFhir(CoverageCompositeId coverageCompositeId) {
    var coverage = setupBaseCoverage(coverageCompositeId, ProfileType.C4BB);
    coverage.setId(coverageCompositeId.fullId());

    coverage.setMeta(meta.toFhirCoverage(ProfileType.C4BB, getMostRecentUpdated()));

    coverage.setBeneficiary(new Reference(PATIENT_REF + beneSk));
    coverage.setRelationship(RelationshipFactory.createSelfSubscriberRelationship());

    coverage.setSubscriberId(identifier.getMbi());
    var coveragePart = coverageCompositeId.coveragePart();

    return switch (coveragePart) {
      case PART_A, PART_B -> mapCoverageAB(coverage, coveragePart, ProfileType.C4BB, "");
      case PART_C -> mapCoverageC(coverage, coveragePart, ProfileType.C4BB, "");
      case PART_D -> mapCoverageD(coverage, coveragePart, ProfileType.C4BB, "");
      case DUAL -> mapCoverageDual(coverage, coveragePart, ProfileType.C4BB, "");
    };
  }

  /**
   * Creates multiple FHIR Coverage resource.
   *
   * @param coverageCompositeId The full ID for the Coverage resource.
   * @param orgId The organization reference ID.
   * @return A list of FHIR Coverage objects.
   */
  public List<Coverage> toFhirCoverages(CoverageCompositeId coverageCompositeId, String orgId) {
    var profileType = (orgId.isBlank()) ? ProfileType.C4BB : ProfileType.C4DIC;
    var baseCoverage = setupBaseCoverage(coverageCompositeId, profileType);
    var coveragePart = coverageCompositeId.coveragePart();

    if (profileType.equals(ProfileType.C4BB)) {
      baseCoverage.setId(coverageCompositeId.fullId());

      baseCoverage.setMeta(meta.toFhirCoverage(ProfileType.C4BB, getMostRecentUpdated()));

      baseCoverage.setBeneficiary(new Reference(PATIENT_REF + beneSk));
      baseCoverage.setRelationship(RelationshipFactory.createSelfSubscriberRelationship());

      baseCoverage.setSubscriberId(identifier.getMbi());
    }

    if (coveragePart == CoveragePart.PART_A || coveragePart == CoveragePart.PART_B) {
      return List.of(mapCoverageAB(baseCoverage, coveragePart, profileType, orgId));
    }

    if (coveragePart == CoveragePart.DUAL) {
      return List.of(mapCoverageDual(baseCoverage, coveragePart, profileType, orgId));
    }

    if (coveragePart == CoveragePart.PART_C || coveragePart == CoveragePart.PART_D) {
      var enrollmentOpt = findEnrollment(coveragePart);

      if (enrollmentOpt.isEmpty()) {
        return List.of(toEmptyResource(baseCoverage));
      }

      var programTypeCode = enrollmentOpt.get().getId().getEnrollmentProgramTypeCode();

      return switch (programTypeCode) {
        case PART_C_PROGRAM_TYPE_CODE ->
            List.of(mapCoverageC(baseCoverage, coveragePart, profileType, orgId));
        case PART_D_PROGRAM_TYPE_CODE ->
            List.of(mapCoverageD(baseCoverage, coveragePart, profileType, orgId));
        case PART_C_AND_D_PROGRAM_TYPE_CODE -> {
          Coverage coverageC =
              mapCoverageC(baseCoverage.copy(), CoveragePart.PART_C, profileType, orgId);
          Coverage coverageD =
              mapCoverageD(baseCoverage.copy(), CoveragePart.PART_D, profileType, orgId);
          yield List.of(coverageC, coverageD);
        }
        default -> List.of(toEmptyResource(baseCoverage));
      };
    }

    return List.of(toEmptyResource(baseCoverage));
  }

  /**
   * Maps Part A and B coverage for both standard and C4DIC formats.
   *
   * @param coverage The base coverage object.
   * @param coveragePart The coverage part (A or B).
   * @param profileType Profile type.
   * @param orgId The organization reference ID (only used if isC4DIC is true).
   * @return The populated Coverage object.
   */
  private Coverage mapCoverageAB(
      Coverage coverage, CoveragePart coveragePart, ProfileType profileType, String orgId) {
    var entitlementOpt = findEntitlement(coveragePart);
    if (entitlementOpt.isEmpty()) {
      return toEmptyResource(coverage);
    }

    identifier.toFhir(orgId).ifPresent(coverage::addIdentifier);
    coverage.addClass_(coveragePart.toFhirClassComponent());

    var entitlement = entitlementOpt.get();
    coverage.setPeriod(entitlement.toFhirPeriod());
    coverage.setStatus(entitlement.toFhirStatus());

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
      getStatus().map(BeneficiaryStatus::toFhir).orElse(List.of()).forEach(coverage::addExtension);
    }

    getEntitlementReason()
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

  /**
   * Maps Dual eligibility coverage for both standard and C4DIC formats.
   *
   * @param coverage The base coverage object.
   * @param coveragePart The coverage part Dual.
   * @param profileType The profile type.
   * @param orgId The organization reference ID (only used if isC4DIC is true).
   * @return The populated Coverage object.
   */
  private Coverage mapCoverageDual(
      Coverage coverage, CoveragePart coveragePart, ProfileType profileType, String orgId) {
    var dualEligibilityOpt = getDualEligibility();
    if (dualEligibilityOpt.isEmpty()) {
      return toEmptyResource(coverage);
    }

    var dualEligibility = dualEligibilityOpt.get();
    coverage.setPeriod(dualEligibility.toFhirPeriod());
    coverage.setStatus(dualEligibility.toFhirStatus());
    identifier
        .toFhir(profileType == ProfileType.C4DIC ? orgId : "")
        .ifPresent(coverage::addIdentifier);
    coverage.addClass_(coveragePart.toFhirClassComponent());

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

  /**
   * Maps Part C coverage for both standard and C4DIC formats.
   *
   * @param coverage The base coverage object.
   * @param coveragePart The coverage part C.
   * @param profileType Profile type.
   * @param orgId The organization reference ID (only used if isC4DIC is true).
   * @return The populated Coverage object.
   */
  private Coverage mapCoverageC(
      Coverage coverage, CoveragePart coveragePart, ProfileType profileType, String orgId) {
    var enrollmentOpt = findEnrollment(coveragePart);
    if (enrollmentOpt.isEmpty()) {
      return toEmptyResource(coverage);
    }

    identifier.toFhir(orgId).ifPresent(coverage::addIdentifier);
    coverage.addClass_(coveragePart.toFhirClassComponent());

    var enrollment = enrollmentOpt.get();
    coverage.setId(createCoverageId(coveragePart, enrollment));
    coverage.setPeriod(enrollment.toFhirPeriod());
    coverage.setStatus(enrollment.toFhirStatus());

    if (profileType == ProfileType.C4DIC) {
      coverage.addPayor(new Reference().setReference(ORGANIZATION_REF + orgId));
      coverage.addExtension(
          new Extension(SystemUrls.C4DIC_ADD_INFO_EXT_URL)
              .setValue(new Annotation(new MarkdownType(C4DIC_ADD_INFO))));
    } else {
      var contract = enrollment.getEnrollmentContract();
      var cmsOrg = OrganizationFactory.createInsurerOrganization(contract);
      coverage.addContained(cmsOrg);
      coverage.addPayor(new Reference().setReference("#" + cmsOrg.getIdElement().getIdPart()));
      enrollment.toFhirExtensions().forEach(coverage::addExtension);
    }

    return coverage;
  }

  /**
   * Maps Part D coverage for both standard and C4DIC formats.
   *
   * @param coverage The base coverage object.
   * @param coveragePart The coverage part C.
   * @param profileType Profile type.
   * @param orgId The organization reference ID (only used if isC4DIC is true).
   * @return The populated Coverage object.
   */
  private Coverage mapCoverageD(
      Coverage coverage, CoveragePart coveragePart, ProfileType profileType, String orgId) {
    var enrollmentOpt = findEnrollment(coveragePart);
    if (enrollmentOpt.isEmpty()) {
      return toEmptyResource(coverage);
    }

    identifier.toFhir(orgId).ifPresent(coverage::addIdentifier);
    coverage.addClass_(coveragePart.toFhirClassComponent());
    coverage.setType(coveragePart.toFhirTypeCode());

    var enrollment = enrollmentOpt.get();
    coverage.setId(createCoverageId(coveragePart, enrollment));
    var rxInfo = findRxEnrollment();
    rxInfo
        .flatMap(BeneficiaryMAPartDEnrollmentRx::getMemberId)
        .ifPresent(
            memberId -> {
              Identifier memberIdentifier = new Identifier();
              memberIdentifier.setType(
                  new CodeableConcept()
                      .addCoding(new Coding(SystemUrls.HL7_IDENTIFIER, "MB", null)));
              memberIdentifier.setValue(memberId);
              coverage.addIdentifier(memberIdentifier);
            });
    rxInfo.ifPresent(rx -> rx.toFhirClassComponents().forEach(coverage::addClass_));

    var lowIncomeSubsidy = findLowIncomeSubsidy();
    lowIncomeSubsidy.ifPresent(lis -> lis.toFhirExtensions().forEach(coverage::addExtension));

    coverage.setPeriod(enrollment.toFhirPeriod());
    coverage.setStatus(enrollment.toFhirStatus());

    if (profileType == ProfileType.C4DIC) {
      coverage.addPayor(new Reference().setReference(ORGANIZATION_REF + orgId));
      coverage.addExtension(
          new Extension(SystemUrls.C4DIC_ADD_INFO_EXT_URL)
              .setValue(new Annotation(new MarkdownType(C4DIC_ADD_INFO))));
    } else {
      var contract = enrollment.getEnrollmentContract();
      var cmsOrg = OrganizationFactory.createInsurerOrganization(contract);
      coverage.addContained(cmsOrg);
      coverage.addPayor(new Reference().setReference("#" + cmsOrg.getIdElement().getIdPart()));
      enrollment.toFhirExtensions().forEach(coverage::addExtension);
    }

    return coverage;
  }

  private ZonedDateTime getMostRecentUpdated() {
    // Collect timestamps from beneficiary and all related child entities
    var allTimestamps =
        Stream.of(
            Stream.of(meta.getUpdatedTimestamp()),
            getStatus().map(BeneficiaryStatus::getBfdUpdatedTimestamp).stream(),
            getEntitlementReason()
                .map(BeneficiaryEntitlementReason::getBfdUpdatedTimestamp)
                .stream(),
            getDualEligibility().map(BeneficiaryDualEligibility::getBfdUpdatedTimestamp).stream(),
            beneficiaryEntitlements.stream().map(BeneficiaryEntitlement::getBfdUpdatedTimestamp),
            beneficiaryThirdParties.stream().map(BeneficiaryThirdParty::getBfdUpdatedTimestamp),
            beneficiaryMAPartDEnrollments.stream()
                .map(BeneficiaryMAPartDEnrollment::getBfdUpdatedTimestamp),
            beneficiaryMAPartDEnrollmentsRx.stream()
                .map(BeneficiaryMAPartDEnrollmentRx::getBfdUpdatedTimestamp),
            beneficiaryLowIncomeSubsidies.stream()
                .map(BeneficiaryLowIncomeSubsidy::getBfdUpdatedTimestamp));

    return allTimestamps
        .flatMap(s -> s)
        .max(Comparator.naturalOrder())
        .orElse(meta.getUpdatedTimestamp());
  }

  private String createCoverageId(
      CoveragePart coveragePart, BeneficiaryMAPartDEnrollment enrollment) {
    Long beneSk = enrollment.getId().getBeneSk();
    String part = coveragePart.getStandardCode();
    String contractNum = enrollment.getContractNumber();
    String contractPbpNum = enrollment.getDrugPlanNumber();
    return beneSk + part + contractNum + contractPbpNum;
  }
}
