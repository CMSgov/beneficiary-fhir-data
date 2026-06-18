package gov.cms.bfd.server.ng.input;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CoveragePartTest {

  @Test
  void parseFromQueryParam_validScenarios() {
    assertEquals(CoveragePart.PART_A, CoveragePart.parseFromQueryParam("part-a").orElse(null));
    assertEquals(CoveragePart.PART_B, CoveragePart.parseFromQueryParam("part-b").orElse(null));
    assertEquals(CoveragePart.PART_C, CoveragePart.parseFromQueryParam("part-c").orElse(null));
    assertEquals(CoveragePart.PART_D, CoveragePart.parseFromQueryParam("part-d").orElse(null));
    assertEquals(CoveragePart.DUAL, CoveragePart.parseFromQueryParam("dual").orElse(null));

    assertEquals(CoveragePart.PART_A, CoveragePart.parseFromQueryParam("Part A").orElse(null));
    assertEquals(CoveragePart.PART_B, CoveragePart.parseFromQueryParam("PART_B").orElse(null));
    assertEquals(CoveragePart.PART_C, CoveragePart.parseFromQueryParam(" part-c ").orElse(null));
    assertEquals(CoveragePart.PART_D, CoveragePart.parseFromQueryParam("Part_D").orElse(null));
  }

  @Test
  void parseFromQueryParam_invalidScenarios() {
    assertTrue(CoveragePart.parseFromQueryParam(null).isEmpty());
    assertTrue(CoveragePart.parseFromQueryParam("").isEmpty());
    assertTrue(CoveragePart.parseFromQueryParam("   ").isEmpty());
    assertTrue(CoveragePart.parseFromQueryParam("part-e").isEmpty());
    assertTrue(CoveragePart.parseFromQueryParam("medicare").isEmpty());
    assertTrue(CoveragePart.parseFromQueryParam("part/a").isEmpty());
  }
}
