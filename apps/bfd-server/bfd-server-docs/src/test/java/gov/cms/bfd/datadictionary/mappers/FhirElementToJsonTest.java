package gov.cms.bfd.datadictionary.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import gov.cms.bfd.datadictionary.model.FhirElement;
import gov.cms.bfd.datadictionary.util.FhirElementStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

/** Tests for the FhirElementToJson class. */
class FhirElementToJsonTest {

  /**
   * Test for the createInstance method with a valid writer.
   *
   * @throws IOException upon read/write errors
   */
  @Test
  void createInstanceWithValidWriter() throws IOException {
    var writer = new StringWriter();
    var mapper = FhirElementToJson.createInstance(writer);
    assertNotNull(mapper);
  }

  /** Test of createInstance when the writer is null. */
  @Test
  void createInstanceWithMissingWriterExpectException() {
    assertThrows(
        RuntimeException.class,
        () -> {
          FhirElementToJson.createInstance(null);
        });
  }

  /**
   * Test of apply, occurs during calls to map.
   *
   * @throws IOException upon read/write errors
   */
  @Test
  void applyWithValidElementsExpectMappedJSON() throws IOException {
    var writer = new StringWriter();
    var mapper = FhirElementToJson.createInstance(writer);
    Stream<FhirElement> stream = new FhirElementStream("src/test/resources/dd/data").stream();

    AtomicInteger count = new AtomicInteger();
    stream
        .map(mapper)
        .forEach(
            e -> {
              count.getAndIncrement();
            });

    // check stream count
    assertEquals(3, count.get());
    // check count of elements written to the output writer
    assertEquals(3, StringUtils.countMatches(writer.getBuffer().toString(), "\"id\" :"));
  }
}
