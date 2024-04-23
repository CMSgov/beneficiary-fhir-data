package gov.cms.model.dsl.codegen.plugin.model;

import com.google.common.base.Strings;
import gov.cms.model.dsl.codegen.plugin.model.validation.JavaName;
import jakarta.annotation.Nonnull;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;

/** Model object containing attributes of a table in the database. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TableBean implements ModelBean {
  /** Name of the table in the database. */
  @NotNull @JavaName private String name;

  /** Name of the schema containing the table. Can be null to use default schema. */
  @JavaName private String schema;

  /** Optional comment to be added to this table's entity class when it is generated. */
  private String comment;

  /** True if names should be quoted in the JPA annotation arguments for this table. */
  @Builder.Default private boolean quoteNames = false;

  /** True if an {@code equals} method should be generated in the entity class. */
  @Builder.Default private boolean equalsNeeded = true;

  /**
   * Name to use for inner class used for composite primary key if this table has more than one
   * column in the key.
   */
  @NotNull @JavaName @Builder.Default private String compositeKeyClassName = "PK";

  /**
   * Names of primary key components. Every name must match either a {@link ColumnBean#name} or a
   * {@link JoinBean#fieldName} from this table.
   */
  @NotNull @Singular private List<@JavaName String> primaryKeyColumns = new ArrayList<>();

  /**
   * Names of columns used for generated {@code equals} method. Every name must match a {@link
   * ColumnBean#name} from this table. Can be different than the primary key columns if necessary.
   */
  @NotNull @Singular private List<@JavaName String> equalsColumns = new ArrayList<>();

  /** All of the {@link ColumnBean} objects for the columns of this table. */
  @NotNull @Singular private List<@Valid ColumnBean> columns = new ArrayList<>();

  /** All of the {@link JoinBean} objects for the joins involving this table. */
  @NotNull @Singular private List<@Valid JoinBean> joins = new ArrayList<>();

  /** List of additional fields to add to the lombok generated {@code Fields} class. */
  @NotNull @Singular
  private List<@Valid AdditionalFieldName> additionalFieldNames = new ArrayList<>();

  /**
   * Finds the column with the specified name.
   *
   * @param columnName name value to search for
   * @return the {@link ColumnBean} with the given name
   * @throws IllegalArgumentException if no match is found
   */
  @Nonnull
  public ColumnBean findColumnByName(String columnName) throws IllegalArgumentException {
    return columns.stream()
        .filter(c -> columnName.equals(c.getName()))
        .findAny()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    String.format(
                        "reference to non-existent column %s in table %s", columnName, name)));
  }

  /**
   * Finds the column with the specified {@link ColumnBean#name}.
   *
   * @param columnName name value to search for
   * @return a (possibly empty) {@link Optional} containing the the {@link ColumnBean} with the
   *     given name
   */
  @Nonnull
  public Optional<ColumnBean> getColumnByName(String columnName) {
    return columns.stream().filter(c -> columnName.equals(c.getName())).findAny();
  }

  /**
   * Determines if this object has a non-empty comment string.
   *
   * @return true if this object has a non-empty comment string
   */
  public boolean hasComment() {
    return !Strings.isNullOrEmpty(comment);
  }

  /**
   * Determines if this object has a non-empty schema name.
   *
   * @return true if this object has a non-empty schema name
   */
  public boolean hasSchema() {
    return !Strings.isNullOrEmpty(schema);
  }

  /**
   * Determines if this object has at least one primary key column defined.
   *
   * @return true if this object has at least one primary key column defined
   */
  public boolean hasPrimaryKey() {
    return primaryKeyColumns.size() > 0;
  }

  /**
   * Determines if a column with the specified name is part of the primary key.
   *
   * @param name name of column
   * @return true if a column with the specified name is part of the primary key
   */
  public boolean isPrimaryKey(String name) {
    return primaryKeyColumns.stream().anyMatch(fieldName -> fieldName.equals(name));
  }

  /**
   * Determines if the specified {@link JoinBean} is on a field that is part of the primary key.
   *
   * @param join a {@link JoinBean} to check
   * @return true if the specified {@link JoinBean} is on a field that is part of the primary key
   */
  public boolean isPrimaryKey(JoinBean join) {
    return join.getJoinType().isSingleValue() && isPrimaryKey(join.getFieldName());
  }

  /**
   * Returns a list of all {@link ColumnBean} objects that are part of the primary key.
   *
   * @return a {@link List} of {@link ColumnBean}
   */
  public List<ColumnBean> getPrimaryKeyColumnBeans() {
    return columns.stream().filter(c -> isPrimaryKey(c.getName())).collect(Collectors.toList());
  }

  /**
   * Creates a temporary {@link Set} containing the names of all of the columns that should be
   * included in the generated {@link Object#equals} method.
   *
   * @return a {@link Set} containing the names of all of the columns that should be included in the
   *     generated {@link Object#equals} method.
   */
  public Set<String> getColumnsForEqualsMethod() {
    if (equalsNeeded) {
      var columnList = equalsColumns.isEmpty() ? primaryKeyColumns : equalsColumns;
      return Set.copyOf(columnList);
    } else {
      return Set.of();
    }
  }

  /**
   * Wrap the name in JPA quotes if this table requires quoted names. Otherwise return the name
   * unchanged.
   *
   * @param name name to quote
   * @return quoted or unchanged name
   */
  public String quoteName(String name) {
    return isQuoteNames() ? "`" + name + "`" : name;
  }

  @Override
  public String getDescription() {
    return "table " + name;
  }

  /**
   * Used by validator to verify that every primary key name matches either a column name or a join
   * field name in this table.
   *
   * @return true if all names were matched
   */
  @AssertTrue(message = "primaryKey")
  public boolean isEveryPrimaryKeyColumnValid() {
    final var unmatched = new HashSet<>(primaryKeyColumns);
    columns.forEach(c -> unmatched.remove(c.getName()));
    joins.forEach(j -> unmatched.remove(j.getFieldName()));
    return unmatched.isEmpty();
  }

  /**
   * Used to allow definition of extra field names for lombok to add to its auto-generated {@code
   * Fields} constant in the generated entity class.
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class AdditionalFieldName {
    /** Name of the constant added to the {@code Fields} class. */
    @JavaName private String name;

    /**
     * Optional value to use instead of {@code name} as the value of the constant in the {@code
     * Fields} class.
     */
    @JavaName private String value;

    /**
     * Gets the value to use in the generated constant. Defaults to the same as {@link
     * AdditionalFieldName#name}.
     *
     * @return either {@link AdditionalFieldName#name} or {@link AdditionalFieldName#value}
     */
    public String getFieldValue() {
      return Strings.isNullOrEmpty(value) ? name : value;
    }
  }
}
