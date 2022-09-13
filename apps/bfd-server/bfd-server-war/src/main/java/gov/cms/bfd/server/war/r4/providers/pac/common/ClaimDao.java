package gov.cms.bfd.server.war.r4.providers.pac.common;

import ca.uhn.fhir.rest.param.DateRangeParam;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.annotations.VisibleForTesting;
import gov.cms.bfd.model.rda.Mbi;
import gov.cms.bfd.server.war.commons.QueryUtils;
import gov.cms.bfd.server.war.r4.providers.TransformerUtilsV2;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

/** Provides common logic for performing DB interactions */
@EqualsAndHashCode
@AllArgsConstructor
public class ClaimDao {

  /** Query name for logging MDC */
  static final String CLAIM_BY_MBI_QUERY = "claim_by_mbi";
  /** Query name for logging MDC */
  static final String CLAIM_BY_ID_QUERY = "claim_by_id";

  /** {@link EntityManager} used for database access. */
  private final EntityManager entityManager;
  /** {@link MetricRegistry} for metrics. */
  private final MetricRegistry metricRegistry;
  /** Whether or not to use old MBI hash functionality. */
  private final boolean isOldMbiHashEnabled;

  /**
   * Gets an entity by it's ID for the given claim type.
   *
   * @param resourceType The type of claim to retrieve.
   * @param id The id of the claim to retrieve.
   * @param <T> The entity type being retrieved.
   * @return An entity object of the given type provided in {@link ResourceTypeV2}
   */
  public <T> T getEntityById(ResourceTypeV2<?, T> resourceType, String id) {
    final Class<T> entityClass = resourceType.getEntityClass();
    final String entityIdAttribute = resourceType.getEntityIdAttribute();

    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<T> criteria = builder.createQuery(entityClass);
    Root<T> root = criteria.from(entityClass);

    criteria.select(root);
    criteria.where(builder.equal(root.get(entityIdAttribute), id));

    T claimEntity = null;

    Timer.Context timerClaimQuery =
        getTimerForResourceQuery(resourceType, CLAIM_BY_ID_QUERY).time();
    try {
      claimEntity = entityManager.createQuery(criteria).getSingleResult();
    } finally {
      logQueryMetric(
          resourceType, CLAIM_BY_ID_QUERY, timerClaimQuery.stop(), claimEntity == null ? 0 : 1);
    }

    return claimEntity;
  }

  /**
   * Find records by MBI (hashed or unhashed) for a given {@link ResourceTypeV2} using search value
   * plus optional last updated and service date ranges.
   *
   * @param resourceType The {@link ResourceTypeV2} that defines properties required for the query.
   * @param mbiSearchValue The desired value of the mbi attribute be searched on.
   * @param isMbiSearchValueHashed True if the mbiSearchValue is a hashed MBI.
   * @param lastUpdated The range of lastUpdated values to search on.
   * @param serviceDate The range of the desired service date to search on.
   * @param <T> The entity type being retrieved.
   * @return A list of entities of type T retrieved matching the given parameters.
   */
  public <T> List<T> findAllByMbiAttribute(
      ResourceTypeV2<?, T> resourceType,
      String mbiSearchValue,
      boolean isMbiSearchValueHashed,
      DateRangeParam lastUpdated,
      DateRangeParam serviceDate) {
    final Class<T> entityClass = resourceType.getEntityClass();
    final String idAttributeName = resourceType.getEntityIdAttribute();
    final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    final CriteriaQuery<T> claimsQuery = builder.createQuery(entityClass);
    final List<Predicate> predicates = new ArrayList<>();

    final Root<T> claim = claimsQuery.from(entityClass);

    claimsQuery.select(claim);

    if (resourceType.getServiceDateSubquerySpec().isPresent() && isDateRangePresent(serviceDate)) {
      // This is a very specific case that uses a sub-query as its only where clause predicate.
      // In this case all of the conditions are handled in the sub-query.
      predicates.add(
          createSubqueryPredicateForMbiLookup(
              builder,
              claimsQuery,
              claim,
              resourceType,
              resourceType.getServiceDateSubquerySpec().get(),
              mbiSearchValue,
              isMbiSearchValueHashed,
              lastUpdated,
              serviceDate));
    } else {
      // Normal case where we do a simple query with all the conditions in its where clause.
      predicates.addAll(
          createStandardPredicatesForMbiLookup(
              builder,
              claim,
              resourceType,
              mbiSearchValue,
              isMbiSearchValueHashed,
              lastUpdated,
              serviceDate));
    }

    claimsQuery.where(predicates.toArray(new Predicate[0]));

    // This sort will ensure predictable responses for any current/future testing needs
    claimsQuery.orderBy(builder.asc(claim.get(idAttributeName)));

    List<T> claimEntities = null;

    Timer.Context timerClaimQuery =
        getTimerForResourceQuery(resourceType, CLAIM_BY_MBI_QUERY).time();
    try {
      claimEntities = entityManager.createQuery(claimsQuery).getResultList();
    } finally {
      logQueryMetric(
          resourceType,
          CLAIM_BY_MBI_QUERY,
          timerClaimQuery.stop(),
          claimEntities == null ? 0 : claimEntities.size());
    }

    return claimEntities;
  }

