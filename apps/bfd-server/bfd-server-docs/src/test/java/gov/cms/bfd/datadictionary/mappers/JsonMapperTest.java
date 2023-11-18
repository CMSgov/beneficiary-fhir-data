package gov.cms.bfd.datadictionary.mappers;

import static org.junit.jupiter.api.Assertions.*;

import gov.cms.bfd.datadictionary.model.FhirElement;
import gov.cms.bfd.datadictionary.util.FhirElementStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

/** Tests for the JsonMapper class. */
class JsonMapperTest {

  /**
   * Test for the createInstance method.
   *
   * @throws IOException upon read/write errors
   */
  @Test
  void createInstance() throws IOException {
    var writer = new StringWriter();
    var mapper = JsonMapper.createInstance(writer);
    assertNotNull(mapper);
  }

  /** Test of createInstance when the writer is null. */
  @Test
  void createInstanceBadWriter() {
    assertThrows(
        RuntimeException.class,
        () -> {
          JsonMapper.createInstance(null);
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
    var mapper = JsonMapper.createInstance(writer);
    Stream<FhirElement> stream = new FhirElementStream("dd/data").stream();

    AtomicInteger count = new AtomicInteger();
    stream
        .map(mapper)
        .forEach(
            e -> {
              count.getAndIncrement();
            });

    assertEquals(3, count.get());
    assertEquals(3, StringUtils.countMatches(writer.getBuffer().toString(), "\"id\" :"));
  }
}
