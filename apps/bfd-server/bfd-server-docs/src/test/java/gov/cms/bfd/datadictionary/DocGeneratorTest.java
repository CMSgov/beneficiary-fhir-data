package gov.cms.bfd.datadictionary;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** Test cases for the DocGenerator class. */
public class DocGeneratorTest {

  /** Test that the command fails with the wrong number of args (0,1,2,3,4). */
  @Test
  void badArgsNumberTest() {
    assertThrows(
        RuntimeException.class,
        () -> {
          DocGenerator.main(new String[] {});
        });
    assertThrows(
        RuntimeException.class,
        () -> {
          DocGenerator.main(new String[] {"blah", "blah", "blah", "blah"});
        });
  }

  /** Test that the command fails if the V1 CSV template file does not exist. */
  @Test
  void badV1CSVTemplateFilenameTest() {
    assertThrows(
        RuntimeException.class,
        () -> {
          DocGenerator.main(
              new String[] {
                "0.0.1-SNAPSHOT",
                "data-dictionary/data",
                "dist",
                "blah",
                "data-dictionary/template/v2-to-csv.json"
              });
        });
  }

  /** Test that the command fails if the V2 CSV template file does not exist. */
  @Test
  void badV2CSVTemplateFilenameTest() {
    assertThrows(
        RuntimeException.class,
        () -> {
          DocGenerator.main(
              new String[] {
                "0.0.1-SNAPSHOT",
                "data-dictionary/data",
                "dist",
                "data-dictionary/template/v1-to-csv.json",
                "blah"
              });
        });
  }

  /** Test that the command fails if the V2 CSV template file does not exist. */
  @Test
  void badSourceDirectoryTest() {
    assertThrows(
        RuntimeException.class,
        () -> {
          DocGenerator.main(
              new String[] {
                "0.0.1-SNAPSHOT",
                "blah",
                "dist",
                "data-dictionary/template/v1-to-csv.json",
                "data-dictionary/template/v2-to-csv.json"
              });
        });
  }
}
