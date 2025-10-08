package gov.cms.bfd.server.ng.coverage.model;

import gov.cms.bfd.server.ng.beneficiary.model.BeneficiaryBase;
import gov.cms.bfd.server.ng.beneficiary.model.OrganizationFactory;
import gov.cms.bfd.server.ng.beneficiary.model.RelationshipFactory;
import gov.cms.bfd.server.ng.input.CoverageCompositeId;
import gov.cms.bfd.server.ng.input.CoveragePart;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import lombok.Getter;
import org.hl7.fhir.r4.model.Coverage;
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
   * Creates a FHIR Coverage resource.
   *
   * @param coverageCompositeId The full ID for the Coverage resource.
   * @return A FHIR Coverage object.
   */
  public Coverage toFhir(CoverageCompositeId coverageCompositeId) {
    var coverage = new Coverage();
    coverage.setId(coverageCompositeId.fullId());
    coverage.setMeta(meta.toFhirCoverage());

    coverage.setBeneficiary(new Reference("Patient/" + beneSk));
    coverage.setRelationship(RelationshipFactory.createSelfSubscriberRelationship());

    coverage.setSubscriberId(identifier.getMbi());
    var coveragePart = coverageCompositeId.coveragePart();
    coverage.setType(coveragePart.toFhirTypeCode());
    coverage.addClass_(coveragePart.toFhirClassComponent());

    return switch (coveragePart) {
      case PART_A, PART_B -> mapCoverageAB(coverage, coveragePart);
      case DUAL -> mapCoverageDual(coverage);
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
    var coverage = toFhir(coverageCompositeId);
    if (coverage.getIdentifier().isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(coverage);
  }

  private Coverage mapCoverageAB(Coverage coverage, CoveragePart coveragePart) {
    var coverageType = coveragePart.getStandardCode();
    var entitlementOpt =
        beneficiaryEntitlements.stream()
            .filter(
                e -> e.getId().getMedicareEntitlementTypeCode().toUpperCase().equals(coverageType))
            .findFirst();
    // If no entitlement record, bene doesn't have this coverage type
    if (entitlementOpt.isEmpty()) {
      return toEmptyResource(coverage);
    }

    identifier.toFhir().ifPresent(coverage::addIdentifier);
    var cmsOrg = OrganizationFactory.createCmsOrganization();
    coverage.addContained(cmsOrg);
    coverage.addPayor(new Reference().setReference("#" + cmsOrg.getIdElement().getIdPart()));

    var entitlement = entitlementOpt.get();

    coverage.setPeriod(entitlement.toFhirPeriod());
    coverage.setStatus(entitlement.toFhirStatus());

    var beneficiaryThirdParty =
        beneficiaryThirdParties.stream()
            .filter(tp -> tp.getId().getThirdPartyTypeCode().toUpperCase().equals(coverageType))
            .findFirst();

    beneficiaryThirdParty.flatMap(BeneficiaryThirdParty::toFhir).ifPresent(coverage::addExtension);
    entitlement.toFhirExtensions().forEach(coverage::addExtension);
    getStatus().map(BeneficiaryStatus::toFhir).orElse(List.of()).forEach(coverage::addExtension);
    getEntitlementReason()
        .flatMap(BeneficiaryEntitlementReason::toFhir)
        .ifPresent(coverage::addExtension);

    return coverage;
  }

  private Coverage toEmptyResource(Coverage coverage) {
    var emptyCoverage = new Coverage();
    emptyCoverage.setId(coverage.getId());
    return emptyCoverage;
  }

  private Coverage mapCoverageDual(Coverage coverage) {
    var dualEligibilityOpt = getDualEligibility();
    if (dualEligibilityOpt.isEmpty()) {
      return toEmptyResource(coverage);
    }

    identifier.toFhir().ifPresent(coverage::addIdentifier);
    var cmsOrg = OrganizationFactory.createCmsOrganization();
    coverage.addContained(cmsOrg);
    coverage.addPayor(new Reference().setReference("#" + cmsOrg.getIdElement().getIdPart()));

    var dualEligibility = dualEligibilityOpt.get();
    coverage.setPeriod(dualEligibility.toFhirPeriod());
    coverage.setStatus(dualEligibility.toFhirStatus());

    dualEligibility.toFhirExtensions().forEach(coverage::addExtension);

    return coverage;
  }
}