  /**
   * Builds a list of predicates for standard MBI and date range restrictions on search. Used for
   * FISS claim lookup and for MCS claim lookup when no service date restriction is in place.
   *
   * @param builder The {@link CriteriaBuilder} being used in current query.
   * @param claim The {@link Root} for the claim in the query
   * @param resourceType The {@link ResourceTypeV2} that defines properties required for the query.
   * @param mbiSearchValue The desired value of the mbi attribute be searched on.
   * @param isMbiSearchValueHashed True if the mbiSearchValue is a hashed MBI.
   * @param lastUpdated The range of lastUpdated values to search on.
   * @param serviceDate The range of the desired service date to search on.
   * @return a {@link Predicate} to be used in query where clause
   * @param <T> The entity type being retrieved.
   */
  @VisibleForTesting
  <T> List<Predicate> createStandardPredicatesForMbiLookup(
      CriteriaBuilder builder,
      Root<T> claim,
      ResourceTypeV2<?, T> resourceType,
      String mbiSearchValue,
      boolean isMbiSearchValueHashed,
      DateRangeParam lastUpdated,
      DateRangeParam serviceDate) {
    final String mbiRecordAttributeName = resourceType.getEntityMbiRecordAttribute();
    final String endDateAttributeName = resourceType.getEntityEndDateAttribute();
    final List<Predicate> predicates = new ArrayList<>();
    final Path<Object> mbiRecord = claim.get(mbiRecordAttributeName);
    predicates.add(createMbiPredicate(mbiRecord, mbiSearchValue, isMbiSearchValueHashed, builder));
    if (isDateRangePresent(lastUpdated)) {
      predicates.add(lastUpdatedDateRangePredicate(claim, lastUpdated, builder));
    }
    if (isDateRangePresent(serviceDate)) {
      predicates.add(serviceDateRangePredicate(claim, serviceDate, builder, endDateAttributeName));
    }
    return predicates;
  }

  /**
   * Shorthand for checking that a date range parameter has been populated by our caller.
   *
   * @param dateRangeParam the param to check
   * @return true if the param contains a date range
   */
  private boolean isDateRangePresent(DateRangeParam dateRangeParam) {
    return dateRangeParam != null && !dateRangeParam.isEmpty();
  }

