package gov.cms.bfd.datadictionary;

import static gov.cms.bfd.datadictionary.util.Version.V1;
import static gov.cms.bfd.datadictionary.util.Version.V2;

import gov.cms.bfd.datadictionary.mappers.CsvToExcel;
import gov.cms.bfd.datadictionary.mappers.FhirElementToCsv;
import gov.cms.bfd.datadictionary.mappers.FhirElementToJson;
import gov.cms.bfd.datadictionary.util.FhirElementStream;
import gov.cms.bfd.datadictionary.util.Version;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/** Generates JSON, CSV and Excel .xlsx files from individual FHIR element JSON resource files. */
public class DocGenerator {

  /**
   * Program entry point which drives the document generation.
   *
   * @param args an array of Strings specifying the project version, source directory, destination
   *     directory and the V1, V2 CSV template file paths.
   */
  public static void main(String[] args) {

    // validate arguments
    validateArgs(args);

    // name arguments
    var projectVersion = args[0];
    var sourceDirectory = args[1];
    var destinationDirectory = args[2];
    var v1TemplateFilePath = args[3];
    var v2TemplateFilePath = args[4];

    var templateFileMap = Map.of(V1, v1TemplateFilePath, V2, v2TemplateFilePath);

    var xlsxFilename =
        String.format("%s/data-dictionary-%s.xlsx", destinationDirectory, projectVersion);

    // Open Excel output here since both API versions are written to the same workbook
    try (var xlsxOutputStream = new FileOutputStream(xlsxFilename);
        var workbook = new XSSFWorkbook()) {

      // Process each data dictionary resource directory in turn
      for (Version version : List.of(V1, V2)) {
        var resourceDirPath = String.format("%s/%s", sourceDirectory, version.name());
        var templatePath = templateFileMap.get(version);

        processDirectory(
            resourceDirPath, destinationDirectory, projectVersion, templatePath, workbook, version);
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
   * @param resourceDirPath the path within the module resources to the data dictionary json files
   * @param destinationDirectory the output file path
   * @param projectVersion the project version
   * @param csvTemplatePath the path within the module resources to the CSV template file
   * @param workbook the Excel workbook (shared across data dictionary versions)
   * @param version the BFD API version, e.g. V1, V2
   */
  public static void processDirectory(
      String resourceDirPath,
      String destinationDirectory,
      String projectVersion,
      String csvTemplatePath,
      XSSFWorkbook workbook,
      Version version) {

    // name output files
    var basePath = getOutputFileBaseName(destinationDirectory, projectVersion, version);
    var jsonPath = basePath + ".json";
    var csvPath = basePath + ".csv";

    // create transformers/writers and stream
    try (var elementToJson = FhirElementToJson.createInstance(new FileWriter(jsonPath));
        var elementToCsv =
            FhirElementToCsv.createInstance(new FileWriter(csvPath), csvTemplatePath);
        var csvToExcel = CsvToExcel.createInstance(workbook, version);
        var elementStream = new FhirElementStream(resourceDirPath).stream()) {

      // stream over elements and write json, csv, excel
      elementStream
          .map(elementToJson)
          .map(elementToCsv)
          .flatMap(Collection::stream)
          .forEach(csvToExcel);

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Validates the program arguments.
   *
   * @param args the project version, destination directory, and V1/V2 CSV template file paths.
   */
  private static void validateArgs(String[] args) {

    // validate number of arguments
    if (args.length != 5) {
      throw new RuntimeException(
          "Project version, source directory, destination directory, V1 and V2 CSV template file paths are required.");
    }

    var sourceDirectory = args[1];
    var destinationDirectory = args[2];
    var v1CsvTemplateFilePath = args[3];
    var v2CsvTemplateFilePath = args[4];

    // validate CSV template files exist
    validateTemplateFile(v1CsvTemplateFilePath);
    validateTemplateFile(v2CsvTemplateFilePath);

    // validate source directory
    var sourceDirectoryFile = new File(sourceDirectory);
    if (!sourceDirectoryFile.exists()) {
      throw new RuntimeException(
          String.format("Source directory (%s) is not valid.", sourceDirectory));
    }

    // validate destination directory, try to create if does not exist
    var destinationDirectoryFile = new File(destinationDirectory);
    if (!destinationDirectoryFile.exists()) {
      if (!destinationDirectoryFile.mkdirs()) {
        throw new RuntimeException(
            String.format("Destination directory (%s) is not valid.", destinationDirectory));
      }
    }
  }

  /**
   * Validates that a given template file exists.
   *
   * @param templateFilePath the path of the file to validate
   */
  private static void validateTemplateFile(String templateFilePath) {
    if (!(new File(templateFilePath)).exists()) {
      throw new RuntimeException(
          String.format("CSV template filename (%s) is not valid.", templateFilePath));
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
