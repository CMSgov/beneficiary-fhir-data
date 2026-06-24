package gov.cms.bfd.server.ng.input;

import gov.cms.bfd.server.ng.claim.model.ClaimTypeCode;
import gov.cms.bfd.server.ng.claim.model.MetaSourceSk;
import gov.cms.bfd.server.ng.model.QueryProfile;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/**
 * Represents the search criteria used to retrieve claims for a specific beneficiary.
 *
 * @param beneSk bene sk
 * @param claimThroughDate service date
 * @param lastUpdated last updated
 * @param limit record count
 * @param offset start index
 * @param tagCriteria tagCriteria
 * @param claimTypeCodes claim type codes
 * @param outcomes claim outcomes
 * @param sources claim sources
 * @param queryProfile the query profile to use
 */
public record ClaimSearchCriteria(
    long beneSk,
    DateTimeRange claimThroughDate,
    DateTimeRange lastUpdated,
    Optional<Integer> limit,
    Optional<Integer> offset,
    List<List<TagCriterion>> tagCriteria,
    List<ClaimTypeCode> claimTypeCodes,
    List<List<ExplanationOfBenefit.RemittanceOutcome>> outcomes,
    List<List<MetaSourceSk>> sources,
    QueryProfile queryProfile) {

  /**
   * Constructs a new ClaimSearchCriteria with default QueryProfile.CMS.
   *
   * @param beneSk bene sk
   * @param claimThroughDate claim through date
   * @param lastUpdated last updated
   * @param limit record count
   * @param offset start index
   * @param tagCriteria tag criteria
   * @param claimTypeCodes claim type codes
   * @param outcomes outcomes
   * @param sources sources
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
        claimThroughDate,
        lastUpdated,
        limit,
        offset,
        tagCriteria,
        claimTypeCodes,
        outcomes,
        sources,
        QueryProfile.CMS);
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
