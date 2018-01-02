package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.sql.Date;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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
	 * Verifies that {@link BeneficiaryTransformer} works correctly when passed
	 * a {@link Beneficiary} where all {@link Optional} fields are set to
	 * {@link Optional#empty()}.
	 */
	@Test
	public void transformBeneficiaryWithAllOptionalsEmpty() {
		List<Object> parsedRecords = ServerTestUtils
				.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
		Beneficiary beneficiary = parsedRecords.stream().filter(r -> r instanceof Beneficiary).map(r -> (Beneficiary) r)
				.findFirst().get();
		TransformerTestUtils.setAllOptionalsToEmpty(beneficiary);

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
		TransformerTestUtils.assertNoEncodedOptionals(patient);

		Assert.assertEquals(beneficiary.getBeneficiaryId(), patient.getIdElement().getIdPart());
		Assert.assertEquals(1, patient.getAddress().size());
		Assert.assertEquals(beneficiary.getStateCode(), patient.getAddress().get(0).getState());
		Assert.assertEquals(beneficiary.getCountyCode(), patient.getAddress().get(0).getDistrict());
		Assert.assertEquals(beneficiary.getPostalCode(), patient.getAddress().get(0).getPostalCode());
		Assert.assertEquals(Date.valueOf(beneficiary.getBirthDate()), patient.getBirthDate());
		if (beneficiary.getSex() == 'M')
			Assert.assertEquals("MALE", patient.getGender().toString().trim());
		else if (beneficiary.getSex() == 'F')
			Assert.assertEquals("FEMALE", patient.getGender().toString().trim());
		if (beneficiary.getRace().isPresent())
			TransformerTestUtils.assertExtensionCodingEquals(patient, BeneficiaryTransformer.EXTENSION_US_CORE_RACE,
					BeneficiaryTransformer.CODING_CCW_RACE, "" + beneficiary.getRace().get());
		Assert.assertEquals(beneficiary.getNameGiven(), patient.getName().get(0).getGiven().get(0).toString());
		if (beneficiary.getNameMiddleInitial().isPresent())
			Assert.assertEquals(beneficiary.getNameMiddleInitial().get().toString(),
					patient.getName().get(0).getGiven().get(1).toString());
		Assert.assertEquals(beneficiary.getNameSurname(), patient.getName().get(0).getFamily());
	}
}
