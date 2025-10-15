package gov.cms.bfd.server.ng.claim;

import gov.cms.bfd.server.ng.beneficiary.BeneficiaryRepository;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Helper class for integration tests
 *
 * <p>Uses JPQL queries to decouple tests from table names and utilize JPA entity mappings.
 */
public class LastUpdatedTestHelper {

  private final ClaimRepository claimRepository;
  private final BeneficiaryRepository beneficiaryRepository;

  public LastUpdatedTestHelper(
      ClaimRepository claimRepository, BeneficiaryRepository beneficiaryRepository) {
    this.claimRepository = claimRepository;
    this.beneficiaryRepository = beneficiaryRepository;
  }

  /**
   * Retrieves the most recent bfd_updated_ts value for a claim by querying the claim and all its
   * related child entities. Computes the MAX timestamp in Java to mirror the production API's
   * Claim.getMostRecentUpdated() logic.
   *
   * @param claimId the claim ID to query (as a String)
   * @return the most recent ZonedDateTime from the claim and its children
   */
  public ZonedDateTime getClaimBfdUpdatedTs(String claimId) {
    var id = Long.parseLong(claimId);

    var claimOpt = claimRepository.findById(id, new DateTimeRange(), new DateTimeRange());
    if (claimOpt.isEmpty()) {
      return null;
    }
    var claim = claimOpt.get();

    // Reuse production logic by converting the entity to FHIR and reading its meta.lastUpdated
    var eob = claim.toFhir();
    var lastUpdatedDate = eob.getMeta().getLastUpdated();
    return lastUpdatedDate == null ? null : lastUpdatedDate.toInstant().atZone(ZoneId.of("UTC"));
  }

  /**
   * Retrieves the bfd_updated_ts value for a beneficiary.
   *
   * @param beneSk the beneficiary skeleton ID
   * @return the ZonedDateTime from beneficiary meta.updatedTimestamp
   */
  public ZonedDateTime getBeneficiaryBfdUpdatedTs(String beneSk) {
    var beneId = Long.parseLong(beneSk);
    var beneOpt = beneficiaryRepository.findById(beneId, new DateTimeRange());
    if (beneOpt.isEmpty()) {
      return null;
    }
    var patient = beneOpt.get().toFhir();
    var lastUpdated = patient.getMeta().getLastUpdated();
    return lastUpdated == null ? null : lastUpdated.toInstant().atZone(ZoneId.of("UTC"));
  }

  /**
   * Retrieves the beneficiary xrefSk for a given claim, used to look up the beneficiary associated
   * with a claim.
   *
   * @param claimId the claim ID
   * @return the beneficiary xrefSk as a String
   */
  public String getBeneficiaryIdForClaim(String claimId) {
    var id = Long.parseLong(claimId);
    var claimOpt = claimRepository.findById(id, new DateTimeRange(), new DateTimeRange());
    if (claimOpt.isEmpty()) {
      return null;
    }
    var beneSimple = claimOpt.get().getBeneficiary();
    return String.valueOf(beneSimple.getXrefSk());
  }
}
