package gov.cms.bfd.server.war.commons;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.annotations.VisibleForTesting;
import gov.cms.bfd.server.war.r4.providers.IPreAdjClaimResponseTypeV2;
import gov.cms.bfd.server.war.r4.providers.IPreAdjClaimTypeV2;
import gov.cms.bfd.server.war.r4.providers.TransformerUtilsV2;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

/** Provides common logic for performing DB interactions */
public class PreAdjClaimDao {

  private static final String CLAIM_METRIC_QUERY_ID = "claim_by_id";
  private static final String CLAIM_METRIC_NAME =
      MetricRegistry.name(PreAdjClaimDao.class.getSimpleName(), "query", CLAIM_METRIC_QUERY_ID);

  private final EntityManager entityManager;
  private final MetricRegistry metricRegistry;

  public PreAdjClaimDao(EntityManager entityManager, MetricRegistry metricRegistry) {
    this.entityManager = entityManager;
    this.metricRegistry = metricRegistry;
  }

  /**
   * Gets an entity by it's ID for the given claim type.
   *
   * @param type The type of claim to retrieve.
   * @param id The id of the claim to retrieve.
   * @return An entity object of the given type provided in {@link IPreAdjClaimTypeV2}
   */
  public Object getEntityById(IPreAdjClaimTypeV2 type, String id) {
    return getEntityById(type.getEntityClass(), type.getEntityIdAttribute(), id);
  }

  /**
   * Gets an entity by it's ID for the given claim type.
   *
   * @param type The type of claim to retrieve.
   * @param id The id of the claim to retrieve.
   * @return An entity object of the given type provided in {@link IPreAdjClaimResponseTypeV2}
   */
  public Object getEntityById(IPreAdjClaimResponseTypeV2 type, String id) {
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

    Timer.Context timerClaimQuery = metricRegistry.timer(CLAIM_METRIC_NAME).time();
    try {
      claimEntity = entityManager.createQuery(criteria).getSingleResult();
    } finally {
      long claimByIdQueryNanoSeconds = timerClaimQuery.stop();
      TransformerUtilsV2.recordQueryInMdc(
          CLAIM_METRIC_QUERY_ID, claimByIdQueryNanoSeconds, claimEntity == null ? 0 : 1);
    }

    return claimEntity;
  }
}
