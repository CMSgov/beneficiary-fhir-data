package gov.cms.model.dsl.codegen.plugin;

import gov.cms.model.dsl.codegen.plugin.model.ColumnBean;
import gov.cms.model.dsl.codegen.plugin.model.MappingBean;
import gov.cms.model.dsl.codegen.plugin.model.ModelUtil;
import gov.cms.model.dsl.codegen.plugin.model.RootBean;
import gov.cms.model.dsl.codegen.plugin.model.TableBean;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * This mojo generates template SQL code for every table in the mapping file. Both CREATE TABLE and
 * ALTER TABLE statements are created for each table. The template file can be used to copy SQL for
 * use in a flyway migration file.
 */
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Mojo(name = "sql", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class GenerateSqlFromDslMojo extends AbstractMojo {
  /** Path to a single mapping file or a directory containing one or more mapping files. */
  @Parameter(property = "mappingPath")
  private String mappingPath;

  /** Path to a single file to hold all of the generated template SQL code. */
  @Parameter(
      property = "sqlFile",
      defaultValue = "${project.build.directory}/generated-sources/entities-schema.sql")
  private String sqlFile;

  /**
   * Executed by maven to execute the mojo. Reads all mapping files and generates template SQL code
   * for every {@link MappingBean}'s {@link MappingBean#entityClassName}.
   *
   * @throws MojoExecutionException if the process fails due to some error
   */
  public void execute() throws MojoExecutionException {
    try {
      File outputFile = new File(this.sqlFile);
      outputFile.getParentFile().mkdirs();
      try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)))) {
        RootBean root = ModelUtil.loadModelFromYamlFileOrDirectory(mappingPath);
        MojoUtil.validateModel(root);
        List<MappingBean> rootMappings = getSortedMappings(root);
        out.println("/*");
        out.println(" ************************** WARNING **************************");
        out.println();
        out.println(
            " * SQL code in this file is intended to serve as a starting point for creating migration files.");
        out.println(
            " * It is should be reviewed manually for performance/optimization when applied to specific use cases.");
        out.println(" */");
        out.println();
        out.println();
        out.println("/************************** CREATES **************************/");
        out.println();
        out.println();
        for (MappingBean mapping : rootMappings) {
          printCreateTableSqlForMapping(root, mapping, out);
          out.println();
        }
        out.println();
        out.println("/************************** ADDS **************************/");
        out.println();
        out.println();
        for (MappingBean mapping : rootMappings) {
          printAddColumnSqlForMapping(mapping, out);
          out.println();
        }
      }
    } catch (IOException ex) {
      throw new MojoExecutionException("I/O error during code generation", ex);
    }
  }

  /**
   * Ensure a predictable order of processing the mappings by sorting them alphabetically.
   *
   * @param root {@link RootBean} containing all known mappings
   * @return list of {@link MappingBean} sorted by id
   */
  private List<MappingBean> getSortedMappings(RootBean root) {
    return root.getMappings().stream()
        .sorted(Comparator.comparing(MappingBean::getId))
        .collect(Collectors.toList());
  }

  /**
   * Writes template {@code CREATE TABLE} SQL to the provided {@link PrintWriter} for the specified
   * {@link MappingBean#table}.
   *
   * @param root {@link RootBean} containing all known mappings
   * @param mapping {@link MappingBean} to create SQL for
   * @param out {@link PrintWriter} to write SQL to
   */
  private void printCreateTableSqlForMapping(RootBean root, MappingBean mapping, PrintWriter out) {
    final var table = mapping.getTable();
    final var primaryKeyColumns = getPrimaryKeyColumns(mapping);
    final var columns = getAllColumns(mapping);
    final var quoteNames = table.isQuoteNames();
    out.println("/*");
    out.print(" * ");
    out.println(table.getName());
    out.println(" */");
    out.print("CREATE TABLE ");
    writeTableName(table, out);
    out.println(" (");
    for (var column : columns) {
      out.print("    ");
      out.print(name(quoteNames, column.getColumnName()));
      out.print(" ");
      out.print(column.getSqlType());
      if (!column.isNullable()) {
        out.print(" NOT NULL");
      }
      out.println(",");
    }
    out.print("    CONSTRAINT ");
    out.print(name(quoteNames, table.getName() + "_key"));
    out.print(" PRIMARY KEY (");
    writeColumnNames(quoteNames, primaryKeyColumns, out);
    out.print(")");
    final var parent = findParent(root, mapping);
    if (parent != null) {
      final var parentPrimaryKeyColumns = getPrimaryKeyColumns(parent);
      out.println(",");
      out.print("    CONSTRAINT ");
      out.print(name(quoteNames, table.getName() + "_parent"));
      out.print(" FOREIGN KEY (");
      writeColumnNames(quoteNames, parentPrimaryKeyColumns, out);
      out.print(") REFERENCES ");
      writeTableName(parent.getTable(), out);
      out.print("(");
      writeColumnNames(quoteNames, parentPrimaryKeyColumns, out);
      out.print(")");
    }
    out.println();
    out.println(");");
  }

  /**
   * Writes template {@code ALTER TABLE ... ADD} SQL to the provided {@link PrintWriter} for the
   * specified {@link MappingBean#table}.
   *
   * @param mapping {@link MappingBean} to create SQL for
   * @param out {@link PrintWriter} to write SQL to
   */
  private void printAddColumnSqlForMapping(MappingBean mapping, PrintWriter out) {
    final var table = mapping.getTable();
    final var columns = getAllColumns(mapping);
    final var quoteNames = table.isQuoteNames();
    out.println("/*");
    out.print(" * ");
    out.println(table.getName());
    out.println(" */");
    for (var column : columns) {
      out.print("ALTER TABLE ");
      writeTableName(table, out);
      out.print(" ADD ");
      out.print(name(quoteNames, column.getColumnName()));
      out.print(" ");
      out.print(column.getSqlType());
      if (!column.isNullable()) {
        out.print(" NOT NULL");
      }
      out.println(";");
    }
  }

  /**
   * Returns a list of {@link ColumnBean} that can be used to generate SQL for primary key columns
   * of a specific {@link MappingBean}. The list will contain columns from any primary key joins
   * followed by regular columns.
   *
   * @param mapping {@link MappingBean} to create SQL for
   * @return list containing {@link ColumnBean} for all primary key columns of the table
   */
  private List<ColumnBean> getPrimaryKeyColumns(MappingBean mapping) {
    // this map ensures no column is included twice
    final var columns = new LinkedHashMap<String, ColumnBean>();
    for (ColumnBean column : mapping.getTable().getPrimaryKeyColumnBeans()) {
      columns.put(column.getColumnName(), column);
    }
    return List.copyOf(columns.values());
  }

  /**
   * Returns a list of {@link ColumnBean} that can be used to generate SQL for all columns of a
   * specific {@link MappingBean}. The list will contain columns from any non-array joins followed
   * by regular columns.
   *
   * @param mapping {@link MappingBean} to create SQL for
   * @return list containing {@link ColumnBean} for all columns in the table
   */
  private List<ColumnBean> getAllColumns(MappingBean mapping) {
    // this map ensures no column is included twice
    final var columns = new LinkedHashMap<String, ColumnBean>();
    for (ColumnBean column : mapping.getTable().getColumns()) {
      columns.put(column.getColumnName(), column);
    }
    return List.copyOf(columns.values());
  }

  /**
   * Writes the names of the specified columns (separated by commas) to the provided {@link
   * PrintWriter}.
   *
   * @param quoted causes the names to be wrapped in quotes when true
   * @param columns list of columns to write the names of
   * @param out {@link PrintWriter} to write name to
   */
  private void writeColumnNames(boolean quoted, List<ColumnBean> columns, PrintWriter out) {
    final var names = columns.stream().map(ColumnBean::getColumnName).collect(Collectors.toList());
    writeNames(quoted, names, out);
  }

  /**
   * Writes the name of the table (including schema name if any) to the provided {@link
   * PrintWriter}.
   *
   * @param table {@link TableBean} to write name of
   * @param out {@link PrintWriter} to write name to
   */
  private void writeTableName(TableBean table, PrintWriter out) {
    if (table.hasSchema()) {
      out.print(name(table.isQuoteNames(), table.getSchema()));
      out.print(".");
    }
    out.print(name(table.isQuoteNames(), table.getName()));
  }

  /**
   * Writes the specified names (separated by commas) to the provided {@link PrintWriter}.
   *
   * @param quoted causes the names to be wrapped in quotes when true
   * @param names list of names to write
   * @param out {@link PrintWriter} to write name to
   */
  private void writeNames(boolean quoted, List<String> names, PrintWriter out) {
    for (int i = 0; i < names.size(); ++i) {
      if (i > 0) {
        out.print(", ");
      }
      out.print(name(quoted, names.get(i)));
    }
  }

  /**
   * Searches all known {@link MappingBean}s to find one that contains an array of objects defined
   * by the specified {@link MappingBean}. If one is found it is returned. Otherwise {@code null} is
   * returned.
   *
   * @param root {@link RootBean} containing all known mappings
   * @param mapping {@link MappingBean} to find parent of
   * @return the parent mapping or {@code null} if no parent exists
   */
  private MappingBean findParent(RootBean root, MappingBean mapping) {
    final var parent =
        root.getMappings().stream()
            .filter(
                m ->
                    m.getArrayJoins().stream()
                        .anyMatch(a -> a.getEntityMapping().equals(mapping.getId())))
            .findFirst();
    return parent.orElse(null);
  }

  /**
   * Wraps the provided string in quotes if the flag is true. Otherwise returns the string
   * unchanged.
   *
   * @param quoted causes the string to be wrapped in quotes when true
   * @param value string to wrap
   * @return the (possibly quoted) string
   */
  private String name(boolean quoted, String value) {
    return quoted ? "\"" + value + "\"" : value;
  }
}
