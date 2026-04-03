package gov.cms.bfd.server.ng;

import gov.cms.bfd.server.ng.claim.model.SystemType;

/** Used by classes that create database filters. */
public interface DbFilterBuilder {
  /**
   * Gets the filters for this filter type.
   *
   * @param tableAlias SQL alias for the table
   * @param systemType System type
   * @return configured filter
   */
  DbFilter getFilters(String tableAlias, SystemType systemType);

  /**
   * Determine whether claims from the specified system type should be queried based on the current
   * search criteria.
   *
   * @param systemType system type which indicates a claim's source
   * @return boolean
   */
  boolean matchesSystemType(SystemType systemType);
}
