package gov.cms.model.dsl.codegen.plugin;

import gov.cms.model.dsl.codegen.plugin.mappers.CsvToExcel;
import gov.cms.model.dsl.codegen.plugin.mappers.FhirElementToCsv;
import gov.cms.model.dsl.codegen.plugin.mappers.FhirElementToJson;
import gov.cms.model.dsl.codegen.plugin.model.FhirElement;
import gov.cms.model.dsl.codegen.plugin.model.FhirElementBean;
import gov.cms.model.dsl.codegen.plugin.model.MappingBean;
import gov.cms.model.dsl.codegen.plugin.model.ModelUtil;
import gov.cms.model.dsl.codegen.plugin.model.RootBean;
import gov.cms.model.dsl.codegen.plugin.util.Version;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * A Maven Mojo that generates a data dictionary from yaml mappings. The generated transformer class
 * consists of the following parts:
 */
@Mojo(name = "dataDictionary", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class GenerateDataDictionaryFromDslMojo extends AbstractMojo {

  /** Path to a single mapping file or a directory containing one or more mapping files. */
  @Parameter(property = "mappingPath")
  private String mappingPath;

  /** Project version. */
  @Parameter(property = "projectVersion")
  private String projectVersion;

  /** Path to directory to contain generated data dictionary. */
  @Parameter(property = "destinationDirectory")
  private String destinationDirectory;

  /** V1 CSV template file path. */
  @Parameter(property = "v1TemplateFilePath")
  private String v1TemplateFilePath;

  /** V2 CSV template file path. */
  @Parameter(property = "v2TemplateFilePath")
  private String v2TemplateFilePath;

  /**
   * Instance of {@link MavenProject} used to call {@link
   * MavenProject#addCompileSourceRoot(String)}.
   */
  @Parameter(property = "project", readonly = true)
  private MavenProject project;

  /** Parameterless constructor used by Maven to instantiate the plugin. */
  public GenerateDataDictionaryFromDslMojo() {}

  /**
   * Executed by maven to execute the mojo. Reads all mapping files and generates a data dictionary
   * using fhir elements from every {@link MappingBean}
   *
   * @throws MojoExecutionException if the process fails due to some error
   */
  public void execute() throws MojoExecutionException {
    var templateFileMap = Map.of(Version.V1, v1TemplateFilePath, Version.V2, v2TemplateFilePath);
    var xlsxFilename =
        String.format("%s/data-dictionary-%s.xlsx", destinationDirectory, projectVersion);

    // Open Excel output here since both API versions are written to the same workbook
    try (var xlsxOutputStream = new FileOutputStream(xlsxFilename);
        var workbook = new XSSFWorkbook()) {

      // Process each data dictionary resource directory in turn
      for (Version version : List.of(Version.V1, Version.V2)) {
        var templatePath = templateFileMap.get(version);
        final RootBean root =
            ModelUtil.loadModelFromYamlFileWithAliasesOrDirectory(
                mappingPath); // todo: modify so does V1 vs V2
        MojoUtil.validateModel(root);
        processYamlFiles(
            root, destinationDirectory, projectVersion, templatePath, workbook, version);
      }

      // save Excel workbook file
      workbook.write(xlsxOutputStream);

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Function to encapsulate the processing of a single resource directory.
   *
   * @param root root bean containing mappings of fhir elements
   * @param destinationDirectory the output file path
   * @param projectVersion the project version
   * @param csvTemplatePath the path within the module resources to the CSV template file
   * @param workbook the Excel workbook (shared across data dictionary versions)
   * @param version the BFD API version, e.g. V1, V2
   */
  public static void processYamlFiles(
      RootBean root,
      String destinationDirectory,
      String projectVersion,
      String csvTemplatePath,
      XSSFWorkbook workbook,
      Version version) {

    // name output files
    var basePath = getOutputFileBaseName(destinationDirectory, projectVersion, version);
    var jsonPath = basePath + ".json";
    var csvPath = basePath + ".csv";

    try {
      // create transformers/writers and stream
      var elementToJson = FhirElementToJson.createInstance(new FileWriter(jsonPath));
      var elementToCsv = FhirElementToCsv.createInstance(new FileWriter(csvPath), csvTemplatePath);
      var csvToExcel = CsvToExcel.createInstance(workbook, version);
      List<FhirElementBean> fhirElements = Collections.emptyList();
      for (MappingBean mapping : root.getMappings()) {
        switch (version) {
          case Version.V2:
            fhirElements = mapping.getR4FhirElements();
            break;
          case Version.V1:
            System.out.println("V1 not yet supported");
            break;
        }
        fhirElements.stream()
            .map(
                bean ->
                    FhirElement.builder()
                        .id(bean.getId())
                        .name(bean.getName())
                        .description(bean.getDescription())
                        .appliesTo(bean.getAppliesTo())
                        .suppliedIn(bean.getSuppliedIn())
                        .bfdTableType(bean.getBfdTableType())
                        .bfdColumnName(bean.getBfdColumnName())
                        .bfdDbType(bean.getBfdDbType())
                        .bfdDbSize(bean.getBfdDbSize())
                        .bfdJavaFieldName(bean.getBfdJavaFieldName())
                        .ccwMapping(bean.getCcwMapping())
                        .cclfMapping(bean.getCclfMapping())
                        .fhirMapping(bean.getFhirMapping())
                        .build())
            .map(elementToJson)
            .map(elementToCsv)
            .flatMap(Collection::stream)
            .forEach(csvToExcel);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Forms the base file name for output files.
   *
   * @param destinationDirectory output file destination directory path
   * @param projectVersion the project version
   * @param version the BFD API version
   * @return a String representing the base output file name
   */
  private static String getOutputFileBaseName(
      String destinationDirectory, String projectVersion, Version version) {
    return String.format(
        "%s/%s-data-dictionary-%s", destinationDirectory, version.name(), projectVersion);
  }
}
