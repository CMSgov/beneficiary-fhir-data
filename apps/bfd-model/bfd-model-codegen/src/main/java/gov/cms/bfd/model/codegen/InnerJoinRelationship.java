package gov.cms.bfd.model.codegen;

import com.fasterxml.jackson.databind.JsonNode;
import javax.persistence.Entity;

/**
 * Encapsulates the information needed to create an InnerJoinRelationship between two {@link
 * Entity}'s, one as the parent and the other as the child.
 */
public final class InnerJoinRelationship {

  private final String mappedBy;
  private final String orderBy;
  private final String childEntity;
  private final String childField;

  public InnerJoinRelationship(
      String mappedBy, String orderBy, String childEntity, String childField) {
    this.mappedBy = mappedBy;
    this.orderBy = orderBy;
    this.childEntity = childEntity;
    this.childField = childField;
  }

  public JsonNode createMetaData(MappingSummarizer logger) {
    var node = logger.createObject();
    node.put("type", "InnerJoinRelationship");
    node.put("mappedBy", mappedBy);
    node.put("orderBy", orderBy);
    node.put("childEntity", childEntity);
    node.put("childField", childField);
    return node;
  }

  /** @return the name of the field being mapped */
  public String getMappedBy() {
    return mappedBy;
  }

  /** @return the name of the field to order by */
  public String getOrderBy() {
    return orderBy;
  }

  /** @return the name of the child entity */
  public String getChildEntity() {
    return childEntity;
  }

  /** @return the name of the child field */
  public String getChildField() {
    return childField;
  }
}
