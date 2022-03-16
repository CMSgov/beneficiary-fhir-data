package gov.cms.model.rda.codegen.plugin;

import static gov.cms.model.rda.codegen.plugin.model.ModelUtil.isValidMappingSource;

import gov.cms.model.rda.codegen.plugin.model.MappingBean;
import gov.cms.model.rda.codegen.plugin.model.ModelUtil;
import gov.cms.model.rda.codegen.plugin.model.RootBean;
import gov.cms.model.rda.codegen.plugin.model.TableBean;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import lombok.SneakyThrows;
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
public class RdaSqlCodeGenMojo extends AbstractMojo {
  @Parameter(property = "mappingFile")
  private String mappingFile;

  @Parameter(
      property = "outputFile",
      defaultValue = "${project.build.directory}/generated-sources/entities-schema.sql")
  private String outputFile;

  @SneakyThrows(IOException.class)
  public void execute() throws MojoExecutionException {
    if (!isValidMappingSource(mappingFile)) {
      fail("mappingFile not defined or does not exist");
    }

    File outputFile = new File(this.outputFile);
    outputFile.getParentFile().mkdirs();
    try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)))) {
      RootBean root = ModelUtil.loadMappingsFromYamlFile(mappingFile);
      List<MappingBean> rootMappings = root.getMappings();
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
  }

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

  private void writeTableName(TableBean table, PrintWriter out) {
    if (table.hasSchema()) {
      out.print(quoted(table.getSchema()));
      out.print(".");
    }
    out.print(quoted(table.getName()));
  }

  private void writeNames(List<String> names, PrintWriter out) {
    for (int i = 0; i < names.size(); ++i) {
      if (i > 0) {
        out.print(", ");
      }
      out.print(quoted(names.get(i)));
    }
  }

  private TableBean findParent(RootBean root, MappingBean mapping) {
    var parent =
        root.getMappings().stream()
            .filter(
                m -> m.getArrays().stream().anyMatch(a -> a.getMapping().equals(mapping.getId())))
            .findFirst()
            .map(m -> m.getTable());
    return parent.orElse(null);
  }

  private String quoted(String value) {
    return "\"" + value + "\"";
  }

  private void fail(String formatString, Object... args) throws MojoExecutionException {
    String message = String.format(formatString, args);
    throw new MojoExecutionException(message);
  }
}
