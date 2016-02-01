package gov.hhs.cms.bluebutton.texttofhir.parsing;

import java.io.InputStream;
import java.time.ZonedDateTime;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link TextFileProcessor}.
 */
public final class TextFileParserTest {
	/**
	 * Ensures that {@link TextFileProcessor} works as expected when run on an
	 * almost empty file.
	 * 
	 * @throws TextFileParseException
	 *             (indicates test failure)
	 */
	@Test
	public void almostEmptyFile() throws TextFileParseException {
		InputStream textFileStream = Thread.currentThread().getContextClassLoader()
				.getResourceAsStream("bb-text-samples/almost-empty.txt");
		TextFile parsedFile = TextFileProcessor.parse(textFileStream);

		Assert.assertNotNull(parsedFile);
		Assert.assertEquals(1, parsedFile.getSections().size());

		Assert.assertTrue(
				ZonedDateTime.parse("2015-02-04T09:18:00-05:00[US/Eastern]").isEqual(parsedFile.getTimestamp()));

		Section demographicSection = parsedFile.getSections().get(0);
		Assert.assertEquals("Demographic", demographicSection.getName());
		Assert.assertEquals(12, demographicSection.getEntries().size());
		Assert.assertEquals("Name", demographicSection.getEntries().get(1).getName());
		Assert.assertEquals("JOHN DOE", demographicSection.getEntries().get(1).getValue());
		Assert.assertEquals("Address Line 2", demographicSection.getEntries().get(4).getName());
		Assert.assertEquals("", demographicSection.getEntries().get(4).getValue());
	}

	/**
	 * Ensures that {@link TextFileProcessor} works as expected when run on a
	 * large file of fake data.
	 * 
	 * @throws TextFileParseException
	 *             (indicates test failure)
	 */
	@Test
	public void largeFakeDataFile() throws TextFileParseException {
		InputStream textFileStream = Thread.currentThread().getContextClassLoader()
				.getResourceAsStream("bb-text-samples/fake-data-large.txt");
		TextFile parsedFile = TextFileProcessor.parse(textFileStream);

		// For now, just ensure it didn't go boom.
		Assert.assertNotNull(parsedFile);
	}
}
