package gov.cms.bfd.server.ng.claim.filter;

import gov.cms.bfd.server.ng.DbFilter;
import gov.cms.bfd.server.ng.DbFilterBuilder;
import gov.cms.bfd.server.ng.DbFilterParam;
import gov.cms.bfd.server.ng.claim.model.SystemType;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import java.util.ArrayList;
import org.jetbrains.annotations.NotNull;

/**
 * Query filter for last updated.
 *
 * @param lastUpdated last updated range
 */
public record LastUpdatedFilterParam(DateTimeRange lastUpdated) implements DbFilterBuilder {
  @Override
  @NotNull
  public DbFilter getFilters(@NotNull String claimTableAlias, @NotNull SystemType systemType) {
    var filterClause = new StringBuilder();
    var params = new ArrayList<DbFilterParam>();
    lastUpdated
        .getLowerBoundDateTime()
        .ifPresent(
            d -> {
              filterClause.append(
                  String.format(
                      " AND (%s.meta.updatedTimestamp %s :lastUpdatedLowerBound)",
                      claimTableAlias, lastUpdated.getLowerBoundSqlOperator()));
              params.add(new DbFilterParam("lastUpdatedLowerBound", d));
            });

    lastUpdated
        .getUpperBoundDateTime()
        .ifPresent(
            d -> {
              filterClause.append(
                  String.format(
                      " AND (%s.meta.updatedTimestamp %s :lastUpdatedUpperBound)",
                      claimTableAlias, lastUpdated.getUpperBoundSqlOperator()));
              params.add(new DbFilterParam("lastUpdatedUpperBound", d));
            });
    return new DbFilter(filterClause.toString(), params);
  }
}
