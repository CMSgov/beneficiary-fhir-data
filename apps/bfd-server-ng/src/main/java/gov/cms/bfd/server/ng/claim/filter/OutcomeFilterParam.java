package gov.cms.bfd.server.ng.claim.filter;

import gov.cms.bfd.server.ng.DbFilter;
import gov.cms.bfd.server.ng.DbFilterBuilder;
import gov.cms.bfd.server.ng.DbFilterParam;
import gov.cms.bfd.server.ng.claim.model.common.SystemType;
import gov.cms.bfd.server.ng.claim.model.common.ClaimPaidStatusCode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.jetbrains.annotations.NotNull;

/**
 * Filters claims by outcome.
 *
 * @param outcomes EOB claim outcomes
 */
public record OutcomeFilterParam(List<List<ExplanationOfBenefit.RemittanceOutcome>> outcomes)
    implements DbFilterBuilder {
  private static final List<Optional<ClaimPaidStatusCode>> COMPLETE_SHARED_SYSTEMS_STATUS_CODES =
      ClaimPaidStatusCode.findByOutcome(ExplanationOfBenefit.RemittanceOutcome.COMPLETE).stream()
          .map(Optional::of)
          .toList();

  @NotNull
  @Override
  public DbFilter getFilters(@NotNull String tableAlias, @NotNull SystemType systemType) {
    if (outcomes.isEmpty()) {
      return DbFilter.empty();
    }

    var stringBuilder = new StringBuilder();
    var params = new ArrayList<DbFilterParam>();

    for (var i = 0; i < outcomes.size(); i++) {
      var orList = outcomes.get(i);
      if (orList.isEmpty()) {
        continue;
      }

      var filter = getOutcomeFilter(tableAlias, systemType, orList, i);
      stringBuilder.append(filter.filterClause());
      params.addAll(filter.params());
    }

    return new DbFilter(stringBuilder.toString(), params);
  }

  private DbFilter getOutcomeFilter(
      String tableAlias,
      SystemType systemType,
      List<ExplanationOfBenefit.RemittanceOutcome> orList,
      int index) {

    switch (systemType) {
      case NCH, DDPS -> {
        if (orList.contains(ExplanationOfBenefit.RemittanceOutcome.COMPLETE)) {
          return DbFilter.empty();
        }
        return noMatchesFilter();
      }
      case SS -> {
        var clauses = new ArrayList<String>();
        var params = new ArrayList<DbFilterParam>();

        if (orList.contains(ExplanationOfBenefit.RemittanceOutcome.COMPLETE)) {
          var paramName = String.format("completeOutcomeStatuses_%d", index);
          clauses.add(String.format("%s.claimPaidStatusCode IN :%s", tableAlias, paramName));
          params.add(new DbFilterParam(paramName, COMPLETE_SHARED_SYSTEMS_STATUS_CODES));
        }

        if (orList.contains(ExplanationOfBenefit.RemittanceOutcome.PARTIAL)) {
          var paramName = String.format("completeOutcomeStatusesForPartial_%d", index);
          clauses.add(
              String.format(
                  "(%s.claimPaidStatusCode IS NULL OR %s.claimPaidStatusCode NOT IN :%s)",
                  tableAlias, tableAlias, paramName));
          params.add(new DbFilterParam(paramName, COMPLETE_SHARED_SYSTEMS_STATUS_CODES));
        }

        if (clauses.isEmpty()) {
          return noMatchesFilter();
        }

        return new DbFilter(String.format(" AND (%s)", String.join(" OR ", clauses)), params);
      }
      default -> {
        return noMatchesFilter();
      }
    }
  }

  private static DbFilter noMatchesFilter() {
    return new DbFilter(" AND FALSE", List.of());
  }

  @Override
  public boolean matchesSystemType(@NotNull SystemType systemType) {
    if (outcomes.isEmpty()) {
      return true;
    }

    var requestedOutcomes = outcomes.stream().flatMap(List::stream).toList();

    return switch (systemType) {
      case NCH, DDPS -> requestedOutcomes.contains(ExplanationOfBenefit.RemittanceOutcome.COMPLETE);
      case SS ->
          requestedOutcomes.contains(ExplanationOfBenefit.RemittanceOutcome.COMPLETE)
              || requestedOutcomes.contains(ExplanationOfBenefit.RemittanceOutcome.PARTIAL);
      default -> false;
    };
  }
}
