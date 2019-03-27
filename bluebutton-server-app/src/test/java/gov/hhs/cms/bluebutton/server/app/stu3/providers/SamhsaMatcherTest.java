package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.exceptions.FHIRException;
import org.junit.Assert;
import org.junit.Test;

import com.codahale.metrics.MetricRegistry;

import gov.hhs.cms.bluebutton.data.model.rif.Beneficiary;
import gov.hhs.cms.bluebutton.data.model.rif.BeneficiaryHistory;
import gov.hhs.cms.bluebutton.data.model.rif.HHAClaim;
import gov.hhs.cms.bluebutton.data.model.rif.HospiceClaim;
import gov.hhs.cms.bluebutton.data.model.rif.InpatientClaim;
import gov.hhs.cms.bluebutton.data.model.rif.MedicareBeneficiaryIdHistory;
import gov.hhs.cms.bluebutton.data.model.rif.OutpatientClaim;
import gov.hhs.cms.bluebutton.data.model.rif.PartDEvent;
import gov.hhs.cms.bluebutton.data.model.rif.SNFClaim;
import gov.hhs.cms.bluebutton.data.model.rif.samples.StaticRifResourceGroup;
import gov.hhs.cms.bluebutton.server.app.ServerTestUtils;

/**
 * Unit tests for {@link SamhsaMatcherTest}. Integration with
 * {@link ExplanationOfBenefitResourceProvider} is covered by
 * {@link ExplanationOfBenefitResourceProviderIT#searchForEobsWithSamhsaFiltering()}
 * and related integration tests.
 */
public final class SamhsaMatcherTest {
	// TODO complete and verify that these exactly match real values in our DB
	// static final String SAMPLE_SAMHSA_DRG_CODE = "TODO";
	static final String SAMPLE_SAMHSA_CPT_CODE = "4320F";
	static final String SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE = "29189";
	// static final String SAMPLE_SAMHSA_ICD_9_PROCEDURE_CODE = "TODO";
	static final String SAMPLE_SAMHSA_ICD_10_DIAGNOSIS_CODE = "F1010";
	// static final String SAMPLE_SAMHSA_ICD_10_PROCEDURE_CODE = "TODO";

	/**
	 * Verifies that {@link SamhsaMatcher#test(ExplanationOfBenefit)} returns
	 * <code>false</code> for claims that have no SAMHSA-related codes.
	 */
	@Test
	public void nonSamhsaRelatedClaims() {
		SamhsaMatcher matcher = new SamhsaMatcher();

		// Note: none of our SAMPLE_A claims have SAMHSA-related codes (by default).
		List<Object> sampleRifRecords = ServerTestUtils
				.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
		List<ExplanationOfBenefit> sampleEobs = sampleRifRecords.stream().map(r -> {
			// FIXME remove most `else if`s once filtering fully supports all claim types
			if (r instanceof Beneficiary)
				return null;
			else if (r instanceof BeneficiaryHistory)
				return null;
			else if (r instanceof MedicareBeneficiaryIdHistory)
				return null;
			else if (r instanceof HHAClaim)
				return null;
			else if (r instanceof HospiceClaim)
				return null;
			else if (r instanceof InpatientClaim)
				return null;
			else if (r instanceof OutpatientClaim)
				return null;
			else if (r instanceof SNFClaim)
				return null;
			else if (r instanceof PartDEvent)
				return null;

			return TransformerUtils.transformRifRecordToEob(new MetricRegistry(), r);
		}).filter(ExplanationOfBenefit.class::isInstance).collect(Collectors.toList());

		for (ExplanationOfBenefit sampleEob : sampleEobs)
			Assert.assertFalse("Unexpected SAMHSA filtering of EOB: " + sampleEob.getId(), matcher.test(sampleEob));
	}

	/**
	 * Verifies that {@link SamhsaMatcher#test(ExplanationOfBenefit)} returns
	 * <code>true</code> for {@link ClaimType#CARRIER} {@link ExplanationOfBenefit}s
	 * that have SAMHSA-related ICD 9 diagnosis codes.
	 * 
	 * @throws FHIRException
	 *             (indicates problem with test data)
	 */
	@Test
	public void matchCarrierClaimsByIcd9Diagnosis() throws FHIRException {
		SamhsaMatcher matcher = new SamhsaMatcher();

		ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.CARRIER);
		Coding sampleEobDiagnosis = sampleEob.getDiagnosisFirstRep().getDiagnosisCodeableConcept().getCodingFirstRep();
		sampleEobDiagnosis.setSystem(IcdCode.CODING_SYSTEM_ICD_9).setCode(SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE);

