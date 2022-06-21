package gov.cms.model.dsl.codegen.plugin.model;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;

/** Root model object for defining a mapping in the DSL. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MappingBean {
  /** Unique identifier for the mapping. */
  private String id;

  /** Full class name for source message object. */
  private String messageClassName;

  /** Full class name for entity object. */
  private String entityClassName;

  /** Full class name for transformer object to be generated. */
  private String transformerClassName;

  /** Defines the type of objects being transformed (either GRPC or CSV). */
  private SourceType sourceType = SourceType.Grpc;

  /** Defines how nullable values are passed to and from field accessor methods. */
  private NullableFieldAccessorType nullableFieldAccessorType = NullableFieldAccessorType.Standard;

  /** Meta data for the database table. */
  private TableBean table;

  /** Minimum valid string length for non-null string fields. */
  @Builder.Default private int minStringLength = 1;

  /** Meta data for enum types. */
  @Singular private List<EnumTypeBean> enumTypes = new ArrayList<>();

  /** Meta data for transformations used to copy data from message to entity. */
  @Singular private List<TransformationBean> transformations = new ArrayList<>();

  /** Meta data for arrays. */
  @Singular private List<ArrayBean> arrays = new ArrayList<>();

  /** Meta data for any external transformations used in transformer. */
  @Singular private List<ExternalTransformationBean> externalTransformations = new ArrayList<>();

  /** List of extra interfaces to add to the entity class. */
  @Singular private List<String> entityInterfaces = new ArrayList<>();

  /**
   * Finds the {@link EnumTypeBean} in this mapping with the given name and returns it.
   *
   * @param enumName name of the enum
   * @return the {@link EnumTypeBean} for the given name
   * @throws IllegalArgumentException if no such enum exists
   */
  public EnumTypeBean findEnum(String enumName) {
    return enumTypes.stream()
        .filter(e -> enumName.equals(e.getName()))
        .findAny()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    String.format(
                        "reference to non-existent enum %s in mapping %s", enumName, id)));
  }

  /**
   * Determines if a {@code transformerClassName} has been defined.
   *
   * @return true if a non-empty {code transformerClassName} has been defined
   */
  public boolean hasTransformer() {
    return !Strings.isNullOrEmpty(transformerClassName);
  }

  /**
   * Determines if any {@code arrays} have been defined.
   *
   * @return true if one or more {code arrays} have been defined
   */
  public boolean hasArrayElements() {
    return arrays.size() > 0;
  }

  /**
   * Determines if any {@code externalTransformations} have been defined.
   *
   * @return true if one or more {code externalTransformations} have been defined
   */
  public boolean hasExternalTransformations() {
    return externalTransformations.size() > 0;
  }

  /**
   * Extract just the java package name for the entity.
   *
   * @return the java package name for the entity.
   */
  public String getEntityClassPackage() {
    return ModelUtil.packageName(entityClassName);
  }

  /**
   * Extract just the java class name for the entity.
   *
   * @return the java class name for the entity.
   */
  public String getEntityClassSimpleName() {
    return ModelUtil.className(entityClassName);
  }

  /**
   * Extract the java package name for the transformer.
   *
   * @return the java package name for the transformer.
   */
  public String transformerPackage() {
    return ModelUtil.packageName(transformerClassName);
  }

  /**
   * Extract the java class name for the transformer.
   *
   * @return the java class name for the transformer.
   */
  public String transformerSimpleName() {
    return ModelUtil.className(transformerClassName);
  }

  /**
   * Determines if any {@code entityInterfaces} have been defined.
   *
   * @return true if one or more {code entityInterfaces} have been defined
   */
  public boolean hasEntityInterfaces() {
    return entityInterfaces.size() > 0;
  }

  /**
   * Searches for a {@link JoinBean} in this entity with the given name.
   *
   * @param name name of the join field
   * @return filled optional containing the {@link JoinBean} if one matches, otherwise empty
   */
  public Optional<JoinBean> findJoinByFieldName(String name) {
    return table.getJoins().stream().filter(c -> name.equals(c.getFieldName())).findAny();
  }

  /**
   * Returns an immutable list of all {@link JoinBean} in our {@link TableBean} that are not related
   * to one of the {@link ArrayBean} fields in this {@link MappingBean}.
   *
   * @return filtered list of {@link JoinBean}
   */
  public List<JoinBean> getNonArrayJoins() {
    var arrayFieldNames = arrays.stream().map(ArrayBean::getTo).collect(Collectors.toSet());
    return table.getJoins().stream()
        .filter(j -> !arrayFieldNames.contains(j.getFieldName()))
        .collect(ImmutableList.toImmutableList());
  }

  /** Enum used to define the type of source objects for the transformations. */
  public enum SourceType {
    /** Source objects are GRPC stubs. */
    Grpc,
    /** Source objects are CSV data wrappers. */
    RifCsv
  }

  /** Enum used to define how nullable fields are accessed. */
  public enum NullableFieldAccessorType {
    /** Nullable fields are accessed directly and have null value if not defined. */
    Standard,
    /** Nullable fields are wrapped in an Optional that is empty if the value is not defined. */
    Optional
  }
}
