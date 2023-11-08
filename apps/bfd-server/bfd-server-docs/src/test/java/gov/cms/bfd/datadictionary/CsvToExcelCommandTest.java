package gov.cms.bfd.datadictionary;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** Tests for the CsvToExcelCommand class. */
class CsvToExcelCommandTest {

  /** Test that the command fails with the wrong number of args (0,1,2). */
  @Test
  void badArgsNumberTest() {
    assertThrows(
        RuntimeException.class,
        () -> {
          CsvToExcelCommand.main(new String[] {});
        });
    assertThrows(
        RuntimeException.class,
        () -> {
          CsvToExcelCommand.main(new String[] {"blah"});
        });
    assertThrows(
        RuntimeException.class,
        () -> {
          CsvToExcelCommand.main(new String[] {"blah", "blah"});
        });
  }

  /** Test that the command fails if the V1 CSV file does not exist. */
  @Test
  void badV1CSVFilenameTest() {
    assertThrows(
        RuntimeException.class,
        () -> {
          CsvToExcelCommand.main(new String[] {"blah", "blah", "blah"});
        });
  }

  /** Test that the command fails if the V2 CSV does not exist. */
  @Test
  void badV2CSVFilenameTest() {
    assertThrows(
        RuntimeException.class,
        () -> {
          CsvToExcelCommand.main(new String[] {"pom.xml", "blah", "blah"});
        });
  }
}
