package gov.cms.bfd.model.codegen;

import javax.persistence.Entity;

/**
 * Encapsulates the information needed to create an InnerJoinRelationship between two {@link
 * Entity}'s, one as the parent and the other as the child.
 */
public final class InnerJoinRelationship {

  /** The name of the field being mapped. */
  private final String mappedBy;
  /** The name of the field to order by. */
  private final String orderBy;
  /** The name of the child entity. */
  private final String childEntity;
  /** The name of the child field. */
  private final String childField;

  /**
   * Instantiates a new Inner join relationship.
   *
   * @param mappedBy the name of the field being mapped
   * @param orderBy the name of the field to order by
   * @param childEntity the child entity name
   * @param childField the child field name
   */
  public InnerJoinRelationship(
      String mappedBy, String orderBy, String childEntity, String childField) {
    this.mappedBy = mappedBy;
    this.orderBy = orderBy;
    this.childEntity = childEntity;
    this.childField = childField;
  }

  /**
   * Gets the {@link #mappedBy}.
   *
   * @return the name of the field being mapped
   */
  public String getMappedBy() {
    return mappedBy;
  }

  /**
   * Gets the {@link #orderBy}.
   *
   * @return the name of the field to order by
   */
  public String getOrderBy() {
    return orderBy;
  }

  /**
   * Gets the {@link #childEntity}.
   *
   * @return the name of the child entity
   */
  public String getChildEntity() {
    return childEntity;
  }

  /**
   * Gets the {@link #childField}.
   *
   * @return the name of the child field
   */
  public String getChildField() {
    return childField;
  }
}
