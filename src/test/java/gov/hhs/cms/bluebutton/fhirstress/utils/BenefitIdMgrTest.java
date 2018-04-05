package gov.hhs.cms.bluebutton.fhirstress.utils;

import org.junit.*;
import static org.junit.Assert.*;

public class BenefitIdMgrTest {
	@Test
	public void testSetCurrIndex() {
		BenefitIdMgr bim = new BenefitIdMgr(40, 20, 60, "test", "%02d");
		assertEquals("test40", bim.nextId());
		bim.setCurrIndex(50);
		assertEquals("test50", bim.nextId());
		bim.setCurrIndex(0);
		assertEquals("test20", bim.nextId());
		bim.setCurrIndex(70);
		assertEquals("test60", bim.nextId());
	}

	@Test
	public void testNextId() {
		BenefitIdMgr bim = new BenefitIdMgr(0, 20, 60, "test", "%02d");
		assertEquals("test20", bim.nextId());
		assertEquals("test21", bim.nextId());
		bim.setCurrIndex(60);
		assertEquals("test60", bim.nextId());
		assertEquals("test20", bim.nextId());
	}

	@Test
	public void testPrevId() {
		BenefitIdMgr bim = new BenefitIdMgr(70, 20, 60, "test", "%02d");
		assertEquals("test59", bim.prevId());
		assertEquals("test58", bim.prevId());
		bim.setCurrIndex(0);
		assertEquals("test60", bim.prevId());
		assertEquals("test59", bim.prevId());
	}
};