  /**
   * Builds a predicate containing an {@code in} clause for the given search criteria. The subquery
   * returns a set of claim ids with correct MBI, lastUpdated, and service date.
   *
   * <p>This example illustrates the type of claim returned for MCS.
   *
   * <pre>
   * where c.idr_clm_hd_icn in (
   *   select mc.idr_clm_hd_icn
   *       from rda.mcs_claims mc
   *       join rda.mbi_cache mbi on mbi.mbi_id = mc.mbi_id
   *       left join rda.mcs_details md on md.idr_clm_hd_icn = mc.idr_clm_hd_icn
   *       where mbi.hash = :'mbi_hash'
   *         and mc.last_updated > :'min_date'
   *       group by mc.idr_clm_hd_icn, mc.idr_hdr_to_date_of_svc
   *       having ((max(md.idr_dtl_to_date) is not null) and (max(md.idr_dtl_to_date) >= :'min_date'))
   *              or ((mc.idr_hdr_to_date_of_svc is not null) and (mc.idr_hdr_to_date_of_svc >= :'min_date')));
   * </pre>
   *
   * @param builder The {@link CriteriaBuilder} being used in current query.
   * @param claimsQuery The {@link CriteriaQuery} for the outer query that will use this predicate
   * @param outerClaim The {@link Root} for the claim in the outer query
   * @param resourceType The {@link ResourceTypeV2} that defines properties required for the query.
   * @param subquerySpec The {@link
   *     gov.cms.bfd.server.war.r4.providers.pac.common.ResourceTypeV2.ServiceDateSubquerySpec}
   *     defining the subquery
   * @param mbiSearchValue The desired value of the mbi attribute be searched on.
   * @param isMbiSearchValueHashed True if the mbiSearchValue is a hashed MBI.
   * @param lastUpdated The range of lastUpdated values to search on.
   * @param serviceDate The range of the desired service date to search on.
   * @return a {@link Predicate} to be used in outer query where clause
   * @param <T> The entity type being retrieved.
   */
  @VisibleForTesting
  <T> Predicate createSubqueryPredicateForMbiLookup(
      CriteriaBuilder builder,
      CriteriaQuery<T> claimsQuery,
      Root<T> outerClaim,
      ResourceTypeV2<?, T> resourceType,
      ResourceTypeV2.ServiceDateSubquerySpec subquerySpec,
      String mbiSearchValue,
      boolean isMbiSearchValueHashed,
      DateRangeParam lastUpdated,
      DateRangeParam serviceDate) {
    final List<Predicate> predicates = new ArrayList<>();
    final Subquery<String> claimIdsQuery = claimsQuery.subquery(String.class);
    final Root<?> innerClaim = claimIdsQuery.from(resourceType.getEntityClass());
    final Join<?, ?> innerDetails =
        innerClaim.join(subquerySpec.getDetailJoinAttribute(), JoinType.LEFT);
    final Path<?> innerClaimMbiRecord = innerClaim.get(resourceType.getEntityMbiRecordAttribute());
    final Path<String> innerClaimId = innerClaim.get(subquerySpec.getClaimIdAttribute());
    final Path<LocalDate> innerClaimDate = innerClaim.get(resourceType.getEntityEndDateAttribute());
    final Path<LocalDate> innerDetailsDate =
        innerDetails.<LocalDate>get(subquerySpec.getDateAttribute());
    final Path<Object> outerClaimId = outerClaim.get(resourceType.getEntityIdAttribute());

    claimIdsQuery.select(innerClaimId);
    predicates.add(
        createMbiPredicate(innerClaimMbiRecord, mbiSearchValue, isMbiSearchValueHashed, builder));
    if (isDateRangePresent(lastUpdated)) {
      predicates.add(lastUpdatedDateRangePredicate(innerClaim, lastUpdated, builder));
    }
    claimIdsQuery.where(predicates.toArray(new Predicate[0]));
    claimIdsQuery.groupBy(innerClaimId, innerClaimDate);
    claimIdsQuery.having(
        builder.or(
            serviceDateRangePredicate(builder, serviceDate, innerClaimDate),
            serviceDateRangePredicate(builder, serviceDate, builder.greatest(innerDetailsDate))));
    return outerClaimId.in(claimIdsQuery);
  }

  /**
   * Helper method for easier mocking related to metrics.
   *
   * @param queryTime The amount of time passed executing the query.
   * @param querySize The number of entities returned by the query.
   */
  @VisibleForTesting
  void logQueryMetric(
      ResourceTypeV2<?, ?> resourceType, String queryName, long queryTime, int querySize) {
    final String combinedQueryId =
        String.format("%s_%s", queryName, resourceType.getNameForMetrics());
    TransformerUtilsV2.recordQueryInMdc(combinedQueryId, queryTime, querySize);
  }

