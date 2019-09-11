package gov.cms.bfd.server.test.perf.utils;

import static org.junit.Assert.*;

import org.junit.*;

public class BenefitIdMgrTest {
  @Test
  public void testSetCurrIndex() {
    BenefitIdMgr bim = new BenefitIdMgr(40, 20, 60, "test", "%02d");
    assertEquals("test40", bim.nextId());
    bim = new BenefitIdMgr(50, 20, 60, "test", "%02d");
    assertEquals("test50", bim.nextId());
    bim = new BenefitIdMgr(0, 20, 60, "test", "%02d");
    assertEquals("test20", bim.nextId());
    bim = new BenefitIdMgr(70, 20, 60, "test", "%02d");
    assertEquals("test60", bim.nextId());
  }

  @Test
  public void testNextId() {
    BenefitIdMgr bim = new BenefitIdMgr(0, 20, 60, "test", "%02d");
    assertEquals("test20", bim.nextId());
    assertEquals("test21", bim.nextId());
    bim = new BenefitIdMgr(60, 20, 60, "test", "%02d");
    assertEquals("test60", bim.nextId());
    assertEquals("test20", bim.nextId());
  }
};
