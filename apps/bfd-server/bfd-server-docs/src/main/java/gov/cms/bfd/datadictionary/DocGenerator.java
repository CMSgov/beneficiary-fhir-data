package gov.cms.bfd.datadictionary;

import static gov.cms.bfd.datadictionary.util.Version.V1;
import static gov.cms.bfd.datadictionary.util.Version.V2;

import gov.cms.bfd.datadictionary.mappers.CsvMapper;
import gov.cms.bfd.datadictionary.mappers.ExcelMapper;
import gov.cms.bfd.datadictionary.mappers.JsonMapper;
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
   * @param args an array of Strings specifying the project version, destination directory and the
   *     V1, V2 CSV template file names.
   */
  public static void main(String[] args) {

    // validate arguments
    validateArgs(args);

    // name arguments
    var projectVersion = args[0];
    var destinationDirectory = args[1];
    var v1TemplateFilename = args[2];
    var v2TemplateFilename = args[3];

    var templateFileMap = Map.of(V1, v1TemplateFilename, V2, v2TemplateFilename);

    var xlsxFilename =
        String.format("%s/data-dictionary-%s.xlsx", destinationDirectory, projectVersion);

    // Open Excel output here since both API versions are written to the same workbook
    try (var xlsxOutputStream = new FileOutputStream(xlsxFilename);
        var workbook = new XSSFWorkbook()) {

      // Process each data dictionary resource directory in turn
      for (Version version : List.of(V1, V2)) {
        var resourceDirPath = String.format("dd/data/%s", version.name());
        var templatePath = String.format("dd/template/%s", templateFileMap.get(version));

        processDirectory(
            resourceDirPath,
            destinationDirectory,
            projectVersion,
            templatePath,
            xlsxOutputStream,
            workbook,
            version);
      }

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
   * @param xlsxOutputStream the output stream for the Excel workbook
   * @param workbook the Excel workbook (shared across data dictionary versions)
   * @param version the BFD API version, e.g. V1, V2
   */
  public static void processDirectory(
      String resourceDirPath,
      String destinationDirectory,
      String projectVersion,
      String csvTemplatePath,
      FileOutputStream xlsxOutputStream,
      XSSFWorkbook workbook,
      Version version) {

    // name output files
    var basePath = getOutputFileBaseName(destinationDirectory, projectVersion, version);
    var jsonPath = basePath + ".json";
    var csvPath = basePath + ".csv";

    // create mappers and stream
    try (var jsonMapper = JsonMapper.createInstance(new FileWriter(jsonPath));
        var csvMapper = CsvMapper.createInstance(new FileWriter(csvPath), csvTemplatePath);
        var excelMapper = ExcelMapper.createInstance(xlsxOutputStream, workbook, version);
        var elementStream = new FhirElementStream(resourceDirPath).stream()) {

      // stream over elements and write json, csv, excel
      elementStream.map(jsonMapper).map(csvMapper).flatMap(Collection::stream).forEach(excelMapper);

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Validates the program arguments.
   *
   * @param args the project version, destination directory, and V1/V2 CSV template filenames.
   */
  private static void validateArgs(String[] args) {

    // validate number of arguments
    if (args.length != 4) {
      throw new RuntimeException(
          "Project version, destination directory, V1 and V2 CSV template filenames are required.");
    }

    var destinationDirectory = args[1];
    var v1CsvTemplateFilename = args[2];
    var v2CsvTemplateFilename = args[3];

    // validate CSV template files exist
    validateTemplateFile(v1CsvTemplateFilename);
    validateTemplateFile(v2CsvTemplateFilename);

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
   * @param templateFileName the path of the file to validate
   */
  private static void validateTemplateFile(String templateFileName) {
    ClassLoader classLoader = DocGenerator.class.getClassLoader();
    var url = classLoader.getResource("dd/template/" + templateFileName);
    if (url == null || !(new File(url.getFile())).exists()) {
      throw new RuntimeException(
          String.format("CSV template filename (%s) is not valid.", templateFileName));
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
