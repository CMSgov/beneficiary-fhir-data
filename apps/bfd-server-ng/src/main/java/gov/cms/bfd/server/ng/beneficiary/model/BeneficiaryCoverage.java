package gov.cms.bfd.server.ng.beneficiary.model;

import gov.cms.bfd.server.ng.input.CoverageCompositeId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import lombok.Getter;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Reference;

/** Entity representing the beneficiary table with coverage info. */
@Entity
@Getter
@Table(name = "beneficiary", schema = "idr")
public class BeneficiaryCoverage extends BeneficiaryBase {
  @OneToMany(fetch = FetchType.EAGER)
  @JoinColumn(name = "bene_sk")
  private Set<BeneficiaryEntitlement> beneficiaryEntitlements;

  @OneToMany(fetch = FetchType.EAGER)
  @JoinColumn(name = "bene_sk")
  private Set<BeneficiaryThirdParty> beneficiaryThirdParties;

  @OneToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "bene_sk")
  private BeneficiaryStatus beneficiaryStatus;

  @OneToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "bene_sk")
  private BeneficiaryEntitlementReason beneficiaryEntitlementReason;

  private Optional<BeneficiaryEntitlementReason> getEntitlementReason() {
    return Optional.ofNullable(beneficiaryEntitlementReason);
  }

  private Optional<BeneficiaryStatus> getStatus() {
    return Optional.ofNullable(beneficiaryStatus);
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
            getStatus().map(BeneficiaryStatus::toFhirExtensions),
            getEntitlementReason().map(BeneficiaryEntitlementReason::toFhirExtensions))
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
    var coverage = toFhir(coverageCompositeId);
    if (coverage.getIdentifier().isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(coverage);
  }
}
