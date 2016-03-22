package gov.hhs.cms.bluebutton.datapipeline.sampledata.pharmacies;

import org.junit.Assert;
import org.junit.Test;

import gov.hhs.cms.bluebutton.datapipeline.sampledata.addresses.SampleAddressGenerator;

/**
 * Unit tests for {@link SamplePharmacyGenerator}.
 */
public final class SamplePharmacyGeneratorTest {
	/**
	 * Verifies that {@link SampleAddressGenerator} can be used to generate at
	 * least one {@link SamplePharmacy}.
	 */
	@Test
	public void canGeneratePharmacy() {
		SamplePharmacyGenerator pharmacyGenerator = new SamplePharmacyGenerator();
		SamplePharmacy pharmacy = pharmacyGenerator.generatePharmacy();
		Assert.assertNotNull(pharmacy);
	}
}
