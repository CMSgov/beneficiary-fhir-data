package gov.cms.bfd.server.ng.coverage;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import gov.cms.bfd.server.ng.FhirUtil;
import gov.cms.bfd.server.ng.beneficiary.BeneficiaryRepository;
import gov.cms.bfd.server.ng.beneficiary.model.Beneficiary;
import gov.cms.bfd.server.ng.beneficiary.model.BeneficiaryThirdParty;
import gov.cms.bfd.server.ng.input.CoverageCompositeId;
import gov.cms.bfd.server.ng.input.CoveragePart;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.stereotype.Component;

/**
 * Handler methods for the Coverage resource. This is called after the FHIR inputs from the resource
 * provider are converted into input types that are easier to work with.
 */
@Component
@RequiredArgsConstructor
public class CoverageHandler {

  private final BeneficiaryRepository beneficiaryRepository;

  /**
   * Reads a Coverage resource based on a composite ID ({part}-{bene_sk}).
   *
   * @param coverageCompositeId The parsed and validated composite ID containing the CoveragePart
   *     and beneSk.
   * @param compositeId The original full ID string from the request, used for setting Coverage.id.
   * @return An {@link Optional} containing the {@link Coverage} resource if found, otherwise empty.
   * @throws InvalidRequestException if the compositeId format is invalid.
   */
  public Optional<Coverage> readCoverage(
      final CoverageCompositeId coverageCompositeId, final String compositeId) {

    var beneficiaryOpt =
        beneficiaryRepository.searchCurrentBeneficiary(
            coverageCompositeId.beneSk(),
            coverageCompositeId.coveragePart().getStandardCode(),
            new DateTimeRange());

    return beneficiaryOpt.map(
        beneficiary -> toFhir(beneficiary, coverageCompositeId.coveragePart(), compositeId));
  }

  /**
   * Searches for Coverage resources based on the parsed composite ID and lastUpdated range.
   *
   * @param parsedCoverageId The parsed composite ID (guaranteed Part A or B by provider).
   * @param compositeId original full ID string from the request, used for setting Coverage.id.
   * @param lastUpdated The date range for _lastUpdated filter.
   * @return A Bundle of Coverage resources.
   */
  public Bundle searchByCoverageId(
      CoverageCompositeId parsedCoverageId, final String compositeId, DateTimeRange lastUpdated) {

    var beneficiaryOpt =
        beneficiaryRepository.searchCurrentBeneficiary(
            parsedCoverageId.beneSk(),
            parsedCoverageId.coveragePart().getStandardCode(),
            lastUpdated);

    var coverages =
        beneficiaryOpt
            .map(beneficiary -> toFhir(beneficiary, parsedCoverageId.coveragePart(), compositeId))
            .stream();

    return FhirUtil.bundleOrDefault(
        coverages.map(c -> c), beneficiaryRepository::beneficiaryLastUpdated);
  }

  /**
   * Searches for all Coverage resources associated with a given beneficiary SK, optionally filtered
   * by _lastUpdated.
   *
   * @param beneSk The beneficiary surrogate key.
   * @param lastUpdated The date range for _lastUpdated filter.
   * @return A Bundle of Coverage resources.
   */
  public Bundle searchByBeneficiary(Long beneSk, DateTimeRange lastUpdated) {

    var coverages = new ArrayList<>();
    var beneficiaryOpt =
        beneficiaryRepository.findById(beneSk, lastUpdated).filter(b -> !b.isMergedBeneficiary());

    beneficiaryOpt.ifPresent(
        beneficiary -> {
          for (BeneficiaryThirdParty tp : beneficiary.getBeneficiaryThirdParties()) {
            coverages.add(
                toFhir(
                    beneficiary,
                    CoveragePart.forCode(tp.getId().getThirdPartyTypeCode()).get(),
                    String.valueOf(beneSk)));
          }
        });

    return FhirUtil.bundleOrDefault(
        (List<Resource>) (List<?>) coverages, beneficiaryRepository::beneficiaryLastUpdated);
  }

  /**
   * Orchestrates the complete transformation of a Beneficiary and its related data into a FHIR
   * Coverage resource.
   *
   * @param beneficiary The current effective Beneficiary object.
   * @param coveragePart The {@link CoveragePart} enum instance (e.g., PART_A, PART_B).
   * @param coverageId The complete ID for the FHIR Coverage resource.
   * @return A populated FHIR Coverage object.
   */
  private Coverage toFhir(Beneficiary beneficiary, CoveragePart coveragePart, String coverageId) {

    Coverage coverage = beneficiary.toFhirCoverage(coverageId, coveragePart);

    List<Extension> allExtensions = new ArrayList<>();

    AtomicReference<List<Extension>> tpExtension = new AtomicReference<>(new ArrayList<>());
    beneficiary.getBeneficiaryThirdParties().stream()
        .forEach(
            tp -> {
              Optional<Period> fhirPeriodOpt = tp.createFhirPeriod();
              fhirPeriodOpt.ifPresent(coverage::setPeriod);
              coverage.setStatus(Coverage.CoverageStatus.ACTIVE);
              tpExtension.set(tp.toFhirExtensions());
            });
    allExtensions.addAll(tpExtension.get());
    AtomicReference<List<Extension>> entExtension = new AtomicReference<>(new ArrayList<>());
    beneficiary.getBeneficiaryEntitlements().stream()
        .forEach(
            ent -> {
              entExtension.set(ent.toFhirExtensions());
            });
    allExtensions.addAll(entExtension.get());
    allExtensions.addAll(beneficiary.getBeneficiaryStatus().toFhirExtensions());
    allExtensions.addAll(beneficiary.getBeneficiaryEntitlementReason().toFhirExtensions());

    if (!allExtensions.isEmpty()) {
      coverage.setExtension(allExtensions);
    }
    return coverage;
  }
}
