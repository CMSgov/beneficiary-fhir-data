package gov.hhs.cms.bluebutton.datapipeline.sampledata.npi;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link SampleProviderGenerator}.
 */
public final class SampleProviderGeneratorTest {
	/**
	 * Verifies that {@link SampleProviderGenerator} can be used to generate at
	 * least one {@link SampleProvider}.
	 */
	@Test
	public void canGenerateAddress() {
		SampleProviderGenerator providerGenerator = new SampleProviderGenerator();
		SampleProvider provider = providerGenerator.generateProvider();
		Assert.assertNotNull(provider);
	}
}
