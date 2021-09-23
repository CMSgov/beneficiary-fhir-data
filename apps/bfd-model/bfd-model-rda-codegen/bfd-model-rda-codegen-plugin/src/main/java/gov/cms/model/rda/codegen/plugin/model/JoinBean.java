package gov.cms.model.rda.codegen.plugin.model;

import com.google.common.base.Strings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JoinBean {
  /** Name of the field in this entity object that holds the joined entity. */
  private String name;

  /** Full class name of the joined entity. Must include the package as well as the class name. */
  private String entityClass;

  /** Name of the column in this entity that will hold the joined table's primary key. */
  private String joinColumnName;

  /**
   * Type of join annotation to apply to this field. Either {@code ManyToOne} or {@code OneToMany}.
   */
  private String joinType;

  /** Name of the {@link javax.persistence.FetchType}. Either {@code EAGER} or @{code LAZY}. */
  private String fetchType;

  /** Optional comment string to be added to the join field in the generated entity. */
  private String comment;

  public boolean isValidEntityClass() {
    return entityClass.indexOf('.') > 0;
  }

  public String getEntityPackage() {
    return entityClass.substring(0, entityClass.lastIndexOf('.'));
  }

  public String getEntityClass() {
    return entityClass.substring(entityClass.lastIndexOf('.') + 1);
  }

  public boolean hasColumnName() {
    return !Strings.isNullOrEmpty(joinColumnName);
  }

  public boolean hasComment() {
    return !Strings.isNullOrEmpty(comment);
  }
}
