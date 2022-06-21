package gov.cms.model.dsl.codegen.plugin.model;

import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
public class TableBean {
  /** Name of the table in the database. */
  private String name;
  /** Name of the schema containing the table. Can be null or empty to use default schema. */
  private String schema;
  /** Optional comment to be added to this table's entity class when it is generated. */
  private String comment;
  /** True if names should be quoted in the JPA annotation arguments for this table. */
  @Builder.Default private boolean quoteNames = true;
  /** True if an {@code equals} method should be generated in the entity class. */
  @Builder.Default private boolean equalsNeeded = true;
  /**
   * Name to use for inner class used for composite primary key if this table has more than one
   * column in the key.
   */
  @Builder.Default private String compositeKeyClassName = "PK";
  /** Names of columns used for primary key. Must all match a column defined in the table. */
  @Singular private List<String> primaryKeyColumns = new ArrayList<>();
  /**
   * Names of columns used for generated {@code equals} method. Must all match a column defined in
   * the table. Can be different than the primary key columns if necessary.
   */
  @Singular private List<String> equalsColumns = new ArrayList<>();
  /** All of the {@link ColumnBean} objects for the columns of this table. */
  @Singular private List<ColumnBean> columns = new ArrayList<>();
  /** All of the {@link JoinBean} objects for the joins involving this table. */
  @Singular private List<JoinBean> joins = new ArrayList<>();

  /**
   * Finds the column with the specified name.
   *
   * @param columnName name value to search for
   * @return the {@link ColumnBean} with the given name
   * @throws IllegalArgumentException if no match is found
   */
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
   * Finds the column with the specified {@link ColumnBean#name} or {@link ColumnBean#dbName}.
   *
   * @param columnName name value to search for
   * @return the {@link ColumnBean} with the given name
   * @throws IllegalArgumentException if no match is found
   */
  public ColumnBean findColumnByNameOrDbName(String columnName) throws IllegalArgumentException {
    return columns.stream()
        .filter(c -> columnName.equals(c.getName()) || columnName.equals(c.getDbName()))
        .findAny()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    String.format(
                        "reference to non-existent column %s in table %s", columnName, name)));
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
}
