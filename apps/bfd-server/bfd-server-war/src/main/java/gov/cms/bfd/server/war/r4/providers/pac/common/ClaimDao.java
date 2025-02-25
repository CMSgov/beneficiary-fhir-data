package gov.cms.bfd.server.war.r4.providers.pac.common;

import ca.uhn.fhir.rest.param.DateRangeParam;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.annotations.VisibleForTesting;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.model.rda.Mbi;
import gov.cms.bfd.model.rda.entities.RdaFissClaim;
import gov.cms.bfd.model.rda.entities.RdaMcsClaim;
import gov.cms.bfd.model.rif.entities.CarrierClaim;
import gov.cms.bfd.model.rif.entities.DMEClaim;
import gov.cms.bfd.model.rif.entities.HHAClaim;
import gov.cms.bfd.model.rif.entities.HospiceClaim;
import gov.cms.bfd.model.rif.entities.InpatientClaim;
import gov.cms.bfd.model.rif.entities.OutpatientClaim;
import gov.cms.bfd.model.rif.entities.SNFClaim;
import gov.cms.bfd.server.war.commons.CommonTransformerUtils;
import gov.cms.bfd.server.war.commons.QueryUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

/** Provides common logic for performing DB interactions. */
@EqualsAndHashCode
@AllArgsConstructor
public class ClaimDao {

  /** Query name for logging MDC. */
  static final String CLAIM_BY_MBI_QUERY = "claim_by_mbi";

