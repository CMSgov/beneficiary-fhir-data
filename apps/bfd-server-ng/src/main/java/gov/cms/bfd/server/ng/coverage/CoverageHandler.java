package gov.cms.bfd.server.ng.coverage;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import gov.cms.bfd.server.ng.beneficiary.BeneficiaryRepository;
import gov.cms.bfd.server.ng.beneficiary.model.Beneficiary;
import gov.cms.bfd.server.ng.beneficiary.model.CoverageIdentity;
import gov.cms.bfd.server.ng.input.CoverageCompositeId;
import gov.cms.bfd.server.ng.input.CoveragePart;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
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
   * @param parsedId The parsed and validated composite ID containing the CoveragePart and beneSk.
   * @param compositeId The original full ID string from the request, used for setting Coverage.id.
   * @return An {@link Optional} containing the {@link Coverage} resource if found, otherwise empty.
   * @throws InvalidRequestException if the compositeId format is invalid.
   */
  public Optional<Coverage> readCoverage(
      final CoverageCompositeId parsedId, final String compositeId) {

    String normalizedPartCode = parsedId.coveragePart().getStandardCode(); // e.g., "A", "B"
    long beneSk = parsedId.beneSk();

    Optional<Beneficiary> beneficiaryOpt =
        beneficiaryRepository.findById(beneSk, new DateTimeRange());
    Optional<Beneficiary> currentEffectiveBeneficiaryOpt =
        filterForCurrentEffective(beneficiaryOpt);

    if (currentEffectiveBeneficiaryOpt.isEmpty()) {
      return Optional.empty();
    }

    Beneficiary beneficiary = currentEffectiveBeneficiaryOpt.get();

    CoverageIdentity coverageIdentity = CoverageIdentity.from(beneficiary.getMbi());
    return toFhir(beneficiary, coverageIdentity, normalizedPartCode, compositeId);
  }

  /**
   * Filters an Optional to ensure that if a beneficiary is present. if beneSk equals its xrefSk
   * (i.e., it's the current effective record).
   *
   * @param beneficiaryOpt The Optional Beneficiary, potentially from a repository call.
   * @return An Optional containing the Beneficiary if it was present and is current effective,
   *     otherwise an empty Optional.
   */
  private Optional<Beneficiary> filterForCurrentEffective(Optional<Beneficiary> beneficiaryOpt) {
    return beneficiaryOpt.filter(
        beneficiary -> {
          return beneficiary.getBeneSk() == beneficiary.getXrefSk();
        });
  }

  /**
   * Orchestrates the complete transformation of a Beneficiary and its related data into a FHIR
   * Coverage resource for a specific part.
   *
   * @param beneficiary The fully loaded Beneficiary object (including its related collections).
   * @param normalizedPartCode The normalized coverage part identifier ("A", "B").
   * @param fullCompositeId The complete ID for the Coverage resource.
   * @param coverageIdentity The coverage Identity resource.
   * @return An {@link Optional} containing the fully populated {@link Coverage} resource, or {@link
   *     Optional#empty()} if essential part-specific data is missing.
   */
  private Optional<Coverage> toFhir(
      Beneficiary beneficiary,
      CoverageIdentity coverageIdentity,
      String normalizedPartCode,
      String fullCompositeId) {

    Coverage coverage = beneficiary.toFhirCoverage(fullCompositeId);

    coverageIdentity.toFhirMbiIdentifier().ifPresent(coverage::addIdentifier);
    coverageIdentity.getMbiValue().ifPresent(coverage::setSubscriberId);

    Optional<CoveragePart> partEnumOptional = CoveragePart.forCode(normalizedPartCode);

    if (partEnumOptional.isPresent()) {
      CoveragePart partEnumInstance = partEnumOptional.get();
      switch (partEnumInstance) {
        case PART_A:
          CoveragePart.addPartACoverageElementsToCoverage(coverage);
          break;
        case PART_B:
          CoveragePart.addPartBCoverageElementsToCoverage(coverage);
          break;
      }
    }
    return Optional.of(coverage);
  }
}
