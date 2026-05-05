package gov.cms.bfd.server.ng.claim;

import gov.cms.bfd.server.ng.DbFilter;
import gov.cms.bfd.server.ng.DbFilterBuilder;
import gov.cms.bfd.server.ng.DbFilterParam;
import gov.cms.bfd.server.ng.claim.model.*;
import gov.cms.bfd.server.ng.input.ClaimSearchCriteria;
import gov.cms.bfd.server.ng.util.LogUtil;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
  private final MeterRegistry meterRegistry;

  @Async
  @SuppressWarnings("java:S2077")
  protected <C extends ClaimBase, B extends DbFilterBuilder>
      CompletableFuture<List<C>> findByIdsInClaimType(
          String baseQuery,
          Class<C> claimClass,
          SystemType systemType,
          List<Long> claimUniqueIds,
          List<B> filterBuilders) {

    var filters = getFilters(filterBuilders, systemType);
    var whereClause = buildWhereClause(filters, systemType);
    var jpql =
        String.format(
            """
            %s
            WHERE c.claimUniqueId IN :claimUniqueIds
            %s
            """,
            baseQuery, whereClause);

    var timer = Timer.start(meterRegistry);

    try {
      var result =
          DbFilterParam.withParams(entityManager.createQuery(jpql, claimClass), filters.params())
              .setParameter("claimUniqueIds", claimUniqueIds)
              .getResultList();
      result.stream()
          .findFirst()
          .ifPresent(claim -> LogUtil.logBeneSk(claim.getBeneficiary().getBeneSk()));
      return CompletableFuture.completedFuture(result);
    } finally {
      timer.stop(
          Timer.builder("application.claim.search_by_ids_in_claim_type")
              .tag("claim_type", claimClass.getSimpleName())
              .register(meterRegistry));
    }
  }

  @Async
  protected <T extends ClaimBase> CompletableFuture<List<T>> fetchClaims(
      String baseQuery,
      Class<T> claimClass,
      SystemType systemType,
      ClaimSearchCriteria criteria,
      List<DbFilterBuilder> filterBuilders) {

    var filters = getFilters(filterBuilders, systemType);
    var whereClause = buildWhereClause(filters, systemType);
    var jpql =
        String.format(
            """
             %s
             WHERE b.xrefSk = :beneSk
             %s
            """,
            baseQuery, whereClause);

    var timer = Timer.start(meterRegistry);

    try {
      var result =
          DbFilterParam.withParams(entityManager.createQuery(jpql, claimClass), filters.params())
              .setParameter("beneSk", criteria.beneSk())
              .getResultList();
      result.stream()
          .findFirst()
          .ifPresent(claim -> LogUtil.logBeneSk(claim.getBeneficiary().getBeneSk()));
      return CompletableFuture.completedFuture(result);
    } finally {
      timer.stop(
          Timer.builder("application.claim.fetch_claims_with_claim_type")
              .tag("claim_type", claimClass.getSimpleName())
              .register(meterRegistry));
    }
  }

  private String buildWhereClause(DbFilter filter, SystemType systemType) {
    var latestClaimFilter =
        systemType.filterLatestClaims() ? "AND c.latestClaimIndicator = 'Y'" : "";
    return String.format(
        """
        AND b.latestTransactionFlag = 'Y'
        %s
        %s
        """,
        filter.filterClause(), latestClaimFilter);
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
}
