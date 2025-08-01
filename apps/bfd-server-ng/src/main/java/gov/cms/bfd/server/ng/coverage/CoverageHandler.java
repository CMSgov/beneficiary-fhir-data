package gov.cms.bfd.server.ng.coverage;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import gov.cms.bfd.server.ng.FhirUtil;
import gov.cms.bfd.server.ng.beneficiary.BeneficiaryRepository;
import gov.cms.bfd.server.ng.input.CoverageCompositeId;
import gov.cms.bfd.server.ng.input.CoveragePart;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coverage;
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
   * @return An {@link Optional} containing the {@link Coverage} resource if found, otherwise empty.
   * @throws InvalidRequestException if the compositeId format is invalid.
   */
  public Optional<Coverage> readCoverage(final CoverageCompositeId coverageCompositeId) {

    var beneficiaryOpt =
        beneficiaryRepository.searchBeneficiaryWithCoverage(
            coverageCompositeId.beneSk(),
            Optional.of(coverageCompositeId.coveragePart().getStandardCode()),
            new DateTimeRange());

    return beneficiaryOpt.map(beneficiary -> beneficiary.toFhir(coverageCompositeId));
  }

  /**
   * Searches for Coverage resources based on the parsed composite ID and lastUpdated range.
   *
   * @param parsedCoverageId The parsed composite ID (guaranteed Part A or B by provider).
   * @param lastUpdated The date range for _lastUpdated filter.
   * @return A Bundle of Coverage resources.
   */
  public Bundle searchByCoverageId(
      CoverageCompositeId parsedCoverageId, DateTimeRange lastUpdated) {

    var beneficiaryOpt =
        beneficiaryRepository.searchBeneficiaryWithCoverage(
            parsedCoverageId.beneSk(),
            Optional.of(parsedCoverageId.coveragePart().getStandardCode()),
            lastUpdated);
    if (beneficiaryOpt.isEmpty()) {
      return FhirUtil.defaultBundle(beneficiaryRepository::beneficiaryLastUpdated);
    }
    var beneficiary = beneficiaryOpt.get();
    var coverage = beneficiary.toFhirCoverageIfPresent(parsedCoverageId);

    return FhirUtil.bundleOrDefault(
        coverage.map(c -> c), beneficiaryRepository::beneficiaryLastUpdated);
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
    var beneficiaryOpt =
        beneficiaryRepository
            .searchBeneficiaryWithCoverage(beneSk, Optional.empty(), lastUpdated)
            .filter(b -> !b.isMergedBeneficiary());
    if (beneficiaryOpt.isEmpty()) {
      return FhirUtil.bundleOrDefault(List.of(), beneficiaryRepository::beneficiaryLastUpdated);
    }
    var beneficiary = beneficiaryOpt.get();
    var coverages =
        Arrays.stream(CoveragePart.values())
            .map(
                c ->
                    beneficiary.toFhirCoverageIfPresent(
                        new CoverageCompositeId(c, beneficiary.getBeneSk())))
            .flatMap(Optional::stream);

    return FhirUtil.bundleOrDefault(
        coverages.map(c -> c), beneficiaryRepository::beneficiaryLastUpdated);
  }
}
