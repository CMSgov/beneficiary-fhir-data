package gov.cms.bfd.server.ng.beneficiary.filter;

import gov.cms.bfd.server.ng.DbFilter;
import gov.cms.bfd.server.ng.DbFilterBuilder;
import gov.cms.bfd.server.ng.DbFilterParam;
import gov.cms.bfd.server.ng.beneficiary.model.PatientMatchParameter;
import gov.cms.bfd.server.ng.claim.model.SystemType;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Class used for building a patient match database filter.
 *
 * @param matchParams list of parameters for searching.
 */
public record PatientMatchFilter(List<PatientMatchParameter> matchParams)
    implements DbFilterBuilder {
  @NotNull
  @Override
  public DbFilter getFilters(@NotNull String tableAlias, @NotNull SystemType systemType) {
    var filterClause = new StringBuilder();
    var params = new ArrayList<DbFilterParam>();

    for (var matchParam : matchParams) {
      // JPQL path syntax needs to be converted here since DB param names can't have periods.
      var paramName = matchParam.name().replace(".", "_");
      filterClause.append(
          String.format(" AND %s.%s IN :%s", tableAlias, matchParam.name(), paramName));
      params.add(new DbFilterParam(paramName, matchParam.values()));
    }
    return new DbFilter(filterClause.toString(), params);
  }

  @Override
  public boolean matchesSystemType(@NotNull SystemType systemType) {
    return true;
  }
}
