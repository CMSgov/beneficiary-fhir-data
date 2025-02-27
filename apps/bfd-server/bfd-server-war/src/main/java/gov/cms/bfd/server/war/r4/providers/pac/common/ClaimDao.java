package gov.cms.bfd.server.war.r4.providers.pac.common;

import ca.uhn.fhir.rest.param.DateRangeParam;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.annotations.VisibleForTesting;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.model.rda.Mbi;
import gov.cms.bfd.server.war.commons.CommonTransformerUtils;
import gov.cms.bfd.server.war.commons.QueryUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Provides common logic for performing DB interactions. */
@EqualsAndHashCode
@AllArgsConstructor
public class ClaimDao {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClaimDao.class);

  /** Query name for logging MDC. */
  static final String CLAIM_BY_MBI_QUERY = "claim_by_mbi";

  /** Query name for logging MDC. */
  static final String CLAIM_BY_ID_QUERY = "claim_by_id";

  /** Query name for logging MDC. */
  static final String CLAIM_SECURITY_TAG_QUERY = "security_tag_by_claim";

  /** {@link EntityManager} used for database access. */
  private final EntityManager entityManager;

  /** {@link MetricRegistry} for metrics. */
  private final MetricRegistry metricRegistry;

  /** Whether or not to use old MBI hash functionality. */
  private final boolean isOldMbiHashEnabled;

  private static final Logger logger = LoggerFactory.getLogger(ClaimDao.class);

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
   * getSecurityTags By claim Ids.
   *
   * @param tagTable The table containing security tags
   * @param claimIds The id of the claim to retrieve.
   * @return An entity object of the given type provided in {@link ResourceTypeV2}
   */
  @Trace
  public Map<String, Set<String>> buildClaimIdToTagsMap(String tagTable, List<String> claimIds) {
    // If no claim IDs, return an empty map
    if (claimIds.isEmpty()) {
      return Collections.emptyMap();
    }

    List<Object[]> results = new ArrayList<>();

    if (tagTable != null) {
      String query =
          String.format("SELECT t.claim, t.code FROM %s t WHERE t.claim IN :claimIds", tagTable);

      results =
          entityManager
              .createQuery(query, Object[].class)
              .setParameter("claimIds", claimIds)
              .getResultList();
    }

    // Build the map from results
    Map<String, Set<String>> claimIdToTagsMap = new HashMap<>();
    for (Object[] result : results) {
      String claimId = result[0].toString();
      String tag = result[1].toString();

      // Add tag to the list for this claim ID
      claimIdToTagsMap.computeIfAbsent(claimId, k -> new HashSet<>()).add(tag);
    }

    return claimIdToTagsMap;
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
  public <T> List<ClaimWithSecurityTags<T>> findAllByMbiAttribute(
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

    List<ClaimWithSecurityTags<T>> claimEntitiesWithTags = new ArrayList<>();
    List<String> claimIds = new ArrayList<>();

    if (claimEntities != null) {
      collectClaimIds((List<Object>) claimEntities, claimIds, resourceType.getEntityIdAttribute());

      if (!claimIds.isEmpty()) {
        // Query for security tags by the collected claim IDs
        Map<String, Set<String>> claimIdToTagsMap =
            buildClaimIdToTagsMap(resourceType.getEntityTagType(), claimIds);

        // Process all claims using the map from the single query
        claimEntities.stream()
            .forEach(
                claimEntity -> {
                  // Get the claim ID
                  String claimId = extractClaimId(claimEntity, resourceType.getEntityIdAttribute());

                  // Look up this claim's tags from our pre-fetched map (no additional DB query)
                  Set<String> claimSpecificTags =
                      claimIdToTagsMap.getOrDefault(claimId, Collections.emptySet());

                  // Wrap the claim and its tags in the response object
                  claimEntitiesWithTags.add(
                      new ClaimWithSecurityTags<>(claimEntity, claimSpecificTags));
                });
      }
    }
    return claimEntitiesWithTags;
  }

  // Method to dynamically collect claim IDs for the given claim entity
  private void collectClaimIds(
      List<Object> claimEntities, List<String> claimIds, String entityIdAttribute) {
    for (Object claimEntity : claimEntities) {

      try {
        // Dynamically access the field corresponding to the entityIdAttribute using reflection
        Field entityIdField = claimEntity.getClass().getDeclaredField(entityIdAttribute);
        entityIdField.setAccessible(true); // Make the field accessible

        // Get the value of the entityIdField
        Object claimIdValue = entityIdField.get(claimEntity);

        // If a valid claim ID is found, add it to the claimIds list
        if (claimIdValue != null) {
          claimIds.add(claimIdValue.toString());
        }
      } catch (NoSuchFieldException e) {
        LOGGER.debug("Field '{}' not found for claim entity: {}", entityIdAttribute, claimEntity);
      } catch (IllegalAccessException e) {
        LOGGER.error("Failed to access entity ID attribute for claim entity: {}", claimEntity, e);
      }
    }
  }

  private String extractClaimId(Object claimEntity, String entityIdAttribute) {
    try {
      // Dynamically access the field corresponding to the entityIdAttribute using reflection
      Field entityIdField = claimEntity.getClass().getDeclaredField(entityIdAttribute);
      entityIdField.setAccessible(true); // Make the field accessible

      Object claimIdValue = entityIdField.get(claimEntity);

      if (claimIdValue != null) {
        return claimIdValue.toString();
      }
    } catch (NoSuchFieldException e) {
      // Field not found, try the next one
    } catch (IllegalAccessException e) {
      // Access error, try the next one
    }
    // If no ID found, return empty string or throw an exception
    return "";
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
