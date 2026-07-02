package gov.cms.bfd.server.ng.fhirtrimmer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BasisProfileMapTest {
  @BeforeEach
  void setUp() {}

  @Test
  void testBasisProfileMap() {
    var profileMap = new BasisProfileMap();
    profileMap.generateProfileBasisMap();

    var blackList = profileMap.getBlackList();
    var whiteList = profileMap.getWhiteList();
    var x = 2;
  }
}
