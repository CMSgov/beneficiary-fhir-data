package gov.cms.model.dsl.codegen.plugin.mappers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import gov.cms.model.dsl.codegen.plugin.model.FhirElement;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

/** Processor for a stream of FhirElements. Converts FhirElements to CSV format and writes them. */
public class FhirElementToCsv implements Function<FhirElement, List<String>>, Closeable {

  /** ObjectMapper for deserializing CSV template JSON. */
  private static final ObjectMapper objectMapper = JsonMapper.builder().build();

  /** Map of field -> header values from CSV template. */
  private Map<String, String> fields;

  /** Printer to write CSV formatted Strings. */
  private CSVPrinter csvPrinter;

  /** Writer to capture CSV content. */
  private final StringWriter stringWriter;

  /** Writer to persist CSV content. */
  private final Writer fileWriter;

  /** Flag indicating first element has been processed. */
  private boolean started;

  /**
   * Factory method to create instances of FhirElementToCsv.
   *
   * @param writer the writer to use to persist the generated CSV content
   * @param templatePath the CSV template file
   * @return an instance of FhirElementToCsv
   * @throws IOException on file errors
   */
  public static FhirElementToCsv createInstance(Writer writer, String templatePath)
      throws IOException {
    var FhirElementToCsv = new FhirElementToCsv(writer);
    FhirElementToCsv.init(templatePath);
    return FhirElementToCsv;
  }

  /**
   * Converts a FhirElement to a list of CSV formatted strings and persists.
   *
   * @param element the FhirElement to transform and save
   * @return a List of CSV formatted String values
   */
  @Override
  public List<String> apply(FhirElement element) {
    // returns list as the first row returns more than one line.
    try {
      if (started) {
        var row = writeElement(element);
        return List.of(row);
      } else {
        var header = writeHeader();
        var row = writeElement(element);
        started = true;
        return List.of(header, row);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Close resources held by the mapper.
   *
   * @throws IOException upon errors closing resources
   */
  @Override
  public void close() throws IOException {
    csvPrinter.close();
    fileWriter.close();
  }

  /**
   * Private constructor.
   *
   * @param fileWriter the writer to use to persist the generated CSV content.
   */
  private FhirElementToCsv(Writer fileWriter) {
    this.fileWriter = fileWriter;
    stringWriter = new StringWriter();
  }

  /**
   * Initialize a CSV mapper.
   *
   * @param templatePath the path of the CSV template file to use
   * @throws IOException upon errors opening or reading files
   */
  private void init(String templatePath) throws IOException {
    csvPrinter = new CSVPrinter(stringWriter, CSVFormat.EXCEL);
    fields = readTemplate(templatePath);
  }

  /**
   * Write the CSV column headings extracted from the template file.
   *
   * @return the value of the CSV line written
   * @throws IOException upon write errors
   */
  private String writeHeader() throws IOException {
    var titles = new ArrayList<>(fields.values().size());
    titles.addAll(fields.values());
    return writeCsv(titles);
  }

  /**
   * Write the CSV row for a given FhirElement.
   *
   * @param element the FhirElement to transform and write
   * @return the value of the CSV line written
   * @throws IOException upon write errors
   */
  private String writeElement(FhirElement element) throws IOException {
    var row = new ArrayList<>();
    for (String field : fields.keySet()) {
      var fieldValue = getFieldValue(element, field);
      row.add(fieldValue);
    }
    return writeCsv(row);
  }

  /**
   * Reads the CSV template file into a map of field names and descriptions.
   *
   * @param templatePath the String file path to the CSV template file
   * @return a Map of field names to descriptions in read order
   * @throws IOException upon file open or read errors
   */
  private Map<String, String> readTemplate(String templatePath) throws IOException {
    var fields = new LinkedHashMap<String, String>();
    File template = new File(templatePath);
    JsonNode root = objectMapper.readTree(template);
    JsonNode node = root.get("fields");
    if (node.isArray()) {
      var iter = node.elements();
      while (iter.hasNext()) {
        var field = iter.next();
        fields.put(field.findValue("field").asText(), field.findValue("title").asText());
      }
    }
    return fields;
  }

  /**
   * Helper function to write a CSV file row.
   *
   * @param row a Collection of Objects to write in CSV format
   * @return the value of the CSV line written
   * @throws IOException upon write errors
   */
  private String writeCsv(Collection<Object> row) throws IOException {
    csvPrinter.printRecord(row); // write csv to string writer to capture
    var csv = stringWriter.toString();
    stringWriter.getBuffer().setLength(0); // reset string writer buffer
    fileWriter.write(csv); // write csv to file
    return csv;
  }

  /**
   * Extract and format the value of a FhirElement given a field name.
   *
   * @param element the FhirElement to retrieve values from
   * @param field the name of the field to retrieve
   * @return an Object (String or Integer) retrieved from the FhirElement
   */
  private Object getFieldValue(FhirElement element, String field) {
    return switch (field) {
      case "id" -> element.getId();
      case "name" -> element.getName();
      case "description" -> element.getDescription();
      case "suppliedIn" -> element.getSuppliedIn();
      case "bfdDbType" -> element.getBfdDbType();
      case "bfdDbSize" -> element.getBfdDbSize();
      case "appliesTo" -> String.join(";", element.getAppliesTo());
      case "ccwMapping" -> String.join(";", element.getCcwMapping());
      case "cclfMapping" -> String.join(";", element.getCclfMapping());
      case "resource" -> element.getFhirMapping().get(0).getResource();
      case "element" -> element.getFhirMapping().get(0).getElement();
      case "derived" -> element.getFhirMapping().get(0).getDerived();
      case "note" -> element.getFhirMapping().get(0).getNote();
      case "fhirPath" -> element.getFhirMapping().get(0).getFhirPath();
      case "example" -> element.getFhirMapping().get(0).getExample();
      case "version" -> element.getFhirMapping().get(0).getVersion();
      case "discriminator" -> String.join(";", element.getFhirMapping().get(0).getDiscriminator());
      case "additional" -> String.join(";", element.getFhirMapping().get(0).getAdditional());
      case "AB2D", "BB2", "BCDA", "BFD", "DPC", "SyntheticData" -> element
              .getSuppliedIn()
              .contains(field)
          ? "X"
          : "";
      case "bfdTableType" -> element.getBfdTableType();
      case "bfdColumnName" -> element.getBfdColumnName();
      case "bfdJavaFieldName" -> element.getBfdJavaFieldName();
      default -> null;
    };
  }
}
