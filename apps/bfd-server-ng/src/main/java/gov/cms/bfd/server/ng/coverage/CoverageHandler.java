package gov.cms.bfd.server.ng.coverage;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import gov.cms.bfd.server.ng.beneficiary.BeneficiaryRepository;
import gov.cms.bfd.server.ng.input.CoverageCompositeId;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Coverage;
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

  private final CoverageRepository coverageRepository;

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

    // Fetch all necessary details using the CoverageRepository
    Optional<CoverageDetails> detailsOpt =
        coverageRepository.findCoverageDetails(coverageCompositeId, new DateTimeRange());

    if (detailsOpt.isEmpty()) {
      return Optional.empty();
    }
    CoverageDetails detailsSO = detailsOpt.get();
    return Optional.of(detailsSO.toFhir(compositeId, coverageCompositeId.coveragePart()));
  }
}
