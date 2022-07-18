package gov.cms.model.dsl.codegen.plugin;

import com.google.common.annotations.VisibleForTesting;
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
import java.util.List;
import java.util.stream.Collectors;
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
@Mojo(name = "sql", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class GenerateSqlFromDslMojo extends AbstractMojo {
  /** Path to a single mapping file or a directory containing one or more mapping files. */
  @Parameter(property = "mappingPath")
  private String mappingPath;

  /** Path to a single file to hold all of the generated template SQL code. */
  @Parameter(
      property = "outputFile",
      defaultValue = "${project.build.directory}/generated-sources/entities-schema.sql")
  private String outputFile;

  /** Parameterless constructor used by Maven to instantiate the plugin. */
  public GenerateSqlFromDslMojo() {}

  /**
   * All fields constructor for use in unit tests.
   *
   * @param mappingPath path to file or directory containing mappings
   * @param outputFile path to file to contain generated code
   */
  @VisibleForTesting
  GenerateSqlFromDslMojo(String mappingPath, String outputFile) {
    this.mappingPath = mappingPath;
    this.outputFile = outputFile;
  }

  /**
   * Executed by maven to execute the mojo. Reads all mapping files and generates template SQL code
   * for every {@link MappingBean}'s {@link MappingBean#entityClassName}.
   *
   * @throws MojoExecutionException if the process fails due to some error
   */
  public void execute() throws MojoExecutionException {
    try {
      File outputFile = new File(this.outputFile);
      outputFile.getParentFile().mkdirs();
      try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)))) {
        RootBean root = ModelUtil.loadModelFromYamlFileOrDirectory(mappingPath);
        List<MappingBean> rootMappings = getSortedMappings(root);
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
    out.println("/*");
    out.print(" * ");
    out.println(table.getName());
    out.println(" */");
    out.print("CREATE TABLE ");
    writeTableName(table, out);
    out.println(" (");
    for (var column : table.getColumns()) {
      out.print("    ");
      out.print(quoted(column.getName()));
      out.print(" ");
      out.print(column.getSqlType());
      if (!column.isNullable()) {
        out.print(" NOT NULL");
      }
      out.println(",");
    }
    out.print("    CONSTRAINT ");
    out.print(quoted(table.getName() + "_key"));
    out.print(" PRIMARY KEY (");
    writeNames(table.getPrimaryKeyColumns(), out);
    out.print(")");
    final var parent = findParent(root, mapping);
    if (parent != null) {
      out.println(",");
      out.print("    CONSTRAINT ");
      out.print(quoted(table.getName() + "_parent"));
      out.print(" FOREIGN KEY (");
      writeNames(parent.getPrimaryKeyColumns(), out);
      out.print(") REFERENCES ");
      writeTableName(parent, out);
      out.print("(");
      writeNames(parent.getPrimaryKeyColumns(), out);
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
    out.println("/*");
    out.print(" * ");
    out.println(table.getName());
    out.println(" */");
    for (var column : table.getColumns()) {
      out.print("ALTER TABLE ");
      writeTableName(table, out);
      out.print(" ADD ");
      out.print(quoted(column.getName()));
      out.print(" ");
      out.print(column.getSqlType());
      if (!column.isNullable()) {
        out.print(" NOT NULL");
      }
      out.println(";");
    }
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
      out.print(quoted(table.getSchema()));
      out.print(".");
    }
    out.print(quoted(table.getName()));
  }

  /**
   * Writes the specified names (separated by commas) to the provided {@link PrintWriter}.
   *
   * @param names list of names to write
   * @param out {@link PrintWriter} to write name to
   */
  private void writeNames(List<String> names, PrintWriter out) {
    for (int i = 0; i < names.size(); ++i) {
      if (i > 0) {
        out.print(", ");
      }
      out.print(quoted(names.get(i)));
    }
  }

  /**
   * Searches all known {@link MappingBean}s to find one that contains an array of objects defined
   * by the specified {@link MappingBean}. If one is found its {@link TableBean} is returned.
   * Otherwise {@code null} is returned.
   *
   * @param root {@link RootBean} containing all known mappings
   * @param mapping {@link MappingBean} to find parent of
   * @return the parent mapping's {@link TableBean} or {@code null} if no parent exists
   */
  private TableBean findParent(RootBean root, MappingBean mapping) {
    var parent =
        root.getMappings().stream()
            .filter(
                m -> m.getArrays().stream().anyMatch(a -> a.getMapping().equals(mapping.getId())))
            .findFirst()
            .map(m -> m.getTable());
    return parent.orElse(null);
  }

  /**
   * Wraps the provided string in quotes.
   *
   * @param value string to wrap
   * @return the wrapped string
   */
  private String quoted(String value) {
    return "\"" + value + "\"";
  }
}
