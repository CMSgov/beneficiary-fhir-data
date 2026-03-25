package gov.cms.bfd.server.ng.beneficiary.filter;

import gov.cms.bfd.server.ng.DbFilter;
import gov.cms.bfd.server.ng.DbFilterBuilder;
import gov.cms.bfd.server.ng.DbFilterParam;
import gov.cms.bfd.server.ng.beneficiary.model.PatientMatchEntry;
import gov.cms.bfd.server.ng.claim.model.SystemType;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public record PatientMatchFilter(List<PatientMatchEntry> entries) implements DbFilterBuilder {
  @NotNull
  @Override
  public DbFilter getFilters(@NotNull String tableAlias, @NotNull SystemType systemType) {
    var filterClause = new StringBuilder();
    var params = new ArrayList<DbFilterParam>();
    for (var entry : entries) {

      entry
          .value()
          .ifPresent(
              v -> {
                var paramName = entry.name().replace(".", "_");
                filterClause.append(
                    String.format(" AND %s.%s = :%s", tableAlias, entry.name(), paramName));
                params.add(new DbFilterParam(paramName, v));
              });
    }
    return new DbFilter(filterClause.toString(), params);
  }

  @Override
  public boolean matchesSystemType(@NotNull SystemType systemType) {
    return true;
  }
}
