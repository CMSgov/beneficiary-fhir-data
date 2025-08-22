package gov.cms.bfd.server.ng.eob;

import gov.cms.bfd.server.ng.FhirUtil;
import gov.cms.bfd.server.ng.beneficiary.BeneficiaryRepository;
import gov.cms.bfd.server.ng.claim.ClaimRepository;
import gov.cms.bfd.server.ng.claim.model.Claim;
import gov.cms.bfd.server.ng.claim.model.ClaimSourceId;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Component;

/**
 * Handler methods for the ExplanationOfBenefit resource. This is called after the FHIR inputs from
 * the resource provider are converted into input types that are easier to work with.
 */
@Component
@RequiredArgsConstructor
public class EobHandler {
  private final BeneficiaryRepository beneficiaryRepository;
  private final ClaimRepository claimRepository;

  /**
   * Returns a {@link Patient} by their {@link IdType}.
   *
   * @param fhirId FHIR ID
   * @return patient
   */
  public Optional<ExplanationOfBenefit> find(final Long fhirId) {
    return searchByIdInner(fhirId, new DateTimeRange(), new DateTimeRange());
  }

  /**
   * Search for claims data by bene.
   *
   * @param beneSk bene sk
   * @param count record count
   * @param serviceDate service date
   * @param lastUpdated last updated
   * @param startIndex start index
   * @param sourceIds sourceIds
   * @return bundle
   */
  public Bundle searchByBene(
      Long beneSk,
      Optional<Integer> count,
      DateTimeRange serviceDate,
      DateTimeRange lastUpdated,
      Optional<Integer> startIndex,
      List<ClaimSourceId> sourceIds) {
    var beneXrefSk = beneficiaryRepository.getXrefSkFromBeneSk(beneSk);
    // Don't return data for historical beneSks
    if (beneXrefSk.isEmpty() || !beneXrefSk.get().equals(beneSk)) {
      return new Bundle();
    }
    var eobs =
        claimRepository.findByBeneXrefSk(
            beneXrefSk.get(), serviceDate, lastUpdated, count, startIndex, sourceIds);
    return FhirUtil.bundleOrDefault(
        eobs.stream().map(Claim::toFhir), claimRepository::claimLastUpdated);
  }

  /**
   * Search for claims data by claim ID.
   *
   * @param claimUniqueId claim ID
   * @param serviceDate service date
   * @param lastUpdated last updated
   * @return bundle
   */
  public Bundle searchById(
      Long claimUniqueId, DateTimeRange serviceDate, DateTimeRange lastUpdated) {
    var eob = searchByIdInner(claimUniqueId, serviceDate, lastUpdated);
    return FhirUtil.bundleOrDefault(eob.map(e -> e), claimRepository::claimLastUpdated);
  }

  private Optional<ExplanationOfBenefit> searchByIdInner(
      Long claimUniqueId, DateTimeRange serviceDate, DateTimeRange lastUpdated) {
    var claim = claimRepository.findById(claimUniqueId, serviceDate, lastUpdated);

    return claim.map(Claim::toFhir);
  }
}
