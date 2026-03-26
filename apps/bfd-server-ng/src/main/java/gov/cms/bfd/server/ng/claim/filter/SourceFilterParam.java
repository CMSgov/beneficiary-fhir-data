package gov.cms.bfd.server.ng.claim.filter;

import gov.cms.bfd.server.ng.DbFilter;
import gov.cms.bfd.server.ng.DbFilterBuilder;
import gov.cms.bfd.server.ng.DbFilterParam;
import gov.cms.bfd.server.ng.claim.model.MetaSourceSk;
import gov.cms.bfd.server.ng.claim.model.SystemType;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Filters claims by source.
 *
 * @param metaSourceSk claim source
 */
public record SourceFilterParam(List<List<MetaSourceSk>> metaSourceSk) implements DbFilterBuilder {

  @NotNull
  @Override
  public DbFilter getFilters(@NotNull String claimTableAlias, @NotNull SystemType systemType) {
    var stringBuilder = new StringBuilder();
    var params = new ArrayList<DbFilterParam>();

    for (var i = 0; i < metaSourceSk.size(); i++) {
      var orList = metaSourceSk.get(i);
      if (orList.isEmpty()) {
        continue;
      }
      var paramName = "src_" + i;
      if (systemType.isCompatibleWithAny(orList)) {
        if (systemType == SystemType.SS) {
          stringBuilder
              .append(" AND ")
              .append(claimTableAlias)
              .append(".metaSourceSk IN :")
              .append(paramName);

          params.add(new DbFilterParam(paramName, orList));
        }
      } else {
        // If the system is incompatible, we set this filter to FALSE since it shouldn't match
        // anything.
        // Note that it could still be combined with an OR so we can't just ignore it.
        stringBuilder.append(" AND FALSE");
      }
    }

    return new DbFilter(stringBuilder.toString(), params);
  }

  @Override
  public boolean matchesSystemType(@NotNull SystemType systemType) {
    if (metaSourceSk().isEmpty()) {
      return true;
    }
    return metaSourceSk.stream().flatMap(List::stream).anyMatch(systemType::isCompatibleWith);
  }
}
