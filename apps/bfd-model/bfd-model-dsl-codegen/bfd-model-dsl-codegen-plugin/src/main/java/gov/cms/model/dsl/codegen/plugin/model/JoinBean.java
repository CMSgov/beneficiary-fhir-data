package gov.cms.model.dsl.codegen.plugin.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.base.Strings;
import com.squareup.javapoet.ClassName;
import gov.cms.model.dsl.codegen.plugin.model.validation.JavaName;
import gov.cms.model.dsl.codegen.plugin.model.validation.JavaNameType;
import gov.cms.model.dsl.codegen.plugin.model.validation.JavaType;
import gov.cms.model.dsl.codegen.plugin.model.validation.MappingExists;
import gov.cms.model.dsl.codegen.plugin.model.validation.ValidationUtil;
import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.experimental.SuperBuilder;

/**
 * Model object for defining JPA compatible join relationships between entities. Several sub-types
 * exist that set default values based on the type of relationship the join represents. The sub-type
 * is specified in the {@code class} property within the YAML. Jackson selects the concrete class
 * based on the values defined in the {@link JsonTypeInfo} and {@link JsonSubTypes} annotations.
 * Using the {@code class} property simplifies the model by defaulting property values and adding
 * specific validity checks appropriately for the join's intended use.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "class", defaultImpl = JoinBean.class)
@JsonSubTypes({
  @JsonSubTypes.Type(name = "array", value = JoinBean.Array.class),
  @JsonSubTypes.Type(name = "parent", value = JoinBean.Parent.class),
  @JsonSubTypes.Type(name = "child", value = JoinBean.Child.class),
})
public class JoinBean implements ModelBean {
  /** Name of the field in this entity object that holds the joined entity. */
  @NotNull @JavaName private String fieldName;

  /**
   * Used when joining to an entity that exists outside of the model. Full class name of the joined
   * entity. Must include the package as well as the class name.
   */
  @JavaName(type = JavaNameType.Compound)
  private String entityClass;

  /**
   * Used when joining to an entity that exists inside of the model. {@link MappingBean#id} of the
   * joined entity mapping within the model.
   */
  @JavaName @MappingExists private String entityMapping;

  /** Name of the column in this entity that will hold the joined table's primary key. */
  @JavaName(type = JavaNameType.Compound)
  private String joinColumnName;

  /** Type of join annotation to apply to this field. */
  private JoinType joinType;

  /** Name of the {@link FetchType}. Either {@link FetchType#EAGER} or @{link FetchType.LAZY}. */
  private FetchType fetchType;

  /** Optional comment string to be added to the join field in the generated entity. */
  private String comment;

  /** Type of collection to use for storing joined objects. */
  @NotNull @Builder.Default private CollectionType collectionType = CollectionType.List;

  /** Value for {@code mappedBy} argument to annotation. */
  @JavaName private String mappedBy;

  /** Value for {@code orphanRemoval} argument to annotation. */
  private Boolean orphanRemoval;

  /** {@link CascadeType}s to use as argument to the annotation. */
  @NotNull @Builder.Default private List<CascadeType> cascadeTypes = new ArrayList<>();

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

  /** Value for {@code readOnly} argument to annotation. */
  @Builder.Default private boolean readOnly = false;

  /**
   * Optional list of properties to generate for single-value joins to simplify access to fields in
   * the joined entity.
   */
  @Singular private List<@Valid Property> properties = new ArrayList<>();

  /**
   * Called by validator to confirm that either {@link #entityClass} or {@link #entityMapping} was
   * provided but not both.
   *
   * @return true if exactly one is not null
   */
  @AssertTrue
  public boolean isEntityClassOrMappingDefined() {
    return ValidationUtil.isExactlyOneNotNull(entityClass, entityMapping);
  }

  /**
   * Determine if there is a join column name defined.
   *
   * @return true if a non-empty value is defined
   */
  public boolean hasColumnName() {
    return !Strings.isNullOrEmpty(joinColumnName);
  }

  /**
   * Determine if there is a javadoc comment defined.
   *
   * @return true if a non-empty value is defined
   */
  public boolean hasComment() {
    return !Strings.isNullOrEmpty(comment);
  }

  /**
   * Determine if there is a value defined for the {@code mappedBy} argument to the annotation.
   *
   * @return true if there is a value defined for the {@code mappedBy} argument to the annotation.
   */
  public boolean hasMappedBy() {
    return !Strings.isNullOrEmpty(mappedBy);
  }

  /**
   * Determine if there is a value defined for the {@code orphanRemoval} argument to the annotation.
   *
   * @return true if there is a value defined for the {@code orphanRemoval} argument to the
   *     annotation.
   */
  public boolean hasOrphanRemoval() {
    return orphanRemoval != null;
  }

  /**
   * Determine if there is a value defined for the {@code orderBy} argument to the annotation.
   *
   * @return true if there is a value defined for the {@code orderBy} argument to the annotation.
   */
  public boolean hasOrderBy() {
    return !Strings.isNullOrEmpty(orderBy);
  }

  /**
   * Determine if there is a value defined for the {@link javax.persistence.ForeignKey} annotation.
   *
   * @return true if there is a value defined for the {@link javax.persistence.ForeignKey}
   *     annotation.
   */
  public boolean hasForeignKey() {
    return !Strings.isNullOrEmpty(foreignKey);
  }

  /**
   * Determine if there is a value defined for the {@code fetchType} argument to the annotation.
   *
   * @return true if there is a value defined for the {@code fetchType} argument to the annotation.
   */
  public boolean isFetchTypeRequired() {
    return fetchType != null;
  }

  @Override
  public String getDescription() {
    return "join " + fieldName + " to " + entityClass + "/" + entityMapping;
  }

  /**
   * An enum type used to specify which annotation to use for the join and whether the join returns
   * a single value or multiple values.
   */
  @AllArgsConstructor
  public enum JoinType {
    /** OneToMany. */
    OneToMany(ClassName.get(jakarta.persistence.OneToMany.class), true),
    /** ManyToOne. */
    ManyToOne(ClassName.get(jakarta.persistence.ManyToOne.class), false),
    /** OneToOne. */
    OneToOne(ClassName.get(jakarta.persistence.OneToOne.class), false);

    /** The annotation class to use when adding the join annotation to the entity. */
    @Getter private final ClassName annotationClass;

    /** Whether the join has a single value or multiple values. */
    @Getter private final boolean multiValue;

    /**
     * Determine if the join returns a single value.
     *
     * @return true if the join returns a single value
     */
    public boolean isSingleValue() {
      return !multiValue;
    }
  }

  /**
   * An enum type used to specify which type of collection to use for joins that return multiple
   * values.
   */
  @AllArgsConstructor
  public enum CollectionType {
    /** List. */
    List(ClassName.get(List.class), ClassName.get(LinkedList.class)),
    /** Set. */
    Set(ClassName.get(Set.class), ClassName.get(HashSet.class));

    /** Collection interface used to declare the field in the entity class. */
    @Getter private final ClassName interfaceName;

    /** Collection class used to create an instance for the field in the entity class. */
    @Getter private final ClassName className;
  }

  /**
   * For single value joins we can define properties of the containing entity that are tied to a
   * field in the joined entity. This object holds the desired name of the property, the name of the
   * field within the joined object, and the java type of the field.
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Property {
    /**
     * Name of the property in the containing entity. The generated getter name will be {@code get}
     * followed by the capitalized {@link Property#name}.
     */
    @NotNull @JavaName private String name;

    /** Name of the field in the joined object to get a value from when the getter is called. */
    @NotNull @JavaName private String fieldName;

    /**
     * Indicates the java type to use for the return value of the getter. Values must be recognized
     * by {@link ModelUtil#mapJavaTypeToTypeName}.
     */
    @JavaType private String javaType;
  }

  /**
   * Model class for array mappings in DSL. Instances default to {@link JoinType#OneToMany}, {@link
   * CollectionType#Set}, and require the definition of the {@link JoinBean#entityMapping} property.
   */
  @Data
  @EqualsAndHashCode(callSuper = true)
  @SuperBuilder(toBuilder = true)
  public static class Array extends JoinBean {
    /** Constructs the bean with default settings appropriate to an array join. */
    public Array() {
      setJoinType(JoinBean.JoinType.OneToMany);
      setCollectionType(CollectionType.Set);
      setFetchType(FetchType.EAGER);
      setOrphanRemoval(true);
      getCascadeTypes().add(CascadeType.ALL);
    }

    /**
     * Tests whether or not the array element objects have a field to hold a reference to the parent
     * object.
     *
     * @return true if the {@link JoinBean#mappedBy} has a non-empty value
     */
    public boolean hasMappedBy() {
      return !Strings.isNullOrEmpty(getMappedBy());
    }

    @Override
    public String getDescription() {
      return "array " + super.getDescription();
    }

    /**
     * {@inheritDoc} Overrides the superclass method to ensure that the entityMapping is always
     * present.
     *
     * @return the entityMapping
     */
    @Override
    public @NotNull @JavaName String getEntityMapping() {
      return super.getEntityMapping();
    }
  }

  /**
   * Model class for parent side of one-to-many relationship in DSL. Instances default to {@link
   * JoinType#OneToMany}, {@link CollectionType#List}, and require the definition of the {@link
   * JoinBean#mappedBy} and {@link JoinBean#entityMapping} properties.
   */
  @Data
  @EqualsAndHashCode(callSuper = true)
  @SuperBuilder(toBuilder = true)
  public static class Parent extends JoinBean {
    /** Constructs the bean with default settings appropriate to a parent join. */
    public Parent() {
      setJoinType(JoinType.OneToMany);
      setCollectionType(CollectionType.List);
      setFetchType(FetchType.LAZY);
      setReadOnly(true);
      setOrphanRemoval(true);
      getCascadeTypes().add(CascadeType.ALL);
    }

    /**
     * {@inheritDoc} Overrides the superclass method to ensure that the entityMapping is always
     * present.
     *
     * @return the entityMapping
     */
    @Override
    public @NotNull @JavaName String getEntityMapping() {
      return super.getEntityMapping();
    }

    /**
     * {@inheritDoc} Overrides the superclass method to ensure that the mappedBy is always present.
     *
     * @return the mappedBy value
     */
    @Override
    public @NotNull @JavaName String getMappedBy() {
      return super.getMappedBy();
    }

    @Override
    public String getDescription() {
      return "parent " + super.getDescription();
    }
  }

  /**
   * Model class for child side of one-to-many relationship in DSL. Instances default to {@link
   * JoinType#ManyToOne} and require the definition of the {@link JoinBean#joinColumnName} and
   * {@link JoinBean#entityMapping} properties.
   */
  @Data
  @EqualsAndHashCode(callSuper = true)
  @SuperBuilder(toBuilder = true)
  public static class Child extends JoinBean {
    /** Constructs the bean with default settings appropriate to a child join. */
    public Child() {
      setJoinType(JoinType.ManyToOne);
    }

    /**
     * {@inheritDoc} Overrides the superclass method to ensure that the entityMapping is always
     * present.
     *
     * @return the entityMapping
     */
    @Override
    public @NotNull @JavaName String getEntityMapping() {
      return super.getEntityMapping();
    }

    /**
     * {@inheritDoc} Overrides the superclass method to ensure that the joinColumnName is always
     * present.
     *
     * @return the joinColumnName
     */
    @Override
    public @NotNull @JavaName String getJoinColumnName() {
      return super.getJoinColumnName();
    }

    @Override
    public String getDescription() {
      return "child " + super.getDescription();
    }
  }
}
