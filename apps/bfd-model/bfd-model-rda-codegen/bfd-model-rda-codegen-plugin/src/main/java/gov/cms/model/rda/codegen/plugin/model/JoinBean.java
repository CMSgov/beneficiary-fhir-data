package gov.cms.model.rda.codegen.plugin.model;

import com.google.common.base.Strings;
import com.squareup.javapoet.ClassName;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.FetchType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JoinBean {
  /** Name of the field in this entity object that holds the joined entity. */
  private String fieldName;

  /** Full class name of the joined entity. Must include the package as well as the class name. */
  private String entityClass;

  /** Name of the column in this entity that will hold the joined table's primary key. */
  private String joinColumnName;

  /** Type of join annotation to apply to this field. */
  private JoinType joinType;

  /** Name of the {@link FetchType}. Either {@code EAGER} or @{code LAZY}. */
  private FetchType fetchType;

  /** Optional comment string to be added to the join field in the generated entity. */
  private String comment;

  private CollectionType collectionType = CollectionType.List;

  private String mappedBy;

  private Boolean orphanRemoval;

  private List<CascadeType> cascadeTypes = new ArrayList<>();

  /**
   * Optionally specifies an order by expression to add using an {@link javax.persistence.OrderBy}
   * annotation.
   */
  private String orderBy;

  /**
   * Optionally specifies a foreign key constraint to reference in the {@link
   * javax.persistence.JoinColumn} annotation.
   */
  private String foreignKey;

  public boolean isValidEntityClass() {
    return entityClass.indexOf('.') > 0;
  }

  public ClassName getEntityClassType() {
    return ModelUtil.classType(entityClass);
  }

  public boolean hasColumnName() {
    return !Strings.isNullOrEmpty(joinColumnName);
  }

  public boolean hasComment() {
    return !Strings.isNullOrEmpty(comment);
  }

  public boolean hasMappedBy() {
    return !Strings.isNullOrEmpty(mappedBy);
  }

  public boolean hasOrphanRemoval() {
    return orphanRemoval != null;
  }

  public boolean hasOrderBy() {
    return !Strings.isNullOrEmpty(orderBy);
  }

  public boolean hasForeignKey() {
    return !Strings.isNullOrEmpty(foreignKey);
  }

  public boolean isFetchTypeRequired() {
    return fetchType != null;
  }

  @AllArgsConstructor
  public enum JoinType {
    OneToMany(javax.persistence.OneToMany.class, true),
    ManyToOne(javax.persistence.ManyToOne.class, false),
    OneToOne(javax.persistence.OneToOne.class, false);

    @Getter private final Class<?> annotationClass;
    @Getter private final boolean multiValue;

    public boolean isSingleValue() {
      return !multiValue;
    }
  }

  @AllArgsConstructor
  public enum CollectionType {
    List(ClassName.get(List.class), ClassName.get(LinkedList.class)),
    Set(ClassName.get(Set.class), ClassName.get(HashSet.class));
    @Getter private final ClassName interfaceName;
    @Getter private final ClassName className;
  }
}
