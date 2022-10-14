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
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
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
      Comparator.comparing(MappingBean::getId);

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

  /** Meta data for arrays. */
  @NotNull @Singular private List<@Valid ArrayBean> arrays = new ArrayList<>();

  /** Meta data for any external transformations used in transformer. */
  @NotNull @Singular
  private List<@Valid ExternalTransformationBean> externalTransformations = new ArrayList<>();

  /** List of extra interfaces to add to the entity class. */
  @NotNull @Singular private List<String> entityInterfaces = new ArrayList<>();

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

  /**
   * Attempts to produce a {@link ColumnBean} suitable for use in defining a database column
   * associated with either a {@link ColumnBean} or a {@link JoinBean} in this mapping.
   *
   * @param root {@link RootBean} containing all known {@link MappingBean} objects
   * @param columnName used to find a suitable {@link ColumnBean} or {@link JoinBean} in this
   *     mapping
   * @return possibly empty {@link Optional} to hold the resulting {@link ColumnBean}
   */
  @Nonnull
  public Optional<ColumnBean> getRealOrJoinedColumnByColumnName(RootBean root, String columnName) {
    return getTable()
        .getColumnByColumnName(columnName)
        .or(() -> getJoinedColumnByColumnName(root, columnName));
  }

  /**
   * Attempts to produce a {@link ColumnBean} suitable for use in defining a database column
   * associated with a {@link JoinBean} in this mapping. This is done by searching for a {@link
   * JoinBean} whose {@link JoinBean#joinColumnName} matches the provided name and then invoking
   * {@link MappingBean#mapJoinToParentColumnBean} for that {@link JoinBean}.
   *
   * @param root {@link RootBean} containing all known {@link MappingBean} objects
   * @param columnName used to find a suitable {@link JoinBean} in this mapping
   * @return possibly empty {@link Optional} to hold the resulting {@link ColumnBean}
   */
  @Nonnull
  public Optional<ColumnBean> getJoinedColumnByColumnName(RootBean root, String columnName) {
    return getNonArrayJoins().stream()
        .filter(join -> join.hasColumnName() && columnName.equals(join.getJoinColumnName()))
        .findAny()
        .flatMap(join -> mapJoinToParentColumnBean(root, join));
  }

  /**
   * Attempts to produce a {@link ColumnBean} suitable for use in defining a database column
   * associated with the specified {@link JoinBean}. This is done by searching for the {@link
   * ColumnBean} with the given joins {@link JoinBean#joinColumnName} in the joins parent mapping.
   * The returned value is {@link Optional} and will be empty if no such column exists for any
   * reason.
   *
   * @param root {@link RootBean} containing all known {@link MappingBean} objects
   * @param join {@link JoinBean} to be mapped
   * @return possibly empty {@link Optional} to hold the resulting {@link ColumnBean}
   */
  public Optional<ColumnBean> mapJoinToParentColumnBean(RootBean root, JoinBean join) {
    var filteredJoin = join.hasColumnName() ? Optional.of(join) : Optional.<JoinBean>empty();
    var parentMapping =
        filteredJoin.flatMap(j -> root.findMappingWithEntityClassName(j.getEntityClass()));
    var parentColumn =
        parentMapping.flatMap(
            pm -> pm.getRealOrJoinedColumnByColumnName(root, join.getJoinColumnName()));
    var noSequenceColumn = parentColumn.map(c -> c.toBuilder().sequence(null).build());
    return noSequenceColumn;
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
