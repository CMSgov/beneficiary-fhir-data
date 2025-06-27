package gov.cms.bfd.server.ng.coverage;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import gov.cms.bfd.server.ng.FhirUtil;
import gov.cms.bfd.server.ng.beneficiary.BeneficiaryRepository;
import gov.cms.bfd.server.ng.beneficiary.model.BeneficiaryThirdParty;
import gov.cms.bfd.server.ng.input.CoverageCompositeId;
import gov.cms.bfd.server.ng.input.CoveragePart;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
  @Transactional(readOnly = true)
  public Optional<Coverage> readCoverage(
      final CoverageCompositeId coverageCompositeId, final String compositeId) {

    var beneficiaryOpt =
        beneficiaryRepository.searchCurrentBeneficiary(
            coverageCompositeId.beneSk(),
            coverageCompositeId.coveragePart().getStandardCode(),
            new DateTimeRange());

    return beneficiaryOpt.map(
        beneficiary -> beneficiary.toFhirCoverage(compositeId, coverageCompositeId.coveragePart()));
  }

  /**
   * Searches for Coverage resources based on the parsed composite ID and lastUpdated range. Since
   * _id should be unique, this will typically return a bundle with 0 or 1 entry.
   *
   * @param parsedCoverageId The parsed composite ID (guaranteed Part A or B by provider).
   * @param compositeId original full ID string from the request, used for setting Coverage.id.
   * @param lastUpdated The date range for _lastUpdated filter.
   * @return A Bundle of Coverage resources.
   */
  @Transactional(readOnly = true)
  public Bundle searchByCoverageId(
      CoverageCompositeId parsedCoverageId, final String compositeId, DateTimeRange lastUpdated) {

    var beneficiaryOpt =
        beneficiaryRepository.searchCurrentBeneficiary(
            parsedCoverageId.beneSk(),
            parsedCoverageId.coveragePart().getStandardCode(),
            lastUpdated);

    var coverages =
        beneficiaryOpt
            .map(
                beneficiary ->
                    beneficiary.toFhirCoverage(compositeId, parsedCoverageId.coveragePart()))
            .stream()
            .collect(Collectors.toList());

    return FhirUtil.bundleOrDefault(
        (List<Resource>) (List<?>) coverages, beneficiaryRepository::beneficiaryLastUpdated);
  }

  /**
   * Searches for all Coverage resources (typically Part A and Part B FFS) associated with a given
   * beneficiary SK, optionally filtered by _lastUpdated.
   *
   * @param beneSk The beneficiary surrogate key.
   * @param lastUpdated The date range for _lastUpdated filter.
   * @return A Bundle of Coverage resources.
   */
  @Transactional(readOnly = true)
  public Bundle searchByBeneficiary(Long beneSk, DateTimeRange lastUpdated) {

    var coverages = new ArrayList<>();
    var beneficiaryOpt =
        beneficiaryRepository
            .findById(beneSk, lastUpdated)
            .filter(b -> b.getBeneSk() == b.getXrefSk());

    beneficiaryOpt.ifPresent(
        beneficiaryDetail -> {
          for (BeneficiaryThirdParty tp : beneficiaryDetail.getBeneficiaryThirdParties()) {
            coverages.add(
                beneficiaryDetail.toFhirCoverage(
                    String.valueOf(beneSk),
                    CoveragePart.forCode(tp.getId().getThirdPartyTypeCode()).get()));
          }
        });

    return FhirUtil.bundleOrDefault(
        (List<Resource>) (List<?>) coverages, beneficiaryRepository::beneficiaryLastUpdated);
  }
}
