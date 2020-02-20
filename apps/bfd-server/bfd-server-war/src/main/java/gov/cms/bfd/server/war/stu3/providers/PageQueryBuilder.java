package gov.cms.bfd.server.war.stu3.providers;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

public final class PageQueryBuilder<T> {

  private final Root<T> root;
  private final CriteriaBuilder builder;
  private final CriteriaQuery criteria;
  private final EntityManager entityManager;

  public PageQueryBuilder(Class<T> returnType, EntityManager em) {
    entityManager = em;
    builder = entityManager.getCriteriaBuilder();
    criteria = builder.createQuery(returnType);
    root = criteria.from(returnType);
  }

  public Root<T> root() {
    return this.root;
  }

  public CriteriaBuilder builder() {
    return this.builder;
  }

  public CriteriaQuery criteria() {
    return this.criteria;
  }

  private CriteriaQuery<Long> createCountCriteria() {
    CriteriaQuery countCriteria = builder.createQuery(Long.class);
    Root<T> countRoot = countCriteria.from(this.criteria.getResultType());
    countCriteria.select(builder.count(countRoot));
    countCriteria.where(this.criteria.getRestriction());
    return countCriteria;
  }

  public Long count() {
    return entityManager.createQuery(createCountCriteria()).getSingleResult();
  }

  private CriteriaQuery<T> createCriteria() {
    criteria.select(root);
    return criteria;
  }

  public Query createQuery() {
    return entityManager.createQuery(createCriteria());
  }
}
