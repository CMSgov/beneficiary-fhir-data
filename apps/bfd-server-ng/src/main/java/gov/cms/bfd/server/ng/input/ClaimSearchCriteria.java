package gov.cms.bfd.server.ng.input;

import gov.cms.bfd.server.ng.claim.model.ClaimTypeCode;
import gov.cms.bfd.server.ng.claim.model.MetaSourceSk;
import java.util.List;
import java.util.Optional;

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
 * @param sources claim sources
 */
public record ClaimSearchCriteria(
    long beneSk,
    DateTimeRange claimThroughDate,
    DateTimeRange lastUpdated,
    Optional<Integer> limit,
    Optional<Integer> offset,
    List<List<TagCriterion>> tagCriteria,
    List<ClaimTypeCode> claimTypeCodes,
    List<List<MetaSourceSk>> sources) {

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
    return limit.orElse(5000);
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
  public boolean hasLasUpdated() {
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
   * Returns whether source-based filtering has been provided.
   *
   * @return boolean
   */
  public boolean hasSources() {
    return !sources.isEmpty();
  }
}
