package gov.cms.bfd.datadictionary.mappers;

import static org.junit.jupiter.api.Assertions.*;

import gov.cms.bfd.datadictionary.model.FhirElement;
import gov.cms.bfd.datadictionary.util.FhirElementStream;
import gov.cms.bfd.datadictionary.util.Version;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.stream.Stream;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

/** Test cases for the ExcelMapper. */
class ExcelMapperTest {

  /** Test for createInstance method. */
  @Test
  void createInstance() {
    var output = new ByteArrayOutputStream();
    var workbook = new XSSFWorkbook();
    var excelMapper = ExcelMapper.createInstance(output, workbook, Version.V2);
    assertNotNull(excelMapper);
  }

  /**
   * Test of the apply, occurs during calls to map.
   *
   * @throws IOException upon read/write errors
   */
  @Test
  void apply() throws IOException {
    var output = new ByteArrayOutputStream();
    var workbook = new XSSFWorkbook();
    var csvMapper = CsvMapper.createInstance(new StringWriter(), "dd/template/v2-to-csv.json");
    var excelMapper = ExcelMapper.createInstance(output, workbook, Version.V2);
    Stream<FhirElement> stream = new FhirElementStream("dd/data").stream();

    // first map FhirElement to CSV String then flatten and map with ExcelMapper
    stream.map(csvMapper).flatMap(Collection::stream).forEach(excelMapper);

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
  void applyBadWorkbook() throws IOException {
    assertThrows(
        RuntimeException.class,
        () -> {
          var output = new ByteArrayOutputStream();
          var csvMapper =
              CsvMapper.createInstance(new StringWriter(), "dd/template/v2-to-csv.json");
          var excelMapper = ExcelMapper.createInstance(output, null, Version.V2);
          Stream<FhirElement> stream = new FhirElementStream("dd/data").stream();
          stream.map(csvMapper).flatMap(Collection::stream).forEach(excelMapper);
        });
  }

  /**
   * Test of the close method, which triggers workbook formatting and saving.
   *
   * @throws IOException upon read/write errors
   */
  @Test
  void close() throws IOException {
    var output = new ByteArrayOutputStream();
    var workbook = new XSSFWorkbook();
    var csvMapper = CsvMapper.createInstance(new StringWriter(), "dd/template/v2-to-csv.json");
    try (var excelMapper = ExcelMapper.createInstance(output, workbook, Version.V2)) {
      Stream<FhirElement> stream = new FhirElementStream("dd/data").stream();
      stream.map(csvMapper).flatMap(Collection::stream).forEach(excelMapper);
    }

    // assert that workbook was written to output stream
    assertTrue(output.size() > 0);
    // check that formatting was applied
    assertEquals(
        ExcelMapper.CUSTOM_BLUE.getIndex(),
        workbook.getSheet("V2").getRow(0).getCell(0).getCellStyle().getFillForegroundColor());
  }
}
