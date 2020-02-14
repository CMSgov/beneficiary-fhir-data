package gov.cms.bfd.server.war.stu3.providers;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

public final class PageQueryBuilder<T> {

  public final Root<T> root;
  public final CriteriaBuilder builder;
  public final CriteriaQuery criteria;
  private final EntityManager entityManager;

  public PageQueryBuilder(Class<T> returnType, EntityManager em) {
    entityManager = em;
    builder = entityManager.getCriteriaBuilder();
    criteria = builder.createQuery(returnType);
    root = criteria.from(returnType);
  }

  private CriteriaQuery<Long> createCountCriteria() {
    criteria.select(builder.count(root));
    return criteria;
  }

  public Long count() {
    return entityManager.createQuery(createCountCriteria()).getSingleResult();
  }

  public Query createQuery() {
    criteria.select(root);

    return entityManager.createQuery(criteria);
  }
}
