package gov.hhs.cms.bluebutton.fhirstress.utils;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;

import org.junit.Test;

public class CsvBenefitIdManagerTest {
	@Test
	public void testNextId() {
		URL url = Thread.currentThread().getContextClassLoader().getResource("bene-ids.csv");
		File f = Paths.get(url.getPath()).toFile();
		BenefitIdManager bim = new CsvBenefitIdManager(f);
		assertEquals("567834", bim.nextId());
		assertEquals("5303", bim.nextId());
		assertEquals("12162", bim.nextId());
	}

	@Test
	public void testLooping() {
		URL url = Thread.currentThread().getContextClassLoader().getResource("bene-ids.csv");
		File f = Paths.get(url.getPath()).toFile();
		BenefitIdManager bim = new CsvBenefitIdManager(f);
		assertEquals("567834", bim.nextId());
		assertEquals("5303", bim.nextId());
		assertEquals("12162", bim.nextId());
		assertEquals("567834", bim.nextId());
	}
};
