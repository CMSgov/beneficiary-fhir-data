package gov.cms.bfd.server.ng.claim.filter;

import gov.cms.bfd.server.ng.DbFilter;
import gov.cms.bfd.server.ng.DbFilterBuilder;
import gov.cms.bfd.server.ng.DbFilterParam;
import gov.cms.bfd.server.ng.claim.model.ClaimSourceId;
import gov.cms.bfd.server.ng.claim.model.SystemType;
import gov.cms.bfd.server.ng.input.TagCriterion;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
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
  public DbFilter getFilters(@NotNull String claimTableAlias, @NotNull SystemType systemType) {
    var stringBuilder = new StringBuilder();
    var params = new ArrayList<DbFilterParam>();
    for (var i = 0; i < tagCriteria.size(); i++) {
      var orList = tagCriteria.get(i);
      if (orList.isEmpty()) {
        continue;
      }
      var clauses = new ArrayList<String>();
      for (var j = 0; j < orList.size(); j++) {
        addParam(i, j, claimTableAlias, orList.get(j), systemType, clauses, params);
      }
      if (!clauses.isEmpty()) {
        stringBuilder.append(" AND (").append(String.join(" OR ", clauses)).append(")");
      }
    }
    return new DbFilter(stringBuilder.toString(), params);
  }

  private void addParam(
      int i,
      int j,
      String claimTableAlias,
      TagCriterion criterion,
      SystemType systemType,
      ArrayList<String> clauses,
      ArrayList<DbFilterParam> params) {
    var paramName = "tag_" + i + "_" + j;
    switch (criterion) {
      case TagCriterion.SourceIdCriterion(var sourceId) -> {
        if (systemType.isCompatibleWith(sourceId)) {
          if (systemType == SystemType.SS) {
            clauses.add(claimTableAlias + ".claimSourceId = :tag_" + i + "_" + j);
            params.add(new DbFilterParam(paramName, sourceId));
          }
        } else {
          // If the system is incompatible, we set this filter to FALSE since it shouldn't match
          // anything.
          // Note that it could still be combined with an OR so we can't just ignore it.
          clauses.add("FALSE");
        }
      }
      case TagCriterion.FinalActionCriterion(var finalAction) -> {
        clauses.add(claimTableAlias + ".finalAction = :tag_" + i + "_" + j);
        params.add(new DbFilterParam(paramName, finalAction));
      }
    }
  }

  @Override
  public boolean matchesSystemType(@NotNull SystemType systemType) {
    if (tagCriteria.isEmpty()) {
      return true;
    }
    var hasFinalAction =
        tagCriteria.stream()
            .flatMap(List::stream)
            .anyMatch(TagCriterion.FinalActionCriterion.class::isInstance);
    if (hasFinalAction) {
      return true;
    }

    return tagCriteria.stream()
        .flatMap(List::stream)
        .mapMulti(this::extractSourceId)
        .anyMatch(systemType::isCompatibleWith);
  }

  private void extractSourceId(TagCriterion criterion, Consumer<ClaimSourceId> consumer) {
    if (criterion instanceof TagCriterion.SourceIdCriterion(ClaimSourceId sourceId)) {
      consumer.accept(sourceId);
    }
  }
}
