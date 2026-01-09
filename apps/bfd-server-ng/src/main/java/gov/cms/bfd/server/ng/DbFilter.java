package gov.cms.bfd.server.ng;

import java.util.List;

/**
 * Represents a database filter.
 *
 * @param filterClause SQL WHERE clause fragment for filtering
 * @param params params to pass to the query
 */
public record DbFilter(String filterClause, List<DbFilterParam> params) {}
