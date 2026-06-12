package gov.cms.bfd.server.ng.claim.filter;

import gov.cms.bfd.server.ng.DbFilter;
import gov.cms.bfd.server.ng.DbFilterBuilder;
import gov.cms.bfd.server.ng.DbFilterParam;
import gov.cms.bfd.server.ng.claim.model.SystemType;
import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.jetbrains.annotations.NotNull;

/**
 * Filters claims by outcome.
 *
 * @param outcomes claim outcomes
 */
public record OutcomeFilterParam(List<List<ExplanationOfBenefit.RemittanceOutcome>> outcomes)
    implements DbFilterBuilder {
  private static final List<String> COMPLETE_SHARED_SYSTEMS_STATUS_CODES =
      List.of("P", "1", "R", "2", "D", "Y");

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
    if (systemType == SystemType.NCH || systemType == SystemType.DDPS) {
      if (orList.contains(ExplanationOfBenefit.RemittanceOutcome.COMPLETE)) {
        return DbFilter.empty();
      }

      return noMatchesFilter();
    }

    if (systemType != SystemType.SS) {
      return noMatchesFilter();
    }

    var clauses = new ArrayList<String>();
    var params = new ArrayList<DbFilterParam>();

    if (orList.contains(ExplanationOfBenefit.RemittanceOutcome.COMPLETE)) {
      var paramName = "completeOutcomeStatuses_" + index;
      clauses.add(tableAlias + ".claimPaidStatusCode IN :" + paramName);
      params.add(new DbFilterParam(paramName, COMPLETE_SHARED_SYSTEMS_STATUS_CODES));
    }

    if (orList.contains(ExplanationOfBenefit.RemittanceOutcome.PARTIAL)) {
      var paramName = "completeOutcomeStatusesForPartial_" + index;
      clauses.add(
          "("
              + tableAlias
              + ".claimPaidStatusCode IS NULL OR "
              + tableAlias
              + ".claimPaidStatusCode NOT IN :"
              + paramName
              + ")");
      params.add(new DbFilterParam(paramName, COMPLETE_SHARED_SYSTEMS_STATUS_CODES));
    }

    if (clauses.isEmpty()) {
      return noMatchesFilter();
    }

    return new DbFilter(" AND (" + String.join(" OR ", clauses) + ")", params);
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

    if (systemType == SystemType.NCH || systemType == SystemType.DDPS) {
      return requestedOutcomes.contains(ExplanationOfBenefit.RemittanceOutcome.COMPLETE);
    }

    return systemType == SystemType.SS
        && (requestedOutcomes.contains(ExplanationOfBenefit.RemittanceOutcome.COMPLETE)
            || requestedOutcomes.contains(ExplanationOfBenefit.RemittanceOutcome.PARTIAL));
  }
}
