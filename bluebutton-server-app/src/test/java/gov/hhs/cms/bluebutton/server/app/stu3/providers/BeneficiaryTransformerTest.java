package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.util.Arrays;
import java.util.List;

import org.hl7.fhir.dstu3.model.Patient;
import org.junit.Assert;
import org.junit.Test;

import gov.hhs.cms.bluebutton.data.model.rif.Beneficiary;
import gov.hhs.cms.bluebutton.data.model.rif.samples.StaticRifResource;
import gov.hhs.cms.bluebutton.data.model.rif.samples.StaticRifResourceGroup;
import gov.hhs.cms.bluebutton.server.app.ServerTestUtils;

/**
 * Unit tests for {@link BeneficiaryTransformer}.
 */
public final class BeneficiaryTransformerTest {
	/**
	 * Verifies that {@link BeneficiaryTransformer#transform(Beneficiary)} works
	 * as expected when run against the {@link StaticRifResource#SAMPLE_A_BENES}
	 * {@link Beneficiary}.
	 */
	@Test
	public void transformSampleARecord() {
		List<Object> parsedRecords = ServerTestUtils
				.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
		Beneficiary beneficiary = parsedRecords.stream().filter(r -> r instanceof Beneficiary).map(r -> (Beneficiary) r)
				.findFirst().get();

		Patient patient = BeneficiaryTransformer.transform(beneficiary);
		assertMatches(beneficiary, patient);
	}

	/**
	 * Verifies that the {@link Patient} "looks like" it should, if it were
	 * produced from the specified {@link Beneficiary}.
	 * 
	 * @param beneficiary
	 *            the {@link Beneficiary} that the {@link Patient} was generated
	 *            from
	 * @param patient
	 *            the {@link Patient} that was generated from the specified
	 *            {@link Beneficiary}
	 */
	static void assertMatches(Beneficiary beneficiary, Patient patient) {
		Assert.assertEquals(beneficiary.getBeneficiaryId(), patient.getIdElement().getIdPart());
	}
}
