package gov.cms.bfd.datadictionary.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.cms.bfd.datadictionary.model.FhirElement;
import gov.cms.bfd.datadictionary.util.FhirElementStream;
import gov.cms.bfd.datadictionary.util.Version;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.stream.Stream;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

/** Test cases for the CsvToExcel. */
class CsvToExcelTest {

  /** Test for createInstance method with valid parameters. */
  @Test
  void createInstanceWithValidParameters() {
    var output = new ByteArrayOutputStream();
    var workbook = new XSSFWorkbook();
    var csvToExcel = CsvToExcel.createInstance(output, workbook, Version.V2);
    assertNotNull(csvToExcel);
  }

  /**
   * Test of apply method with valid CSV. Verifies expected rows and headers.
   *
   * @throws IOException upon read/write errors
   */
  @Test
  void applyWithValidCsvExpectMappedRowsAndHeaders() throws IOException {
    var output = new ByteArrayOutputStream();
    var workbook = new XSSFWorkbook();
    var fhirElementToCsv =
        FhirElementToCsv.createInstance(
            new StringWriter(), "src/test/resources/dd/template/v2-to-csv.json");
    var csvToExcel = CsvToExcel.createInstance(output, workbook, Version.V2);

    // create stream
    Stream<FhirElement> stream = new FhirElementStream("src/test/resources/dd/data").stream();

    // first map FhirElement to CSV String then flatten and map with CsvToExcel
    stream.map(fhirElementToCsv).flatMap(Collection::stream).forEach(csvToExcel);

    // assert that 4 rows (including header) were added to V2 sheet (rows/cols start with 0)
    assertEquals("Element ID", workbook.getSheet("V2").getRow(0).getCell(0).toString());
    assertEquals("X", workbook.getSheet("V2").getRow(3).getCell(24).toString());
  }

  /**
   * Test of apply in the case where the Excel workbook is null.
   *
   * @throws IOException upon read/write errors
   */
  @Test
  void applyWithBadWorkbookExpectException() throws IOException {
    assertThrows(
        FileNotFoundException.class,
        () -> {
          var output = new ByteArrayOutputStream();
          var fhirElementToCsv =
              FhirElementToCsv.createInstance(
                  new StringWriter(), "src/test/resources/dd/v2-to-csv.json");
          var csvToExcel = CsvToExcel.createInstance(output, null, Version.V2);
          Stream<FhirElement> stream = new FhirElementStream("src/test/resources/dd/data").stream();
          stream.map(fhirElementToCsv).flatMap(Collection::stream).forEach(csvToExcel);
        });
  }

  /**
   * Test of the close method, which triggers workbook formatting and saving.
   *
   * @throws IOException upon read/write errors
   */
  @Test
  void closeWithValidWorkbookVerifySaveAndFormatting() throws IOException {
    var output = new ByteArrayOutputStream();
    var workbook = new XSSFWorkbook();
    var fhirElementToCsv =
        FhirElementToCsv.createInstance(
            new StringWriter(), "src/test/resources/dd/template/v2-to-csv.json");
    try (var csvToExcel = CsvToExcel.createInstance(output, workbook, Version.V2)) {
      Stream<FhirElement> stream = new FhirElementStream("src/test/resources/dd/data").stream();
      stream.map(fhirElementToCsv).flatMap(Collection::stream).forEach(csvToExcel);
    }

    // assert that workbook was written to output stream
    assertTrue(output.size() > 0);
    // check that formatting was applied
    assertEquals(
        CsvToExcel.CUSTOM_BLUE.getIndex(),
        workbook.getSheet("V2").getRow(0).getCell(0).getCellStyle().getFillForegroundColor());
  }
}