  /**
   * Helper method to create the appropriate MBI predicate depending on if the current or old MBI
   * Hash should be used.
   *
   * @param root The root path of the entity to get attributes from.
   * @param mbiSearchValue The MBI value being searched for.
   * @param isMbiSearchValueHashed Indicates if the search value is a hash or raw MBI.
   * @param isOldMbiHashEnabled Indicates if the old MBI should be checked for the query.
   * @param builder The builder to use for creating predicates.
   * @return A {@link Predicate} that checks for the given mbi value.
   */
  @VisibleForTesting
  Predicate createMbiPredicate(
      Path<?> root,
      String mbiSearchValue,
      boolean isMbiSearchValueHashed,
      CriteriaBuilder builder) {
    final String mbiValueAttributeName = isMbiSearchValueHashed ? Mbi.Fields.hash : Mbi.Fields.mbi;
    var answer = builder.equal(root.get(mbiValueAttributeName), mbiSearchValue);
    if (isMbiSearchValueHashed && isOldMbiHashEnabled) {
      var oldHashPredicate = builder.equal(root.get(Mbi.Fields.oldHash), mbiSearchValue);
      answer = builder.or(answer, oldHashPredicate);
    }
    return answer;
  }

  /**
   * Helper method to create a date range predicate to make mocking easier.
   *
   * @param root The root path of the entity to get attributes from.
   * @param dateRange The date range to search for.
   * @param builder The builder to use for creating predicates.
   * @return A {@link Predicate} that checks for the given date range.
   */
  @VisibleForTesting
  Predicate lastUpdatedDateRangePredicate(
      Root<?> root, DateRangeParam dateRange, CriteriaBuilder builder) {
    return QueryUtils.createLastUpdatedPredicate(builder, root, dateRange);
  }

  /**
   * Helper method to create a service date range predicate.
   *
   * @param root The root path of the entity to get attributes from.
   * @param serviceDate The service date to search for.
   * @param builder The builder to use for creating predicates.
   * @param endDateExpression An expression defining the date value to test.
   * @return A {@link Predicate} that checks for the given service date range.
   */
  @VisibleForTesting
  Predicate serviceDateRangePredicate(
      CriteriaBuilder builder, DateRangeParam dateRange, Expression<LocalDate> dateExpression) {
    return QueryUtils.createDateRangePredicate(builder, dateRange, dateExpression);
  }

  /**
   * Helper method to create a service date range predicate.
   *
   * @param root The root path of the entity to get attributes from.
   * @param serviceDate The service date to search for.
   * @param builder The builder to use for creating predicates.
   * @param endDateAttributeName The name of the end date attribute on the entity.
   * @return A {@link Predicate} that checks for the given service date range.
   */
  @VisibleForTesting
  Predicate serviceDateRangePredicate(
      Root<?> root,
      DateRangeParam serviceDate,
      CriteriaBuilder builder,
      String endDateAttributeName) {
    return QueryUtils.createDateRangePredicate(
        builder, serviceDate, root.get(endDateAttributeName));
  }

  /**
   * Obtains a {@link Timer} metric to time a query on a given {@link ResourceTypeV2}.
   *
   * @param resourceType The type of claim being retrieved.
   * @param queryName used to specify the particular query in the metric name
   * @return A drop wizard {@link Timer}
   */
  private Timer getTimerForResourceQuery(ResourceTypeV2<?, ?> resourceType, String queryName) {
    return metricRegistry.timer(createMetricNameForResourceQuery(resourceType, queryName));
  }

  /**
   * Creates a metric name for a query on a given {@link ResourceTypeV2}.
   *
   * @param resourceType The type of claim being retrieved.
   * @param queryName used to specify the particular query in the metric name
   * @return A valid drop wizard metric name
   */
  @VisibleForTesting
  static String createMetricNameForResourceQuery(
      ResourceTypeV2<?, ?> resourceType, String queryName) {
    return MetricRegistry.name(
        ClaimDao.class.getSimpleName(), "query", queryName, resourceType.getNameForMetrics());
  }
}
