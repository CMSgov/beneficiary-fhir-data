package gov.cms.bfd.server.ng.input;

import gov.cms.bfd.server.ng.claim.model.MetaSourceSk;
import java.util.List;

/**
 * Represents the search criteria used to retrieve claims for a specific ids.
 *
 * @param claimUniqueIds claim ids
 * @param serviceDate service date
 * @param lastUpdated last updated
 * @param sources claim sources
 */
public record ClaimIdSearchCriteria(
    List<Long> claimUniqueIds,
    DateTimeRange serviceDate,
    DateTimeRange lastUpdated,
    List<List<MetaSourceSk>> sources) {

  /**
   * Returns whether a last updated date filter has been provided.
   *
   * @return boolean
   */
  public boolean hasServiceUpdated() {
    return serviceDate() != null && serviceDate().hasBounds();
  }

  /**
   * Returns whether a last updated date filter has been provided.
   *
   * @return boolean
   */
  public boolean hasLastUpdated() {
    return lastUpdated() != null && lastUpdated().hasBounds();
  }

  /**
   * Returns whether source-based filtering has been provided.
   *
   * @return boolean
   */
  public boolean hasSources() {
    return sources != null && !sources.isEmpty();
  }
}
