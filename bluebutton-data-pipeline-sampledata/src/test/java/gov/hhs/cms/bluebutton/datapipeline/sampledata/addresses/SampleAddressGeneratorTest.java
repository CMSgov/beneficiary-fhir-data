package gov.hhs.cms.bluebutton.datapipeline.sampledata.addresses;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link SampleAddressGenerator}.
 */
public final class SampleAddressGeneratorTest {
	/**
	 * Verifies that {@link SampleAddressGenerator} can be used to generate at
	 * least one {@link SampleAddress}.
	 */
	@Test
	public void canGenerateAddress() {
		SampleAddressGenerator addressGenerator = new SampleAddressGenerator();
		SampleAddress address = addressGenerator.generateAddress();
		Assert.assertNotNull(address);
	}
}
