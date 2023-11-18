package gov.cms.bfd.datadictionary.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import gov.cms.bfd.datadictionary.model.FhirElement;
import gov.cms.bfd.datadictionary.util.FhirElementStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

/** Test class for CsvMapper. */
class CsvMapperTest {
  /**
   * Test for the createInstance method.
   *
   * @throws IOException upon write errors
   */
  @Test
  void createInstance() throws IOException {
    var writer = new StringWriter();
    var mapper = CsvMapper.createInstance(writer, "dd/template/v2-to-csv.json");
    assertNotNull(mapper);
  }

  /** Test where the template file does not exist. */
  @Test
  void createInstanceBadTemplate() {
    assertThrows(
        RuntimeException.class,
        () -> {
          var writer = new StringWriter();
          CsvMapper.createInstance(writer, "blah");
        });
  }

  /**
   * Test of apply, occurs during calls to map.
   *
   * @throws IOException upon read/write errors
   */
  @Test
  void apply() throws IOException {
    var writer = new StringWriter();
    var mapper = CsvMapper.createInstance(writer, "dd/template/v2-to-csv.json");
    Stream<FhirElement> stream = new FhirElementStream("dd/data").stream();

    // apply returns stream of lists which can be flattened
    var csv = stream.map(mapper).flatMap(Collection::stream);

    // 3 rows plus the header
    assertEquals(4, csv.count());
    assertEquals(4, StringUtils.countMatches(writer.getBuffer().toString(), "\n"));
  }

  /** Test apply when the writer is null. */
  @Test
  void applyBadWriter() {
    assertThrows(
        RuntimeException.class,
        () -> {
          var mapper = CsvMapper.createInstance(null, "dd/template/v2-to-csv.json");
          Stream<FhirElement> stream = new FhirElementStream("dd/data").stream();
          stream.map(mapper).mapToLong(Collection::size).sum();
        });
  }
}
