package gov.cms.bfd.server.ng.claim.filter;

import gov.cms.bfd.server.ng.DbFilter;
import gov.cms.bfd.server.ng.DbFilterBuilder;
import gov.cms.bfd.server.ng.DbFilterParam;
import gov.cms.bfd.server.ng.claim.model.SystemType;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import java.util.ArrayList;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

/**
 * Filters the billable period on the claim through date.
 *
 * @param claimThroughDate claim through date
 */
public record BillablePeriodFilterParam(DateTimeRange claimThroughDate) implements DbFilterBuilder {
  @NotNull
  @Override
  public DbFilter getFilters(@NotNull String claimTableAlias, @NotNull SystemType systemType) {
    var filterClause = new StringBuilder();
    var params = new ArrayList<DbFilterParam>();
    claimThroughDate
        .getLowerBoundDate()
        .ifPresent(
            d -> {
              filterClause.append(
                  String.format(
                      " AND (%s.billablePeriod.claimThroughDate %s :claimThroughDateLowerBound)",
                      claimTableAlias, claimThroughDate.getLowerBoundSqlOperator()));
              params.add(new DbFilterParam("claimThroughDateLowerBound", Optional.of(d)));
            });

    claimThroughDate
        .getUpperBoundDate()
        .ifPresent(
            d -> {
              filterClause.append(
                  String.format(
                      " AND (%s.billablePeriod.claimThroughDate %s :claimThroughDateUpperBound)",
                      claimTableAlias, claimThroughDate.getUpperBoundSqlOperator()));
              params.add(new DbFilterParam("claimThroughDateUpperBound", Optional.of(d)));
            });
    return new DbFilter(filterClause.toString(), params);
  }
}
