package gov.cms.bfd.server.war.commons;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import gov.cms.bfd.server.war.r4.providers.PreAdjClaimResponseTypeV2;
import gov.cms.bfd.server.war.r4.providers.PreAdjClaimTypeV2;
import gov.cms.bfd.server.war.r4.providers.TransformerUtilsV2;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

public class PreAdjClaimDao {

  private final EntityManager entityManager;
  private final MetricRegistry metricRegistry;

  public PreAdjClaimDao(EntityManager entityManager, MetricRegistry metricRegistry) {
    this.entityManager = entityManager;
    this.metricRegistry = metricRegistry;
  }

  public Object getEntityById(PreAdjClaimTypeV2 type, String id) {
    return getEntityById(type.getEntityClass(), type.getEntityIdAttribute(), id);
  }

  public Object getEntityById(PreAdjClaimResponseTypeV2 type, String id) {
    return getEntityById(type.getEntityClass(), type.getEntityIdAttribute(), id);
  }

  public Object getEntityById(Class<?> entityClass, String entityIdAttribute, String id) {
    Object claimEntity = null;

    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<?> criteria = builder.createQuery(entityClass);
    Root root = criteria.from(entityClass);

    criteria.select(root);
    criteria.where(builder.equal(root.get(entityIdAttribute), id));

    Timer.Context timerClaimQuery =
        metricRegistry
            .timer(MetricRegistry.name(getClass().getSimpleName(), "query", "claim_by_id"))
            .time();
    try {
      claimEntity = entityManager.createQuery(criteria).getSingleResult();
    } finally {
      long claimByIdQueryNanoSeconds = timerClaimQuery.stop();
      TransformerUtilsV2.recordQueryInMdc(
          "claim_by_id", claimByIdQueryNanoSeconds, claimEntity == null ? 0 : 1);
    }

    return claimEntity;
  }
}
