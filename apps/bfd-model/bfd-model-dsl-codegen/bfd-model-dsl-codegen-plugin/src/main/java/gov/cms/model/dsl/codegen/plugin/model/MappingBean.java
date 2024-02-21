package gov.cms.model.dsl.codegen.plugin.model;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import gov.cms.model.dsl.codegen.plugin.model.validation.JavaName;
import gov.cms.model.dsl.codegen.plugin.model.validation.JavaNameType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
public class MappingBean implements ModelBean {
  /**
   * Implementation of {@link Comparator} that can be used to sort mappings by their {@link
   * MappingBean#id} values.
   */
  public static final Comparator<MappingBean> IdComparator =
      Comparator.comparing(mapping -> Strings.nullToEmpty(mapping.getId()));

  /** Unique identifier for the mapping. */
  @NotNull @JavaName private String id;

  /** Full class name for source message object. */
  @JavaName(type = JavaNameType.Compound)
  private String messageClassName;

  /** Full class name for entity object. */
  @NotNull
  @JavaName(type = JavaNameType.Compound)
  private String entityClassName;

  /** Full class name for transformer object to be generated. */
  @JavaName(type = JavaNameType.Compound)
  private String transformerClassName;

  /** Defines the type of objects being transformed (either GRPC or CSV). */
  @NotNull private SourceType sourceType = SourceType.Grpc;

  /** Defines how nullable values are passed to and from field accessor methods. */
  @NotNull
  private NullableFieldAccessorType nullableFieldAccessorType = NullableFieldAccessorType.Standard;

  /** Meta data for the database table. */
  @NotNull @Valid private TableBean table;

  /** Minimum valid string length for non-null string fields. */
  @Builder.Default private int minStringLength = 1;

  /** Meta data for enum types. */
  @NotNull @Singular private List<@Valid EnumTypeBean> enumTypes = new ArrayList<>();

  /** Meta data for transformations used to copy data from message to entity. */
  @NotNull @Singular private List<@Valid TransformationBean> transformations = new ArrayList<>();

  /** Meta data for R4 fhir elements. */
  @Singular private List<@Valid FhirElementBean> r4FhirElements = new ArrayList<>();

  /** Meta data for any external transformations used in transformer. */
  @NotNull @Singular
  private List<@Valid ExternalTransformationBean> externalTransformations = new ArrayList<>();

  /** List of extra interfaces to add to the entity class. */
  @NotNull @Singular
  private List<@JavaName(type = JavaNameType.Compound) String> entityInterfaces = new ArrayList<>();

  /**
   * Finds the {@link EnumTypeBean} in this mapping with the given name and returns it.
   *
   * @param enumName name of the enum
   * @return {@link Optional} containing the {@link EnumTypeBean} for the given name if there was
   *     one, empty otherwise
   */
  public Optional<EnumTypeBean> getEnum(String enumName) {
    return enumTypes.stream().filter(e -> enumName.equals(e.getName())).findAny();
  }

  /**
   * Finds the {@link EnumTypeBean} in this mapping with the given name and returns it.
   *
   * @param enumName name of the enum
   * @return the {@link EnumTypeBean} for the given name
   * @throws IllegalArgumentException if no such enum exists
   */
  public EnumTypeBean findEnum(String enumName) {
    return getEnum(enumName)
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
   * Determines if any array transformations have been defined.
   *
   * @return true if one or more array transformations have been defined
   */
  public boolean hasArrayTransformations() {
    return transformations.stream().anyMatch(TransformationBean::isArray);
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
    return table.getJoins().stream().filter(c -> name.equals(c.getFieldName())).findFirst();
  }

  /**
   * Searches for a {@link TransformationBean} in this entity with the {@link
   * TransformationBean#getTo()} name.
   *
   * @param toName name of the transformed field
   * @return filled optional containing the {@link TransformationBean} if one matches, otherwise
   *     empty
   */
  public Optional<TransformationBean> findTransformationByToName(String toName) {
    return transformations.stream().filter(t -> t.getTo().equals(toName)).findFirst();
  }

  /**
   * Returns an immutable list of all {@link JoinBean} in our {@link TableBean} that are associated
   * with an array transformer.
   *
   * @return list of {@link JoinBean}
   */
  public List<JoinBean> getArrayJoins() {
    final Set<String> arrayFields = getArrayFieldNames();
    return table.getJoins().stream()
        .filter(j -> arrayFields.contains(j.getFieldName()))
        .collect(ImmutableList.toImmutableList());
  }

  /**
   * Returns an immutable list of all {@link JoinBean} in our {@link TableBean} that are not
   * associated with an array transformer.
   *
   * @return filtered list of {@link JoinBean}
   */
  public List<JoinBean> getNonArrayJoins() {
    final Set<String> arrayFields = getArrayFieldNames();
    return table.getJoins().stream()
        .filter(j -> !arrayFields.contains(j.getFieldName()))
        .collect(ImmutableList.toImmutableList());
  }

  /**
   * Builds a set of all field names that are referenced by a {@link TransformationBean} whose
   * {@link TransformationBean#isArray()} method returns true. Intended for use by {@link
   * #getArrayJoins()} and {@link #getNonArrayJoins()}.
   *
   * @return {@link Set} containing all array field names
   */
  private Set<String> getArrayFieldNames() {
    return transformations.stream()
        .filter(TransformationBean::isArray)
        .map(TransformationBean::getTo)
        .collect(Collectors.toSet());
  }

  @Override
  public String getDescription() {
    return "mapping " + id;
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
