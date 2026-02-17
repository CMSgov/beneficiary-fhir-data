package gov.cms.bfd.server.ng.claim.filter;

import gov.cms.bfd.server.ng.DbFilter;
import gov.cms.bfd.server.ng.DbFilterBuilder;
import gov.cms.bfd.server.ng.DbFilterParam;
import gov.cms.bfd.server.ng.claim.model.MetaSourceSk;
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
  public DbFilter getFilters(@NotNull String claimTableAlias) {
    var stringBuilder = new StringBuilder();
    var params = new ArrayList<DbFilterParam>();

    for (var i = 0; i < metaSourceSk.size(); i++) {
      var orList = metaSourceSk.get(i);
      if (orList.isEmpty()) {
        continue;
      }
      var paramName = "src_" + i;
      stringBuilder
          .append(" AND ")
          .append(claimTableAlias)
          .append(".metaSourceSk IN :")
          .append(paramName);

      params.add(new DbFilterParam(paramName, orList));
    }

    return new DbFilter(stringBuilder.toString(), params);
  }
}