  /** Query name for logging MDC. */
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
  @Trace
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
  @Trace
  public <T> List<T> findAllByMbiAttribute(
      ResourceTypeV2<?, T> resourceType,
      String mbiSearchValue,
      boolean isMbiSearchValueHashed,
      DateRangeParam lastUpdated,
      DateRangeParam serviceDate) {
    final Class<T> entityClass = resourceType.getEntityClass();
    final String idAttributeName = resourceType.getEntityIdAttribute();
    final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    final CriteriaQuery<T> criteria = builder.createQuery(entityClass);
    final Root<T> root = criteria.from(entityClass);

    criteria.select(root);

    // Normal case where we do a simple query with all the conditions in its where clause.
    final List<Predicate> predicates =
        createStandardPredicatesForMbiLookup(
            builder,
            root,
            resourceType,
            mbiSearchValue,
            isMbiSearchValueHashed,
            lastUpdated,
            serviceDate);

    criteria.where(predicates.toArray(new Predicate[0]));

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
  @Trace
  public <T> List<ClaimWithSecurityTagsV2<T>> findAllByMbiAttributeWithSecurityTags(
      ResourceTypeV2<?, T> resourceType,
      String mbiSearchValue,
      boolean isMbiSearchValueHashed,
      DateRangeParam lastUpdated,
      DateRangeParam serviceDate) {

    final Class<T> entityClass = resourceType.getEntityClass();
    final String idAttributeName = resourceType.getEntityIdAttribute();
    final CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    final CriteriaQuery<T> criteria = builder.createQuery(entityClass);
    final Root<T> root = criteria.from(entityClass);

    // Standard predicates for the MBI lookup
    final List<Predicate> predicates =
        createStandardPredicatesForMbiLookup(
            builder,
            root,
            resourceType,
            mbiSearchValue,
            isMbiSearchValueHashed,
            lastUpdated,
            serviceDate);

    criteria.where(predicates.toArray(new Predicate[0]));

    // Sorting to ensure predictable responses
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

    // List to hold claims with their respective security tags
    List<ClaimWithSecurityTagsV2<T>> claimEntitiesWithTags = new ArrayList<>();
    Long claimId = null;
    String mcsFissClaimId = null;
    String query = null;

    if (claimEntities != null) {
      // Iterate over each claim and get its security tags
      for (T claimEntity : claimEntities) {
        // Identify claim type and get the claimId
        switch (claimEntity) {
          case RdaMcsClaim rdaMcsClaim:
            mcsFissClaimId = rdaMcsClaim.getIdrClmHdIcn();
            query = "SELECT t.code FROM McsTag t WHERE t.claim = :mcsFissClaimId";
            break;
          case RdaFissClaim rdaFissClaim:
            mcsFissClaimId = rdaFissClaim.getClaimId();
            query = "SELECT t.code FROM FissTag t WHERE t.claim = :mcsFissClaimId";
            break;
          case CarrierClaim carrierClaim:
            claimId = carrierClaim.getClaimId();
            query = "SELECT t.code FROM CarrierTag t WHERE t.claim = :claimId";
            break;
          case DMEClaim dmeClaim:
            claimId = dmeClaim.getClaimId();
            query = "SELECT t.code FROM DmeTag t WHERE t.claim = :claimId";
            break;
          case HHAClaim hhaClaim:
            claimId = hhaClaim.getClaimId();
            query = "SELECT t.code FROM HhaTag t WHERE t.claim = :claimId";
            break;
          case HospiceClaim hospiceClaim:
            claimId = hospiceClaim.getClaimId();
            query = "SELECT t.code FROM HospiceTag t WHERE t.claim = :claimId";
            break;
          case InpatientClaim inpatientClaim:
            claimId = inpatientClaim.getClaimId();
            query = "SELECT t.code FROM InpatientTag t WHERE t.claim = :claimId";
            break;
          case OutpatientClaim outpatientClaim:
            claimId = outpatientClaim.getClaimId();
            query = "SELECT t.code FROM OutpatientTag t WHERE t.claim = :claimId";
            break;
          case SNFClaim snfClaim:
            claimId = snfClaim.getClaimId();
            query = "SELECT t.code FROM SnfTag t WHERE t.claim = :claimId";
            break;
          default:
            // Handle other claim types or throw an exception if the type is unsupported
            throw new IllegalArgumentException(
                "Unsupported claim type: " + claimEntity.getClass().getName());
        }

        // Query tags for the identified claimId
        List<String> securityTagResults =
            entityManager
                .createQuery(query, String.class)
                .setParameter(mcsFissClaimId != null ? "mcsFissClaimId" : "claimId", claimId)
                .getResultList();

        // Collect the security tags into a Set (avoiding duplicates)
        Set<String> securityTags = new HashSet<>(securityTagResults);

        // Wrap the claim and its tags in the response object
        claimEntitiesWithTags.add(new ClaimWithSecurityTagsV2<>(claimEntity, securityTags));
      }
    }
    return claimEntitiesWithTags;
  }

  /**
   * Builds a list of predicates for standard MBI and date range restrictions on search. Used for
   * FISS claim lookup and for MCS root lookup when no service date restriction is in place. If an
   * entity has multiple service date attributes (e.g. from and/or to date) the query will match if
   * any of those dates match.
   *
   * @param builder The {@link CriteriaBuilder} being used in current query.
   * @param root The {@link Root} for the claim in the query
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
      Root<T> root,
      ResourceTypeV2<?, T> resourceType,
      String mbiSearchValue,
      boolean isMbiSearchValueHashed,
      DateRangeParam lastUpdated,
      DateRangeParam serviceDate) {
    final String mbiRecordAttributeName = resourceType.getEntityMbiRecordAttribute();
    final List<String> serviceDateAttributeNames = resourceType.getEntityServiceDateAttributes();
    final List<Predicate> predicates = new ArrayList<>();
    final Path<Object> mbiRecord = root.get(mbiRecordAttributeName);
    predicates.add(createMbiPredicate(mbiRecord, mbiSearchValue, isMbiSearchValueHashed, builder));
    if (isDateRangePresent(lastUpdated)) {
      predicates.add(lastUpdatedPredicate(root, lastUpdated, builder));
    }
    if (isDateRangePresent(serviceDate) && !serviceDateAttributeNames.isEmpty()) {
      final var serviceDatePredicate =
          createServiceDatePredicates(builder, root, serviceDate, serviceDateAttributeNames);
      predicates.add(serviceDatePredicate);
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
   * Creates a service date predicate for each attribute name in the list and combines them using
   * logical OR.
   *
   * @param builder The {@link CriteriaBuilder} being used in current query.
   * @param root The {@link Root} for the claim in the query
   * @param serviceDate The range of the desired service date to search on.
   * @param serviceDateAttributeNames List of service date attribute names.
   * @return a {@link Predicate} to be used in query where clause
   * @param <T> The entity type being retrieved.
   */
  @VisibleForTesting
  <T> Predicate createServiceDatePredicates(
      CriteriaBuilder builder,
      Root<T> root,
      DateRangeParam serviceDate,
      List<String> serviceDateAttributeNames) {
    final var serviceDatePredicates =
        serviceDateAttributeNames.stream()
            .map(attributeName -> root.<LocalDate>get(attributeName))
            .map(datePath -> serviceDatePredicate(builder, serviceDate, datePath))
            .toArray(arraySize -> new Predicate[arraySize]);
    return builder.or(serviceDatePredicates);
  }

  /**
   * Helper method for easier mocking related to metrics.
   *
   * @param resourceType the resource type
   * @param queryName the query name
   * @param queryTime The amount of time passed executing the query.
   * @param querySize The number of entities returned by the query.
   */
  @VisibleForTesting
  void logQueryMetric(
      ResourceTypeV2<?, ?> resourceType, String queryName, long queryTime, int querySize) {
    final String combinedQueryId = String.format("%s_%s", queryName, resourceType.getTypeLabel());
    CommonTransformerUtils.recordQueryInMdc(combinedQueryId, queryTime, querySize);
  }

  /**
   * Helper method to create the appropriate MBI predicate depending on if the current or old MBI
   * Hash should be used.
   *
   * @param root The root path of the entity to get attributes from.
   * @param mbiSearchValue The MBI value being searched for.
   * @param isMbiSearchValueHashed Indicates if the search value is a hash or raw MBI.
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
  Predicate lastUpdatedPredicate(Root<?> root, DateRangeParam dateRange, CriteriaBuilder builder) {
    return QueryUtils.createLastUpdatedPredicate(builder, root, dateRange);
  }

  /**
   * Helper method to create a service date range predicate.
   *
   * @param builder The builder to use for creating predicates.
   * @param serviceDate The service date to search for.
   * @param dateExpression An expression defining the date value to test.
   * @return A {@link Predicate} that checks for the given service date range.
   */
  @VisibleForTesting
  Predicate serviceDatePredicate(
      CriteriaBuilder builder, DateRangeParam serviceDate, Expression<LocalDate> dateExpression) {
    return QueryUtils.createDateRangePredicate(builder, serviceDate, dateExpression);
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
        ClaimDao.class.getSimpleName(), "query", queryName, resourceType.getTypeLabel());
  }
}
