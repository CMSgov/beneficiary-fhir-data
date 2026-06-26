package gov.cms.bfd.server.ng.coverage;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import gov.cms.bfd.server.ng.input.CoverageCompositeId;
import gov.cms.bfd.server.ng.input.CoveragePart;
import gov.cms.bfd.server.ng.input.CoverageSearchCriteria;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import gov.cms.bfd.server.ng.loadprogress.LoadProgressRepository;
import gov.cms.bfd.server.ng.util.DateUtil;
import gov.cms.bfd.server.ng.util.FhirUtil;
import io.micrometer.core.annotation.Timed;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;
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

  private final CoverageRepository coverageRepository;
  private final LoadProgressRepository loadProgressRepository;
  private final DateUtil dateUtil;

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
        coverageRepository.searchBeneficiaryWithCoverage(
            new CoverageSearchCriteria(
                coverageCompositeId.beneSk(), new DateTimeRange(), Optional.empty()));

    var benefitDate = dateUtil.nowAoe();
    return beneficiaryOpt.map(beneficiary -> beneficiary.toFhir(coverageCompositeId, benefitDate));
  }

  /**
   * Searches for Coverage resources based on the parsed composite ID and lastUpdated range.
   *
   * @param parsedCoverageId The parsed composite ID (guaranteed Part A or B by provider).
   * @param lastUpdated The date range for _lastUpdated filter.
   * @return A Bundle of Coverage resources.
   */
  @Timed("application.coverage.handler.search_by_coverage_id")
  public Bundle searchByCoverageId(
      CoverageCompositeId parsedCoverageId, DateTimeRange lastUpdated) {
    var beneficiaryOpt =
        coverageRepository.searchBeneficiaryWithCoverage(
            new CoverageSearchCriteria(parsedCoverageId.beneSk(), lastUpdated, Optional.empty()));
    if (beneficiaryOpt.isEmpty()) {
      return FhirUtil.defaultBundle(loadProgressRepository::lastUpdated);
    }
    var beneficiary = beneficiaryOpt.get();
    var benefitDate = dateUtil.nowAoe();
    var coverage = beneficiary.toFhirCoverageIfPresent(parsedCoverageId, benefitDate);

    return FhirUtil.bundleOrDefault(coverage.map(r -> r), loadProgressRepository::lastUpdated);
  }

  /**
   * Searches for all Coverage resources associated with a given beneficiary SK, optionally filtered
   * by _lastUpdated.
   *
   * @param criteria Coverage Search Criteria.
   * @return A Bundle of Coverage resources.
   */
  @Timed("application.coverage.handler.search_by_beneficiary")
  public Bundle searchByBeneficiary(CoverageSearchCriteria criteria) {
    var beneficiaryOpt =
        coverageRepository
            .searchBeneficiaryWithCoverage(criteria)
            .filter(b -> !b.isMergedBeneficiary());
    if (beneficiaryOpt.isEmpty()) {
      return FhirUtil.bundleOrDefault(Stream.of(), loadProgressRepository::lastUpdated);
    }
    var beneficiary = beneficiaryOpt.get();
    var benefitDate = dateUtil.nowAoe();
    var coverageParts =
        criteria
            .coveragePart()
            .map(Stream::of)
            .orElseGet(() -> Arrays.stream(CoveragePart.values()));
    var coverages =
        coverageParts
            .map(
                c ->
                    beneficiary.toFhirCoverageIfPresent(
                        new CoverageCompositeId(c, beneficiary.getBeneSk()), benefitDate))
            .flatMap(Optional::stream);

    return FhirUtil.bundleOrDefault(coverages, loadProgressRepository::lastUpdated);
  }
}
