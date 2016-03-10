package gov.hhs.cms.bluebutton.datapipeline.desynpuf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link SynpufSampleLoader}.
 */
public final class SynpufSampleLoaderTest {
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

			SynpufSample sample = SynpufSampleLoader.extractSynpufFile(extractionDir, SynpufArchive.SAMPLE_1);
			Assert.assertNotNull(sample);
			Assert.assertTrue(sample.allFilesExist());
		} finally {
			if (extractionDir != null)
				FileUtils.deleteDirectory(extractionDir.toFile());
		}
	}
}
