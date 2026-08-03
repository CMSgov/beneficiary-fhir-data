package gov.cms.bfd.server.ng.claim;

import static gov.cms.bfd.server.ng.util.LogUtil.logUniqueBeneficiaries;
import static gov.cms.bfd.server.ng.util.MetricRecorder.CLAIM_TYPE;

import gov.cms.bfd.server.ng.DbFilter;
import gov.cms.bfd.server.ng.DbFilterBuilder;
import gov.cms.bfd.server.ng.DbFilterParam;
import gov.cms.bfd.server.ng.claim.model.*;
import gov.cms.bfd.server.ng.input.ClaimSearchCriteria;
import gov.cms.bfd.server.ng.log.QueryTelemetryUtil;
import gov.cms.bfd.server.ng.util.MetricRecorder;
import io.micrometer.core.instrument.Tags;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

/**
 * Repository methods for claims. Suppress SonarQube about dynamically formatted SQL queries being
 * safe here. Ignore. These are internally generated.
 */
@Repository
@AllArgsConstructor
@SuppressWarnings("java:S2077")
public class ClaimAsyncService {

  private final EntityManagerFactory entityManagerFactory;
  private final MetricRecorder metricRecorder;
  private final QueryTelemetryUtil queryTelemetryUtil;
  private final PriorAuthorizationRepository priorAuthorizationRepository;

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

    return metricRecorder.recordMetricAsync(
        "application.claim.search_by_ids_in_claim_type",
        () -> Tags.of(CLAIM_TYPE, claimClass.getSimpleName()),
        () -> {
          try (var entityManager = readonly(entityManagerFactory.createEntityManager())) {
            var query =
                DbFilterParam.withParams(
                        entityManager.createQuery(jpql, claimClass), filters.params())
                    .setParameter("claimUniqueIds", claimUniqueIds);
            var result = queryTelemetryUtil.executeAndTrack("findByIdsInClaimType", query);
            logUniqueBeneficiaries(result);
            return CompletableFuture.completedFuture(result);
          }
        });
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
    try (var entityManager = readonly(entityManagerFactory.createEntityManager())) {
      return metricRecorder.recordMetricAsync(
          "application.claim.fetch_claims_with_claim_type",
          () -> Tags.of(CLAIM_TYPE, claimClass.getSimpleName()),
          () -> {
            var query =
                DbFilterParam.withParams(
                        entityManager.createQuery(jpql, claimClass), filters.params())
                    .setParameter("beneSk", criteria.beneSk());
            var result =
                queryTelemetryUtil.executeAndTrack(
                    "fetchClaims_" + claimClass.getSimpleName(), query);
            logUniqueBeneficiaries(result);
            return CompletableFuture.completedFuture(result);
          });
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

  @Async
  protected CompletableFuture<List<PriorAuthorization>> fetchPriorAuth(String mbi) {
    return CompletableFuture.completedFuture(
        priorAuthorizationRepository.getPriorAuthorizationFromMbi(mbi));
  }

  private EntityManager readonly(EntityManager entityManager) {
    entityManager.unwrap(org.hibernate.Session.class).setDefaultReadOnly(true);
    return entityManager;
  }
}
