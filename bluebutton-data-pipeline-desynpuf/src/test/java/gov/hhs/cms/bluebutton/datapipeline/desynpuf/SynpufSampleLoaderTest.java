package gov.hhs.cms.bluebutton.datapipeline.desynpuf;

import java.io.IOException;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit tests for {@link SynpufSampleLoader}.
 */
public final class SynpufSampleLoaderTest extends SynpufSampleLoaderTester {
	private static final Logger LOGGER = LoggerFactory.getLogger(SynpufSampleLoaderTest.class);

	/**
	 * Verifies the small subsetted/"test" {@link SynpufArchive}s.
	 * 
	 * @throws IOException
	 *             (indicates test failure)
	 */
	@Test
	public void verifyTestArchives() throws IOException {
		for (SynpufArchive archive : SynpufArchive.values()) {
			// Skip the large, "real" archives to keep this unit test fast.
			if (archive.getBeneficiaryCount() > (10000))
				continue;

			LOGGER.info("Testing against {}", archive);
			extractAndVerify(archive);
		}
	}
}
