package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.util.Arrays;
import java.util.List;

import org.hl7.fhir.dstu3.model.Coverage;
import org.hl7.fhir.dstu3.model.Coverage.CoverageStatus;
import org.hl7.fhir.exceptions.FHIRException;
import org.junit.Assert;
import org.junit.Test;

import gov.hhs.cms.bluebutton.data.model.rif.Beneficiary;
import gov.hhs.cms.bluebutton.data.model.rif.samples.StaticRifResource;
import gov.hhs.cms.bluebutton.data.model.rif.samples.StaticRifResourceGroup;
import gov.hhs.cms.bluebutton.server.app.ServerTestUtils;

/**
 * Unit tests for {@link CoverageTransformer}.
 */
public final class CoverageTransformerTest {
	/**
	 * Verifies that
	 * {@link CoverageTransformer#transform(MedicareSegment, Beneficiary)} works
	 * as expected when run against the
	 * {@link StaticRifResource#SAMPLE_A_CARRIER} {@link Beneficiary}.
	 * 
	 * @throws FHIRException
	 *             (indicates test failure)
	 */
	@Test
	public void transformSampleARecord() throws FHIRException {
		List<Object> parsedRecords = ServerTestUtils
				.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
		Beneficiary beneficiary = parsedRecords.stream().filter(r -> r instanceof Beneficiary).map(r -> (Beneficiary) r)
				.findFirst().get();

		Coverage partACoverage = CoverageTransformer.transform(MedicareSegment.PART_A, beneficiary);
		assertPartAMatches(beneficiary, partACoverage);

		Coverage partBCoverage = CoverageTransformer.transform(MedicareSegment.PART_B, beneficiary);
		assertPartBMatches(beneficiary, partBCoverage);

		Coverage partDCoverage = CoverageTransformer.transform(MedicareSegment.PART_D, beneficiary);
		assertPartDMatches(beneficiary, partDCoverage);
	}

	/**
	 * Verifies that the specified {@link MedicareSegment#PART_A}
	 * {@link Coverage} "looks like" it should, if it were produced from the
	 * specified {@link Beneficiary}.
	 * 
	 * @param beneficiary
	 *            the {@link Beneficiary} that the specified {@link Coverage}
	 *            should match
	 * @param coverage
	 *            the {@link Coverage} to verify
	 */
	static void assertPartAMatches(Beneficiary beneficiary, Coverage coverage) {
		Assert.assertNotNull(coverage);

		Assert.assertEquals(TransformerUtils.buildCoverageId(MedicareSegment.PART_A, beneficiary).getIdPart(),
				coverage.getIdElement().getIdPart());
		Assert.assertEquals(TransformerConstants.COVERAGE_PLAN, coverage.getGrouping().getSubGroup());
		Assert.assertEquals(TransformerConstants.COVERAGE_PLAN_PART_A, coverage.getGrouping().getSubPlan());
		Assert.assertEquals(CoverageStatus.ACTIVE, coverage.getStatus());

		if (beneficiary.getMedicareEnrollmentStatusCode().isPresent())
			TransformerTestUtils.assertExtensionCodingEquals(coverage,
					TransformerConstants.EXTENSION_CODING_CCW_MEDICARE_STATUS,
					TransformerConstants.EXTENSION_CODING_CCW_MEDICARE_STATUS,
					beneficiary.getMedicareEnrollmentStatusCode().get());

		if (beneficiary.getEntitlementCodeOriginal().isPresent())
			TransformerTestUtils.assertExtensionCodingEquals(coverage,
					TransformerConstants.EXTENSION_CODING_CCW_MEDICARE_ENTITLEMENT_ORIGINAL,
					TransformerConstants.EXTENSION_CODING_CCW_MEDICARE_ENTITLEMENT_ORIGINAL,
					String.valueOf(beneficiary.getEntitlementCodeOriginal().get()));
		if (beneficiary.getEntitlementCodeCurrent().isPresent())
			TransformerTestUtils.assertExtensionCodingEquals(coverage,
					TransformerConstants.EXTENSION_CODING_CCW_MEDICARE_ENTITLEMENT_CURRENT,
					TransformerConstants.EXTENSION_CODING_CCW_MEDICARE_ENTITLEMENT_CURRENT,
					String.valueOf(beneficiary.getEntitlementCodeCurrent().get()));
		if (beneficiary.getEndStageRenalDiseaseCode().isPresent())
			TransformerTestUtils.assertExtensionCodingEquals(coverage,
					TransformerConstants.EXTENSION_CODING_CCW_ESRD_INDICATOR,
					TransformerConstants.EXTENSION_CODING_CCW_ESRD_INDICATOR,
					String.valueOf(beneficiary.getEndStageRenalDiseaseCode().get()));
	}

	/**
	 * Verifies that the specified {@link MedicareSegment#PART_B}
	 * {@link Coverage} "looks like" it should, if it were produced from the
	 * specified {@link Beneficiary}.
	 * 
	 * @param beneficiary
	 *            the {@link Beneficiary} that the specified {@link Coverage}
	 *            should match
	 * @param coverage
	 *            the {@link Coverage} to verify
	 */
	static void assertPartBMatches(Beneficiary beneficiary, Coverage coverage) {
		Assert.assertNotNull(coverage);

		Assert.assertEquals(TransformerUtils.buildCoverageId(MedicareSegment.PART_B, beneficiary).getIdPart(),
				coverage.getIdElement().getIdPart());
		Assert.assertEquals(TransformerConstants.COVERAGE_PLAN, coverage.getGrouping().getSubGroup());
		Assert.assertEquals(TransformerConstants.COVERAGE_PLAN_PART_B, coverage.getGrouping().getSubPlan());
		Assert.assertEquals(CoverageStatus.ACTIVE, coverage.getStatus());

		if (beneficiary.getMedicareEnrollmentStatusCode().isPresent())
			TransformerTestUtils.assertExtensionCodingEquals(coverage,
					TransformerConstants.EXTENSION_CODING_CCW_MEDICARE_STATUS,
					TransformerConstants.EXTENSION_CODING_CCW_MEDICARE_STATUS,
					beneficiary.getMedicareEnrollmentStatusCode().get());
	}

	/**
	 * Verifies that the specified {@link MedicareSegment#PART_D}
	 * {@link Coverage} "looks like" it should, if it were produced from the
	 * specified {@link Beneficiary}.
	 * 
	 * @param beneficiary
	 *            the {@link Beneficiary} that the specified {@link Coverage}
	 *            should match
	 * @param coverage
	 *            the {@link Coverage} to verify
	 */
	static void assertPartDMatches(Beneficiary beneficiary, Coverage coverage) {
		Assert.assertNotNull(coverage);

		Assert.assertEquals(TransformerUtils.buildCoverageId(MedicareSegment.PART_D, beneficiary).getIdPart(),
				coverage.getIdElement().getIdPart());
		Assert.assertEquals(TransformerConstants.COVERAGE_PLAN, coverage.getGrouping().getSubGroup());
		Assert.assertEquals(TransformerConstants.COVERAGE_PLAN_PART_D, coverage.getGrouping().getSubPlan());

		if (beneficiary.getMedicareEnrollmentStatusCode().isPresent())
			TransformerTestUtils.assertExtensionCodingEquals(coverage,
					TransformerConstants.EXTENSION_CODING_CCW_MEDICARE_STATUS,
					TransformerConstants.EXTENSION_CODING_CCW_MEDICARE_STATUS,
					beneficiary.getMedicareEnrollmentStatusCode().get());
	}
}
