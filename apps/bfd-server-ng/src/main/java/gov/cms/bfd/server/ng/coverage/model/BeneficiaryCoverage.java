package gov.cms.bfd.server.ng.coverage.model;

import gov.cms.bfd.server.ng.beneficiary.model.BeneficiaryBase;
import gov.cms.bfd.server.ng.beneficiary.model.OrganizationFactory;
import gov.cms.bfd.server.ng.beneficiary.model.RelationshipFactory;
import gov.cms.bfd.server.ng.input.CoverageCompositeId;
import gov.cms.bfd.server.ng.input.CoveragePart;
import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.UUID;
import lombok.Getter;
import org.hl7.fhir.r4.model.Annotation;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.Extension;
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

  @OneToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "bene_sk")
  private BeneficiaryStatus beneficiaryStatus;

  @OneToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "bene_sk")
  private BeneficiaryEntitlementReason beneficiaryEntitlementReason;

  @OneToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "bene_sk")
  private BeneficiaryDualEligibility beneficiaryDualEligibility;

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

  /** Patient reference */
  public static final String PATIENT_REF = "Patient/";

  /** Organization reference */
  public static final String ORGANIZATION_REF = "Organization/";

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
   * Sets up the base coverage resource with common fields.
   *
   * @param coverageCompositeId The full ID for the Coverage resource.
   * @param isC4DIC Whether this is a C4DIC profile coverage.
   * @return A base Coverage object with common fields populated.
   */
  private Coverage setupBaseCoverage(CoverageCompositeId coverageCompositeId, boolean isC4DIC) {
    var coverage = new Coverage();
    coverage.setRelationship(RelationshipFactory.createSelfSubscriberRelationship());
    coverage.setSubscriberId(identifier.getMbi());

    var coveragePart = coverageCompositeId.coveragePart();
    coverage.setType(coveragePart.toFhirTypeCode());
    coverage.addClass_(coveragePart.toFhirClassComponent());

    if (isC4DIC) {
      coverage.setMeta(meta.toFhirCoverage(SystemUrls.PROFILE_C4DIC_COVERAGE));
      coverage.setId(UUID.randomUUID().toString());
      coverage.setBeneficiary(new Reference(PATIENT_REF + id));
      coverage.setSubscriber(new Reference(PATIENT_REF + id));
    } else {
      coverage.setId(coverageCompositeId.fullId());
      coverage.setMeta(meta.toFhirCoverage(""));
      coverage.setBeneficiary(new Reference(PATIENT_REF + beneSk));
    }

    return coverage;
  }

  /**
   * Creates a FHIR Coverage resource.
   *
   * @param coverageCompositeId The full ID for the Coverage resource.
   * @return A FHIR Coverage object.
   */
  public Coverage toFhir(CoverageCompositeId coverageCompositeId) {
    var coverage = setupBaseCoverage(coverageCompositeId, false);
    var coveragePart = coverageCompositeId.coveragePart();

    return switch (coveragePart) {
      case PART_A, PART_B -> mapCoverageAB(coverage, coveragePart, false, null);
      case DUAL -> mapCoverageDual(coverage, false, null);
    };
  }

  /**
   * Creates a FHIR Coverage resource.
   *
   * @param coverageCompositeId The full ID for the Coverage resource.
   * @param orgId The organization reference ID.
   * @return A FHIR Coverage object.
   */
  public Coverage toFhirC4DIC(CoverageCompositeId coverageCompositeId, String orgId) {
    var coverage = setupBaseCoverage(coverageCompositeId, true);
    var coveragePart = coverageCompositeId.coveragePart();

    return switch (coveragePart) {
      case PART_A, PART_B -> mapCoverageAB(coverage, coveragePart, true, orgId);
      case DUAL -> mapCoverageDual(coverage, true, orgId);
    };
  }

  /**
   * Creates a FHIR Coverage resource or None if the beneficiary does not have the matching coverage
   * type.
   *
   * @param coverageCompositeId The full ID for the Coverage resource.
   * @return A FHIR Coverage object.
   */
  public Optional<Coverage> toFhirCoverageIfPresent(CoverageCompositeId coverageCompositeId) {
    return Optional.ofNullable(toFhir(coverageCompositeId))
        .filter(c -> !c.getIdentifier().isEmpty());
  }

  /**
   * Creates a FHIR Coverage resource or None if the beneficiary does not have the matching coverage
   * type.
   *
   * @param orgId The organization reference ID (only used if isC4DIC is true).
   * @param coverageCompositeId The full ID for the Coverage resource.
   * @return A FHIR Coverage object.
   */
  public Optional<Coverage> toFhirCoverageIfPresentC4DIC(
      CoverageCompositeId coverageCompositeId, String orgId) {
    return Optional.ofNullable(toFhirC4DIC(coverageCompositeId, orgId))
        .filter(c -> !c.getIdentifier().isEmpty());
  }

  /**
   * Maps Part A and B coverage for both standard and C4DIC formats.
   *
   * @param coverage The base coverage object.
   * @param coveragePart The coverage part (A or B).
   * @param isC4DIC Whether this is C4DIC format.
   * @param orgId The organization reference ID (only used if isC4DIC is true).
   * @return The populated Coverage object.
   */
  private Coverage mapCoverageAB(
      Coverage coverage, CoveragePart coveragePart, boolean isC4DIC, String orgId) {
    var entitlementOpt = findEntitlement(coveragePart);
    if (entitlementOpt.isEmpty()) {
      return toEmptyResource(coverage);
    }

    identifier.toFhir(isC4DIC ? orgId : "").ifPresent(coverage::addIdentifier);

    var entitlement = entitlementOpt.get();
    coverage.setPeriod(entitlement.toFhirPeriod());
    coverage.setStatus(entitlement.toFhirStatus());

    if (isC4DIC) {
      coverage.addPayor(new Reference().setReference(ORGANIZATION_REF + orgId));
      coverage.addExtension(
          new Extension(SystemUrls.EXT_BENE_BUYIN_CD_URL)
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
   * @param isC4DIC Whether this is C4DIC format.
   * @param orgId The organization reference ID (only used if isC4DIC is true).
   * @return The populated Coverage object.
   */
  private Coverage mapCoverageDual(Coverage coverage, boolean isC4DIC, String orgId) {
    var dualEligibilityOpt = getDualEligibility();
    if (dualEligibilityOpt.isEmpty()) {
      return toEmptyResource(coverage);
    }

    var dualEligibility = dualEligibilityOpt.get();
    coverage.setPeriod(dualEligibility.toFhirPeriod());
    coverage.setStatus(dualEligibility.toFhirStatus());
    identifier.toFhir(isC4DIC ? orgId : "").ifPresent(coverage::addIdentifier);

    if (isC4DIC) {
      coverage.addPayor(new Reference().setReference(ORGANIZATION_REF + orgId));
      coverage.addExtension(
          new Extension(SystemUrls.EXT_BENE_BUYIN_CD_URL)
              .setValue(new Annotation(new MarkdownType(C4DIC_ADD_INFO))));
    } else {
      var cmsOrg = OrganizationFactory.createCmsOrganization();
      coverage.addContained(cmsOrg);
      coverage.addPayor(new Reference().setReference("#" + cmsOrg.getIdElement().getIdPart()));
      dualEligibility.toFhirExtensions().forEach(coverage::addExtension);
    }

    return coverage;
  }
}
