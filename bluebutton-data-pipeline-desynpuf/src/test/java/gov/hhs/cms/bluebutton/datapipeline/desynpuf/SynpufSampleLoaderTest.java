package gov.hhs.cms.bluebutton.datapipeline.desynpuf;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit tests for {@link SynpufSampleLoader}.
 */
public final class SynpufSampleLoaderTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(SynpufSampleLoaderTest.class);

	/**
	 * Verifies that {@link SynpufSampleLoader} works as expected.
	 * 
	 * @throws IOException
	 *             (indicates test failure)
	 */
	@Test
	public void normalUsage() throws IOException {
		Path extractionDir = null;
		try {
			extractionDir = Files.createTempDirectory("synpuf-tests");

			SynpufArchive[] archives = SynpufArchive.values();
			for (SynpufArchive archive : archives) {
				SynpufSample sample = SynpufSampleLoader.extractSynpufFile(extractionDir, archive);
				Assert.assertNotNull(sample);
				Assert.assertTrue(sample.allFilesExist());

				/*
				 * Format check: each file should have >= 1 row and 3 columns.
				 */
				for (SynpufFile file : SynpufFile.values()) {
					LOGGER.info("Verifying file: {}", file);
					Path filePath = sample.resolve(file);
					try (Reader in = new FileReader(filePath.toFile());) {
						CSVFormat csvFormat = CSVFormat.EXCEL;
						CSVParser csvParser = csvFormat.parse(in);
						Iterator<CSVRecord> recordIter = csvParser.iterator();
						Assert.assertTrue("row count < 1", recordIter.hasNext());
						int numColumns = recordIter.next().size();
						Assert.assertTrue(String.format("not enough columns '%d' in file '%s'", numColumns, file),
								numColumns >= 3);

						csvParser.close();
					}
				}
			}
		} finally {
			if (extractionDir != null)
				FileUtils.deleteDirectory(extractionDir.toFile());
		}
	}
}
