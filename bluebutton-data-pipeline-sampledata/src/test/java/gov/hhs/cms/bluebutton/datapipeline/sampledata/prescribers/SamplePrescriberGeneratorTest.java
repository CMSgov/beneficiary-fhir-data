package gov.hhs.cms.bluebutton.datapipeline.sampledata.prescribers;

import org.junit.Assert;
import org.junit.Test;

import gov.hhs.cms.bluebutton.datapipeline.sampledata.addresses.SampleAddressGenerator;

/**
 * Unit tests for {@link SamplePrescriberGenerator}.
 */
public final class SamplePrescriberGeneratorTest {
	/**
	 * Verifies that {@link SampleAddressGenerator} can be used to generate at
	 * least one {@link SamplePresciber}.
	 */
	@Test
	public void canGeneratePrescriber() {
		SamplePrescriberGenerator pharmacyGenerator = new SamplePrescriberGenerator();
		SamplePresciber prescriber = pharmacyGenerator.generatePrescriber();
		Assert.assertNotNull(prescriber);
	}
}
