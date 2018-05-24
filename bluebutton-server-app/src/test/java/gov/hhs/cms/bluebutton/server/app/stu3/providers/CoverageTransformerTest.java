package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.util.Arrays;
import java.util.List;

import org.hl7.fhir.dstu3.model.Coverage;
import org.hl7.fhir.dstu3.model.Coverage.CoverageStatus;
import org.hl7.fhir.exceptions.FHIRException;
import org.junit.Assert;
import org.junit.Test;

import com.codahale.metrics.MetricRegistry;

import gov.hhs.cms.bluebutton.data.codebook.data.CcwCodebookVariable;
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

		Coverage partACoverage = CoverageTransformer.transform(new MetricRegistry(), MedicareSegment.PART_A,
				beneficiary);
		assertPartAMatches(beneficiary, partACoverage);

		Coverage partBCoverage = CoverageTransformer.transform(new MetricRegistry(), MedicareSegment.PART_B,
				beneficiary);
		assertPartBMatches(beneficiary, partBCoverage);

		Coverage partDCoverage = CoverageTransformer.transform(new MetricRegistry(), MedicareSegment.PART_D,
				beneficiary);
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
			TransformerTestUtils.assertExtensionCodingEquals(CcwCodebookVariable.MS_CD, beneficiary.getMedicareEnrollmentStatusCode(),
					coverage);
		if (beneficiary.getEntitlementCodeOriginal().isPresent())
			TransformerTestUtils.assertExtensionCodingEquals(CcwCodebookVariable.OREC, beneficiary.getEntitlementCodeOriginal(),
					coverage);
		if (beneficiary.getEntitlementCodeCurrent().isPresent())
			TransformerTestUtils.assertExtensionCodingEquals(CcwCodebookVariable.CREC, beneficiary.getEntitlementCodeCurrent(),
					coverage);
		if (beneficiary.getEndStageRenalDiseaseCode().isPresent())
			TransformerTestUtils.assertExtensionCodingEquals(CcwCodebookVariable.ESRD_IND, beneficiary.getEndStageRenalDiseaseCode(),
					coverage);
		if (beneficiary.getPartATerminationCode().isPresent())
			TransformerTestUtils.assertExtensionCodingEquals(CcwCodebookVariable.A_TRM_CD, beneficiary.getPartATerminationCode(),
					coverage);
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
			TransformerTestUtils.assertExtensionCodingEquals(CcwCodebookVariable.MS_CD, beneficiary.getMedicareEnrollmentStatusCode(),
					coverage);
		if (beneficiary.getPartBTerminationCode().isPresent())
			TransformerTestUtils.assertExtensionCodingEquals(CcwCodebookVariable.B_TRM_CD, beneficiary.getPartBTerminationCode(),
					coverage);
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
			TransformerTestUtils.assertExtensionCodingEquals(CcwCodebookVariable.MS_CD, beneficiary.getMedicareEnrollmentStatusCode(),
					coverage);
		Assert.assertEquals(CoverageStatus.ACTIVE, coverage.getStatus());
	}
}
