package gov.cms.bfd.server.war.r4.providers.pac.common;

import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.annotations.VisibleForTesting;
import gov.cms.bfd.model.rda.Mbi;
import gov.cms.bfd.server.war.commons.QueryUtils;
import gov.cms.bfd.server.war.r4.providers.TransformerUtilsV2;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

/** Provides common logic for performing DB interactions */
public class ClaimDao {

  static final String CLAIM_BY_MBI_QUERY = "claim_by_mbi";
  static final String CLAIM_BY_ID_QUERY = "claim_by_id";

  private final EntityManager entityManager;
  private final MetricRegistry metricRegistry;
  private final boolean isOldMbiHashEnabled;

  public ClaimDao(
      EntityManager entityManager, MetricRegistry metricRegistry, boolean isOldMbiHashEnabled) {
    this.entityManager = entityManager;
    this.metricRegistry = metricRegistry;
    this.isOldMbiHashEnabled = isOldMbiHashEnabled;
  }

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
    final String mbiRecordAttributeName = resourceType.getEntityMbiRecordAttribute();
    final String idAttributeName = resourceType.getEntityIdAttribute();
    final String endDateAttributeName = resourceType.getEntityEndDateAttribute();

    final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    final CriteriaQuery<T> criteria = builder.createQuery(entityClass);
    final Root<T> root = criteria.from(entityClass);

    criteria.select(root);
    criteria.where(
        builder.and(
            createMbiPredicate(
                root.get(mbiRecordAttributeName),
                mbiSearchValue,
                isMbiSearchValueHashed,
                isOldMbiHashEnabled,
                builder),
            lastUpdated == null
                ? builder.and()
                : createDateRangePredicate(root, lastUpdated, builder),
            serviceDate == null
                ? builder.and()
                : serviceDateRangePredicate(root, serviceDate, builder, endDateAttributeName)));
    // This sort will ensure predictable responses for any current/future testing needs
    criteria.orderBy(builder.asc(root.get(idAttributeName)));

    List<T> claimEntities = null;

    Timer.Context timerClaimQuery =
        getTimerForResourceQuery(resourceType, CLAIM_BY_MBI_QUERY).time();
    try {
      claimEntities = entityManager.createQuery(criteria).getResultList();
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
      boolean isOldMbiHashEnabled,
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
   * Helper method to create a date range predicat to make mocking easier.
   *
   * @param root The root path of the entity to get attributes from.
   * @param dateRange The date range to search for.
   * @param builder The builder to use for creating predicates.
   * @return A {@link Predicate} that checks for the given date range.
   */
  @VisibleForTesting
  Predicate createDateRangePredicate(
      Root<?> root, DateRangeParam dateRange, CriteriaBuilder builder) {
    return QueryUtils.createLastUpdatedPredicateInstant(builder, root, dateRange);
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
    Path<LocalDate> serviceDateEndPath = root.get(endDateAttributeName);

    List<Predicate> predicates = new ArrayList<>();

    DateParam lowerBound = serviceDate.getLowerBound();

    if (lowerBound != null) {
      LocalDate from = lowerBound.getValue().toInstant().atOffset(ZoneOffset.UTC).toLocalDate();

      if (ParamPrefixEnum.GREATERTHAN.equals(lowerBound.getPrefix())) {
        predicates.add(builder.greaterThan(serviceDateEndPath, from));
      } else if (ParamPrefixEnum.GREATERTHAN_OR_EQUALS.equals(lowerBound.getPrefix())) {
        predicates.add(builder.greaterThanOrEqualTo(serviceDateEndPath, from));
      } else {
        throw new IllegalArgumentException(
            String.format("Unsupported prefix supplied %s", lowerBound.getPrefix()));
      }
    }

    DateParam upperBound = serviceDate.getUpperBound();

    if (upperBound != null) {
      LocalDate to = upperBound.getValue().toInstant().atOffset(ZoneOffset.UTC).toLocalDate();

      if (ParamPrefixEnum.LESSTHAN_OR_EQUALS.equals(upperBound.getPrefix())) {
        predicates.add(builder.lessThanOrEqualTo(serviceDateEndPath, to));
      } else if (ParamPrefixEnum.LESSTHAN.equals(upperBound.getPrefix())) {
        predicates.add(builder.lessThan(serviceDateEndPath, to));
      } else {
        throw new IllegalArgumentException(
            String.format("Unsupported prefix supplied %s", upperBound.getPrefix()));
      }
    }

    return builder.and(predicates.toArray(new Predicate[0]));
  }

  /** {@inheritDoc} */
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ClaimDao claimDao = (ClaimDao) o;
    return Objects.equals(entityManager, claimDao.entityManager)
        && Objects.equals(metricRegistry, claimDao.metricRegistry);
  }

  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    return Objects.hash(entityManager, metricRegistry);
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
