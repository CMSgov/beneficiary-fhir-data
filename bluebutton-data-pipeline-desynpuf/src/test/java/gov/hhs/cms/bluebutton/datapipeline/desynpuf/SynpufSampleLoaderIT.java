package gov.hhs.cms.bluebutton.datapipeline.desynpuf;

import java.io.IOException;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integration tests for {@link SynpufSampleLoader}.
 */
public final class SynpufSampleLoaderIT extends SynpufSampleLoaderTester {
	private static final Logger LOGGER = LoggerFactory.getLogger(SynpufSampleLoaderIT.class);

	/**
	 * Verifies the large/"real" {@link SynpufArchive}s.
	 * 
	 * @throws IOException
	 *             (indicates test failure)
	 */
	@Test
	public void verifyRealArchives() throws IOException {
		for (SynpufArchive archive : SynpufArchive.values()) {
			/*
			 * Skip the small archives that are already covered in the unit
			 * tests.
			 */
			if (archive.getBeneficiaryCount() <= (10000))
				continue;

			LOGGER.info("Testing against {}", archive);
			extractAndVerify(archive);
		}
	}
}
