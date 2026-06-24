package gov.cms.bfd.server.ng.claim;

import static gov.cms.bfd.server.ng.util.MetricRecorder.CLAIM_TYPE;

import gov.cms.bfd.server.ng.DbFilter;
import gov.cms.bfd.server.ng.DbFilterBuilder;
import gov.cms.bfd.server.ng.DbFilterParam;
import gov.cms.bfd.server.ng.claim.model.*;
import gov.cms.bfd.server.ng.input.ClaimSearchCriteria;
import gov.cms.bfd.server.ng.log.QueryTelemetryUtil;
import gov.cms.bfd.server.ng.model.QueryProfile;
import gov.cms.bfd.server.ng.util.LogUtil;
import gov.cms.bfd.server.ng.util.MetricRecorder;
import io.micrometer.core.instrument.Tags;
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

  @Async
  @SuppressWarnings("java:S2077")
  protected <C extends ClaimBase, B extends DbFilterBuilder>
      CompletableFuture<List<C>> findByIdsInClaimType(
          String baseQuery,
          Class<C> claimClass,
          SystemType systemType,
          List<Long> claimUniqueIds,
          List<B> filterBuilders,
          QueryProfile queryProfile) {

    var filters = getFilters(filterBuilders, systemType);
    var whereClause = buildWhereClause(filters, systemType);
    var conditions =
        String.format(
            """
            WHERE c.claimUniqueId IN :claimUniqueIds
            %s
            """,
            whereClause);
    var jpql = buildProfileJpql(baseQuery, claimClass, queryProfile, conditions);

    return metricRecorder.recordMetricAsync(
        "application.claim.search_by_ids_in_claim_type",
        () -> Tags.of(CLAIM_TYPE, claimClass.getSimpleName()),
        () -> {
          try (var entityManager = entityManagerFactory.createEntityManager()) {
            var query =
                DbFilterParam.withParams(
                        entityManager.createQuery(jpql, claimClass), filters.params())
                    .setParameter("claimUniqueIds", claimUniqueIds);
            var result = queryTelemetryUtil.executeAndTrack("findByIdsInClaimType", query);
            result.stream()
                .findFirst()
                .ifPresent(claim -> LogUtil.logBeneSk(claim.getBeneficiary().getBeneSk()));
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
    var conditions =
        String.format(
            """
             WHERE b.xrefSk = :beneSk
             %s
            """,
            whereClause);
    var jpql = buildProfileJpql(baseQuery, claimClass, criteria.queryProfile(), conditions);
    try (var entityManager = entityManagerFactory.createEntityManager()) {
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
            result.stream()
                .findFirst()
                .ifPresent(claim -> LogUtil.logBeneSk(claim.getBeneficiary().getBeneSk()));
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

  private String buildProfileJpql(
      String baseQuery, Class<?> claimClass, QueryProfile profile, String conditions) {
    if (claimClass == ClaimRx.class) {
      String selectClause =
          switch (profile) {
            case BASIS ->
                """
                SELECT new gov.cms.bfd.server.ng.claim.model.ClaimRx(
                    c.claimUniqueId,
                    c.claimTypeCode,
                    c.claimEffectiveDate,
                    c.finalAction,
                    c.latestClaimIndicator,
                    null,
                    c.meta,
                    c.identifiers,
                    c.billablePeriod,
                    c.claimIDRLoadDate,
                    b,
                    null,
                    c.contractName,
                    c.pricingCode,
                    c.serviceProviderHistory,
                    c.prescribingProviderHistory,
                    null,
                    c.claimPaymentDate,
                    c.submitterContractNumber,
                    c.submitterContractPBPNumber,
                    c.claimSubmissionDate,
                    null,
                    new gov.cms.bfd.server.ng.claim.model.ClaimItemRx(
                        new gov.cms.bfd.server.ng.claim.model.ClaimLineRx(
                            c.claimItems.claimLine.fromDate,
                            c.claimItems.claimLine.ndc,
                            c.claimItems.claimLine.serviceUnitQuantity,
                            c.claimItems.claimLine.claimRxSupportingInfo
                        ),
                        c.claimItems.claimLineRxNum
                    )
                )
                """;
            case REGULAR ->
                """
                SELECT new gov.cms.bfd.server.ng.claim.model.ClaimRx(
                    c.claimUniqueId,
                    c.claimTypeCode,
                    c.claimEffectiveDate,
                    c.finalAction,
                    c.latestClaimIndicator,
                    null,
                    c.meta,
                    c.identifiers,
                    c.billablePeriod,
                    c.claimIDRLoadDate,
                    b,
                    c.claimFormatCode,
                    c.contractName,
                    c.pricingCode,
                    c.serviceProviderHistory,
                    c.prescribingProviderHistory,
                    c.adjudicationCharge,
                    c.claimPaymentDate,
                    c.submitterContractNumber,
                    c.submitterContractPBPNumber,
                    c.claimSubmissionDate,
                    null,
                    new gov.cms.bfd.server.ng.claim.model.ClaimItemRx(
                        new gov.cms.bfd.server.ng.claim.model.ClaimLineRx(
                            c.claimItems.claimLine.fromDate,
                            c.claimItems.claimLine.ndc,
                            c.claimItems.claimLine.serviceUnitQuantity,
                            c.claimItems.claimLine.adjudicationCharge,
                            c.claimItems.claimLine.claimRxSupportingInfo
                        ),
                        c.claimItems.claimLineRxNum
                    )
                )
                """;
            case CMS ->
                """
                SELECT new gov.cms.bfd.server.ng.claim.model.ClaimRx(
                    c.claimUniqueId,
                    c.claimTypeCode,
                    c.claimEffectiveDate,
                    c.finalAction,
                    c.latestClaimIndicator,
                    c.claimAdjustmentTypeCode,
                    c.meta,
                    c.identifiers,
                    c.billablePeriod,
                    c.claimIDRLoadDate,
                    b,
                    c.claimFormatCode,
                    c.contractName,
                    c.pricingCode,
                    c.serviceProviderHistory,
                    c.prescribingProviderHistory,
                    c.adjudicationCharge,
                    c.claimPaymentDate,
                    c.submitterContractNumber,
                    c.submitterContractPBPNumber,
                    c.claimSubmissionDate,
                    c.claimProcessDate,
                    new gov.cms.bfd.server.ng.claim.model.ClaimItemRx(
                        new gov.cms.bfd.server.ng.claim.model.ClaimLineRx(
                            c.claimItems.claimLine.fromDate,
                            c.claimItems.claimLine.ndc,
                            c.claimItems.claimLine.serviceUnitQuantity,
                            c.claimItems.claimLine.adjudicationCharge,
                            c.claimItems.claimLine.claimRxSupportingInfo
                        ),
                        c.claimItems.claimLineRxNum
                    )
                )
                """;
          };
      String queryWithoutSelect =
          baseQuery
              .replaceFirst("(?i)SELECT\\s+c\\s+", "")
              .replace("JOIN FETCH", "JOIN")
              .replace("LEFT JOIN FETCH", "LEFT JOIN");
      return String.format(
          """
          %s
          %s
          %s
          """,
          selectClause, queryWithoutSelect, conditions);
    } else {
      return String.format(
          """
          %s
          %s
          """,
          baseQuery, conditions);
    }
  }
}