		Assert.assertTrue(matcher.test(sampleEob));
	}

	/**
	 * Verifies that {@link SamhsaMatcher#test(ExplanationOfBenefit)} returns
	 * <code>true</code> for {@link ClaimType#CARRIER} {@link ExplanationOfBenefit}s
	 * that have SAMHSA-related ICD 10 diagnosis codes.
	 * 
	 * @throws FHIRException
	 *             (indicates problem with test data)
	 */
	@Test
	public void matchCarrierClaimsByIcd10Diagnosis() throws FHIRException {
		SamhsaMatcher matcher = new SamhsaMatcher();

		ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.CARRIER);
		Coding sampleEobDiagnosis = sampleEob.getDiagnosisFirstRep().getDiagnosisCodeableConcept().getCodingFirstRep();
		sampleEobDiagnosis.setSystem(IcdCode.CODING_SYSTEM_ICD_10).setCode(SAMPLE_SAMHSA_ICD_10_DIAGNOSIS_CODE);

		Assert.assertTrue(matcher.test(sampleEob));
	}

	/**
	 * Verifies that {@link SamhsaMatcher#test(ExplanationOfBenefit)} returns
	 * <code>true</code> for {@link ClaimType#CARRIER} {@link ExplanationOfBenefit}s
	 * that have SAMHSA-related CPT procedure codes.
	 * 
	 * @throws FHIRException
	 *             (indicates problem with test data)
	 */
	@Test
	public void matchCarrierClaimsByCptProcedure() throws FHIRException {
		SamhsaMatcher matcher = new SamhsaMatcher();

		ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.CARRIER);
		Coding sampleEobService = sampleEob.getItemFirstRep().getService().getCodingFirstRep();
		sampleEobService.setCode(SAMPLE_SAMHSA_CPT_CODE);

		Assert.assertTrue(matcher.test(sampleEob));
	}

	/**
	 * Verifies that {@link SamhsaMatcher#test(ExplanationOfBenefit)} returns
	 * <code>true</code> for {@link ClaimType#DME} {@link ExplanationOfBenefit}s
	 * that have SAMHSA-related ICD 9 diagnosis codes.
	 * 
	 * @throws FHIRException
	 *             (indicates problem with test data)
	 */
	@Test
	public void matchDmeClaimsByIcd9Diagnosis() throws FHIRException {
		SamhsaMatcher matcher = new SamhsaMatcher();

		ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.DME);
		Coding sampleEobDiagnosis = sampleEob.getDiagnosisFirstRep().getDiagnosisCodeableConcept().getCodingFirstRep();
		sampleEobDiagnosis.setSystem(IcdCode.CODING_SYSTEM_ICD_9).setCode(SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE);

		Assert.assertTrue(matcher.test(sampleEob));
	}

	/**
	 * Verifies that {@link SamhsaMatcher#test(ExplanationOfBenefit)} returns
	 * <code>true</code> for {@link ClaimType#DME} {@link ExplanationOfBenefit}s
	 * that have SAMHSA-related ICD 10 diagnosis codes.
	 * 
	 * @throws FHIRException
	 *             (indicates problem with test data)
	 */
	@Test
	public void matchDmeClaimsByIcd10Diagnosis() throws FHIRException {
		SamhsaMatcher matcher = new SamhsaMatcher();

		ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.DME);
		Coding sampleEobDiagnosis = sampleEob.getDiagnosisFirstRep().getDiagnosisCodeableConcept().getCodingFirstRep();
		sampleEobDiagnosis.setSystem(IcdCode.CODING_SYSTEM_ICD_10).setCode(SAMPLE_SAMHSA_ICD_10_DIAGNOSIS_CODE);

		Assert.assertTrue(matcher.test(sampleEob));
	}

	/**
	 * Verifies that {@link SamhsaMatcher#test(ExplanationOfBenefit)} returns
	 * <code>true</code> for {@link ClaimType#DME} {@link ExplanationOfBenefit}s
	 * that have SAMHSA-related CPT procedure codes.
	 * 
	 * @throws FHIRException
	 *             (indicates problem with test data)
	 */
	@Test
	public void matchDmeClaimsByCptProcedure() throws FHIRException {
		SamhsaMatcher matcher = new SamhsaMatcher();

		ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.DME);
		Coding sampleEobService = sampleEob.getItemFirstRep().getService().getCodingFirstRep();
		sampleEobService.setCode(SAMPLE_SAMHSA_CPT_CODE);

		Assert.assertTrue(matcher.test(sampleEob));
	}

	/**
	 * @param claimType
	 *            the {@link ClaimType} to get a sample {@link ExplanationOfBenefit}
	 *            for
	 * @return a sample {@link ExplanationOfBenefit} of the specified
	 *         {@link ClaimType} (derived from the
	 *         {@link StaticRifResourceGroup#SAMPLE_A} sample RIF records)
	 */
	private ExplanationOfBenefit getSampleAClaim(ClaimType claimType) {
		List<Object> sampleRifRecords = ServerTestUtils
				.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
		Object sampleRifRecordForClaimType = sampleRifRecords.stream().filter(claimType.getEntityClass()::isInstance)
				.findFirst().get();
		ExplanationOfBenefit sampleEobForClaimType = TransformerUtils.transformRifRecordToEob(new MetricRegistry(),
				sampleRifRecordForClaimType);

		return sampleEobForClaimType;
	}
}
