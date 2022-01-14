package gov.cms.bfd.server.war.r4.providers.partadj.common;

import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.annotations.VisibleForTesting;
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

  private static final String CLAIM_BY_MBI_METRIC_QUERY = "claim_by_mbi";
  private static final String CLAIM_BY_MBI_METRIC_NAME =
      MetricRegistry.name(ClaimDao.class.getSimpleName(), "query", CLAIM_BY_MBI_METRIC_QUERY);
  private static final String CLAIM_BY_ID_METRIC_QUERY = "claim_by_id";
  private static final String CLAIM_BY_ID_METRIC_NAME =
      MetricRegistry.name(ClaimDao.class.getSimpleName(), "query", CLAIM_BY_ID_METRIC_QUERY);

  private final EntityManager entityManager;
  private final MetricRegistry metricRegistry;

  public ClaimDao(EntityManager entityManager, MetricRegistry metricRegistry) {
    this.entityManager = entityManager;
    this.metricRegistry = metricRegistry;
  }

  /**
   * Gets an entity by it's ID for the given claim type.
   *
   * @param type The type of claim to retrieve.
   * @param id The id of the claim to retrieve.
   * @return An entity object of the given type provided in {@link ResourceTypeV2}
   */
  public Object getEntityById(ResourceTypeV2<?> type, String id) {
    return getEntityById(type.getEntityClass(), type.getEntityIdAttribute(), id);
  }

  /**
   * Gets an entity by it's ID for the given claim type.
   *
   * @param entityClass The type of entity to retrieve.
   * @param entityIdAttribute The name of the entity's id attribute.
   * @param id The id value of the claim to retrieve.
   * @param <T> The entity type of the claim.
   * @return The retrieved entity of the given type for the requested claim id.
   */
  @VisibleForTesting
  <T> T getEntityById(Class<T> entityClass, String entityIdAttribute, String id) {
    T claimEntity = null;

    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<T> criteria = builder.createQuery(entityClass);
    Root<T> root = criteria.from(entityClass);

    criteria.select(root);
    criteria.where(builder.equal(root.get(entityIdAttribute), id));

    Timer.Context timerClaimQuery = metricRegistry.timer(CLAIM_BY_ID_METRIC_NAME).time();
    try {
      claimEntity = entityManager.createQuery(criteria).getSingleResult();
    } finally {
      long claimByIdQueryNanoSeconds = timerClaimQuery.stop();
      TransformerUtilsV2.recordQueryInMdc(
          CLAIM_BY_ID_METRIC_QUERY, claimByIdQueryNanoSeconds, claimEntity == null ? 0 : 1);
    }

    return claimEntity;
  }

  /**
   * Find records based on a given attribute name and value with a given last updated range.
   *
   * @param entityClass The entity type to retrieve.
   * @param attributeName The name of the attribute to search on.
   * @param attributeValue The desired value of the attribute be searched on.
   * @param lastUpdated The range of lastUpdated values to search on.
   * @param serviceDate Date range of the desired service date to search on.
   * @param endDateAttributeName The name of the entity attribute denoting service end date.
   * @param <T> The entity type being retrieved.
   * @return A list of entities of type T retrieved matching the given parameters.
   */
  public <T> List<T> findAllByAttribute(
      Class<T> entityClass,
      String attributeName,
      String attributeValue,
      DateRangeParam lastUpdated,
      DateRangeParam serviceDate,
      String endDateAttributeName) {
    List<T> claimEntities = null;

    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<T> criteria = builder.createQuery(entityClass);
    Root<T> root = criteria.from(entityClass);

    criteria.select(root);
    criteria.where(
        builder.and(
            builder.equal(root.get(attributeName), attributeValue),
            lastUpdated == null
                ? builder.and()
                : createDateRangePredicate(root, lastUpdated, builder),
            serviceDate == null
                ? builder.and()
                : serviceDateRangePredicate(root, serviceDate, builder, endDateAttributeName)));

    Timer.Context timerClaimQuery = metricRegistry.timer(CLAIM_BY_MBI_METRIC_NAME).time();
    try {
      claimEntities = entityManager.createQuery(criteria).getResultList();
    } finally {
      long claimByIdQueryNanoSeconds = timerClaimQuery.stop();
      TransformerUtilsV2.recordQueryInMdc(
          CLAIM_BY_MBI_METRIC_QUERY,
          claimByIdQueryNanoSeconds,
          claimEntities == null || claimEntities.isEmpty() ? 0 : 1);
    }

    return claimEntities;
  }

  @VisibleForTesting
  Predicate createDateRangePredicate(
      Root<?> root, DateRangeParam dateRange, CriteriaBuilder builder) {
    return QueryUtils.createLastUpdatedPredicateInstant(builder, root, dateRange);
  }

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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ClaimDao claimDao = (ClaimDao) o;
    return Objects.equals(entityManager, claimDao.entityManager)
        && Objects.equals(metricRegistry, claimDao.metricRegistry);
  }

  @Override
  public int hashCode() {
    return Objects.hash(entityManager, metricRegistry);
  }
}
