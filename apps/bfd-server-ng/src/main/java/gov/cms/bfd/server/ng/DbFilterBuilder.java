package gov.cms.bfd.server.ng;

/** Used by classes that create database filters. */
public interface DbFilterBuilder {
  /**
   * Gets the filters for this filter type.
   *
   * @param claimTableAlias SQL alias for the claim table
   * @return configured filter
   */
  public DbFilter getFilters(String claimTableAlias);
}
