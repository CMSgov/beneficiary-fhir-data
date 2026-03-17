package gov.cms.bfd.server.ng.claim;

import gov.cms.bfd.server.ng.DbFilterBuilder;
import gov.cms.bfd.server.ng.claim.model.ClaimBase;
import gov.cms.bfd.server.ng.claim.model.SystemType;
import java.util.List;

/**
 * Definition of a claim type used for executing claim searches.
 *
 * @param baseQuery base query
 * @param claimClass entity class
 * @param systemType system type which indicates a claim's source
 */
public record ClaimTypeDefinition(
    String baseQuery, Class<? extends ClaimBase> claimClass, SystemType systemType) {

  /**
   * Determine whether this claim type definition is compatible with any active filters. Inactive
   * filters do not exclude the claim type. search criteria.
   *
   * @param filters the filter builders to check against
   * @return boolean
   */
  public boolean matchesSystemType(List<DbFilterBuilder> filters) {
    return filters.stream().allMatch(filter -> filter.matchesSystemType(systemType));
  }
}
