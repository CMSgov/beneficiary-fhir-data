package gov.cms.bfd.pipeline.sharedutils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

public class IdHasherTest {
  @Test
  public void testHashing() {
    IdHasher hasher =
        new IdHasher(
            new IdHasher.Config(1000, "nottherealpepper".getBytes(StandardCharsets.UTF_8)));
    assertEquals(
        "d95a418b0942c7910fb1d0e84f900fe12e5a7fd74f312fa10730cc0fda230e9a",
        hasher.computeIdentifierHash("123456789A"));
    assertEquals(
        "ec49dc08f8dd8b4e189f623ab666cfc8b81f201cc94fe6aef860a4c3bd57f278",
        hasher.computeIdentifierHash("3456789"));
  }
}
