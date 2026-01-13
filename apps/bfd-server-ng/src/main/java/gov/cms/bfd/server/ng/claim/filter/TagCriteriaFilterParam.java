package gov.cms.bfd.server.ng.claim.filter;

import gov.cms.bfd.server.ng.DbFilter;
import gov.cms.bfd.server.ng.DbFilterBuilder;
import gov.cms.bfd.server.ng.DbFilterParam;
import gov.cms.bfd.server.ng.input.TagCriterion;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Filter for tags.
 *
 * @param tagCriteria tags
 */
public record TagCriteriaFilterParam(List<List<TagCriterion>> tagCriteria)
    implements DbFilterBuilder {

  @NotNull
  @Override
  public DbFilter getFilters(@NotNull String claimTableAlias) {
    var stringBuilder = new StringBuilder();
    var params = new ArrayList<DbFilterParam>();
    for (var i = 0; i < tagCriteria.size(); i++) {
      var orList = tagCriteria.get(i);
      if (orList.isEmpty()) {
        continue;
      }
      var clauses = new ArrayList<String>();
      for (var j = 0; j < orList.size(); j++) {
        var criterion = orList.get(j);
        var paramName = "tag_" + i + "_" + j;
        switch (criterion) {
          case TagCriterion.SourceIdCriterion(var sourceId) -> {
            clauses.add(claimTableAlias + ".claimSourceId = :tag_" + i + "_" + j);
            params.add(new DbFilterParam(paramName, sourceId));
          }
          case TagCriterion.FinalActionCriterion(var finalAction) -> {
            clauses.add(claimTableAlias + ".finalAction = :tag_" + i + "_" + j);
            params.add(new DbFilterParam(paramName, finalAction));
          }
        }
      }
      stringBuilder.append(" AND (").append(String.join(" OR ", clauses)).append(")");
    }
    return new DbFilter(stringBuilder.toString(), params);
  }
}
