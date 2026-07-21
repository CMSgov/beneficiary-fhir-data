package gov.cms.bfd.server.ng.input;

import gov.cms.bfd.server.ng.claim.model.ClaimTypeCode;
import gov.cms.bfd.server.ng.claim.model.MetaSourceSk;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/**
 * Represents the search criteria used to retrieve claims for a specific beneficiary.
 *
 * @param beneSk bene sk
 * @param mbi mbi
 * @param claimThroughDate service date
 * @param lastUpdated last updated
 * @param limit record count
 * @param offset start index
 * @param tagCriteria tagCriteria
 * @param claimTypeCodes claim type codes
 * @param outcomes claim outcomes
 * @param sources claim sources
 */
public record ClaimSearchCriteria(
    long beneSk,
    String mbi,
    DateTimeRange claimThroughDate,
    DateTimeRange lastUpdated,
    Optional<Integer> limit,
    Optional<Integer> offset,
    List<List<TagCriterion>> tagCriteria,
    List<ClaimTypeCode> claimTypeCodes,
    List<List<ExplanationOfBenefit.RemittanceOutcome>> outcomes,
    List<List<MetaSourceSk>> sources) {

  /**
   * Alternate constructor defaults the MBI to an empty string so that other areas of the
   * application focused on just claims and not prior authorization don't have to provide it.
   */
  public ClaimSearchCriteria(
      long beneSk,
      DateTimeRange claimThroughDate,
      DateTimeRange lastUpdated,
      Optional<Integer> limit,
      Optional<Integer> offset,
      List<List<TagCriterion>> tagCriteria,
      List<ClaimTypeCode> claimTypeCodes,
      List<List<ExplanationOfBenefit.RemittanceOutcome>> outcomes,
      List<List<MetaSourceSk>> sources) {
    this(
        beneSk,
        "",
        claimThroughDate,
        lastUpdated,
        limit,
        offset,
        tagCriteria,
        claimTypeCodes,
        outcomes,
        sources);
  }

  /**
   * Returns the offset or the default.
   *
   * @return offset
   */
  public Integer resolveOffset() {
    return offset.orElse(0);
  }

  /**
   * Returns the limit or the default.
   *
   * @return limit
   */
  public Integer resolveLimit() {
    return resolveLimitWithExtra(0);
  }

  /**
   * Returns the limit or the default.
   *
   * @param extra extra to add for pagination checking than the requested limit
   * @return limit
   */
  public Integer resolveLimitWithExtra(int extra) {
    return limit.orElse(5000) + extra;
  }

  /**
   * Returns whether a claim through date filter has been provided.
   *
   * @return boolean
   */
  public boolean hasClaimThroughDate() {
    return claimThroughDate.hasBounds();
  }

  /**
   * Returns whether a last updated date filter has been provided.
   *
   * @return boolean
   */
  public boolean hasLastUpdated() {
    return lastUpdated().hasBounds();
  }

  /**
   * Returns whether tag-based filtering has been provided.
   *
   * @return boolean
   */
  public boolean hasTags() {
    return !tagCriteria.isEmpty();
  }

  /**
   * Returns whether claim type filters have been provided.
   *
   * @return boolean
   */
  public boolean hasClaimTypeCodes() {
    return !claimTypeCodes().isEmpty();
  }

  /**
   * Returns whether outcome-based filtering has been provided.
   *
   * @return boolean
   */
  public boolean hasOutcomes() {
    return !outcomes.isEmpty();
  }

  /**
   * Returns whether source-based filtering has been provided.
   *
   * @return boolean
   */
  public boolean hasSources() {
    return !sources.isEmpty();
  }
}
