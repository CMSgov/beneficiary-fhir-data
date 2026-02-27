package gov.cms.bfd.server.ng.claim;

import gov.cms.bfd.server.ng.DbFilter;
import gov.cms.bfd.server.ng.DbFilterBuilder;
import gov.cms.bfd.server.ng.DbFilterParam;
import gov.cms.bfd.server.ng.claim.model.*;
import gov.cms.bfd.server.ng.input.ClaimSearchCriteria;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

/** Repository methods for claims. */
@Repository
@AllArgsConstructor
public class ClaimAsyncService {

  @PersistenceContext private final EntityManager entityManager;

  @Async
  protected <C extends ClaimBase, B extends DbFilterBuilder>
      CompletableFuture<Optional<C>> findByIdInClaimType(
          String baseQuery,
          Class<C> claimClass,
          SystemType systemType,
          long claimUniqueId,
          List<B> paramBuilders) {

    var filters = getFilters(paramBuilders, systemType);
    var jpql =
        String.format(
            """
            %s
            WHERE c.claimUniqueId = :claimUniqueId
            %s
            """,
            baseQuery, filters.filterClause());

    var result =
        withParams(entityManager.createQuery(jpql, claimClass), filters.params())
            .setParameter("claimUniqueId", claimUniqueId)
            .getResultList()
            .stream()
            .findFirst();

    return CompletableFuture.completedFuture(result);
  }

  @Async
  protected <T extends ClaimBase> CompletableFuture<List<T>> fetchClaims(
      String baseQuery,
      Class<T> claimClass,
      SystemType systemType,
      ClaimSearchCriteria criteria,
      List<DbFilterBuilder> filterBuilders) {

    var filters = getFilters(filterBuilders, systemType);

    var jpql =
        String.format(
            """
            %s
            WHERE b.xrefSk = :beneSk
            %s
            """,
            baseQuery, filters.filterClause());

    var result =
        withParams(entityManager.createQuery(jpql, claimClass), filters.params())
            .setParameter("beneSk", criteria.beneSk())
            .getResultList();

    return CompletableFuture.completedFuture(result);
  }

  <T extends DbFilterBuilder> DbFilter getFilters(List<T> builders, SystemType systemType) {
    var sb = new StringBuilder();
    var queryParams = new ArrayList<DbFilterParam>();
    for (var builder : builders) {
      var params = builder.getFilters("c", systemType);
      sb.append(params.filterClause());
      queryParams.addAll(params.params());
    }
    return new DbFilter(sb.toString(), queryParams);
  }

  private <T> TypedQuery<T> withParams(TypedQuery<T> query, List<DbFilterParam> params) {
    for (var param : params) {
      query.setParameter(param.name(), param.value());
    }
    return query;
  }
}
