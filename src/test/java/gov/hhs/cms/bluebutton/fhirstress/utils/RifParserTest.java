package gov.hhs.cms.bluebutton.fhirstress.utils;

import org.junit.*;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.runners.Parameterized.Parameter;

import java.util.Arrays;
import java.util.Collection;

/**
 * Unit test for simple RifParser.
 */
@RunWith(value = Parameterized.class)
public class RifParserTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(RifParserTest.class);
	
	@Parameter(value = 0)
	public String rifFile;

	@Parameter(value = 1)
	public String delimiter;

	@Parameters(name = "{index}: testRifParser({0},{1})")
	public static Collection<Object[]> data() {
		return Arrays.asList(
				new Object[][] { { new String("src/test/resources/beneficiary_truncated.rif"), new String("|") } });
	}

	/**
	 * Test RIF file parsing
	 */
	@Test
	public void testRifParser() {
		RifParser parser = new RifParser(rifFile, delimiter);
		if (parser.hasNext()) {
			RifEntry entry = parser.next();
			LOGGER.info(entry.toString());
			assertEquals(entry.DML_IND, "INSERT");
			assertEquals(entry.BENE_ID, "1");
			assertEquals(entry.STATE_CODE, "ER");
			assertEquals(entry.BENE_COUNTY_CD, "Tbqgwwm");
			assertEquals(entry.BENE_ZIP_CD, "769677434");
			assertEquals(entry.BENE_BIRTH_DT, "18-DEC-1924");
			assertEquals(entry.BENE_SEX_IDENT_CD, "G");
			assertEquals(entry.BENE_RACE_CD, "M");
			assertEquals(entry.BENE_ENTLMT_RSN_ORIG, 8);
			assertEquals(entry.BENE_ENTLMT_RSN_CURR, 6);
			assertEquals(entry.BENE_ESRD_IND, "E");
			assertEquals(entry.BENE_MDCR_STATUS_CD, 90);
			assertEquals(entry.BENE_PTA_TRMNTN_CD, 2);
			assertEquals(entry.BENE_PTB_TRMNTN_CD, 7);
			assertEquals(entry.BENE_CRNT_HIC_NUM, "625127864T");
			assertEquals(entry.BENE_SRNM_NAME, "Wyvrmz");
			assertEquals(entry.BENE_GVN_NAME, "Nevf");
			assertEquals(entry.BENE_MDL_NAME, "B");
		}
	}
}
