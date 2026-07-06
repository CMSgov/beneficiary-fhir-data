package gov.cms.bfd.server.ng.input;

import java.util.Optional;

/**
 * Coverage Criteria to Search By.
 *
 * @param beneSk beneficiary id
 * @param lastUpdated date last updated
 * @param coveragePart Coverage Part filter
 */
public record CoverageSearchCriteria(
    long beneSk, DateTimeRange lastUpdated, Optional<CoveragePart> coveragePart) {

  /**
   * Returns whether a last updated date filter has been provided.
   *
   * @return boolean
   */
  public boolean hasLastUpdated() {
    return lastUpdated().hasBounds();
  }
}
