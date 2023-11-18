package gov.cms.bfd.datadictionary.util;

import static org.junit.jupiter.api.Assertions.*;

import gov.cms.bfd.datadictionary.model.FhirElement;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Test cases for the FhirElementStream. */
class FhirElementStreamTest {

  /** Verify that the stream method operates correctly. */
  @Test
  void stream() {
    Stream<FhirElement> stream = new FhirElementStream("dd/data").stream();
    assertNotNull(stream);
    assertEquals(3, stream.count());
  }
}
