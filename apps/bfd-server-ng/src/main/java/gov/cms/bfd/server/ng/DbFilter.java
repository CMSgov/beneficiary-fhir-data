package gov.cms.bfd.server.ng;

import java.util.List;

/**
 * Represents a database filter.
 *
 * @param filterClause SQL WHERE clause fragment for filtering
 * @param params params to pass to the query
 */
public record DbFilter(String filterClause, List<DbFilterParam> params) {
  /**
   * Returns an empty DB filter.
   *
   * @return filter
   */
  public static DbFilter empty() {
    return new DbFilter("", List.of());
  }
}
