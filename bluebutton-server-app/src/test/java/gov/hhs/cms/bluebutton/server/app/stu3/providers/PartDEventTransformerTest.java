package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.sql.Date;
import java.util.Arrays;
import java.util.List;

import org.hl7.fhir.dstu3.model.Coverage;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.exceptions.FHIRException;
import org.junit.Assert;
import org.junit.Test;

import gov.hhs.cms.bluebutton.data.model.rif.PartDEvent;
import gov.hhs.cms.bluebutton.data.model.rif.samples.StaticRifResource;
import gov.hhs.cms.bluebutton.data.model.rif.samples.StaticRifResourceGroup;
import gov.hhs.cms.bluebutton.server.app.ServerTestUtils;

/**
 * Unit tests for {@link PartDEventTransformer}.
 */
public final class PartDEventTransformerTest {
	/**
	 * Verifies that {@link PartDEventTransformer#transform(Object)} works as
	 * expected when run against the {@link StaticRifResource#SAMPLE_A_PDE}
	 * {@link PartDEvent}.
	 * 
	 * @throws FHIRException
	 *             (indicates test failure)
	 */
	@Test
	public void transformSampleARecord() throws FHIRException {
		List<Object> parsedRecords = ServerTestUtils
				.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
		PartDEvent claim = parsedRecords.stream().filter(r -> r instanceof PartDEvent).map(r -> (PartDEvent) r)
				.findFirst().get();

		ExplanationOfBenefit eob = PartDEventTransformer.transform(claim);
		assertMatches(claim, eob);
	}

	/**
	 * Verifies that the {@link ExplanationOfBenefit} "looks like" it should, if
	 * it were produced from the specified {@link PartDEvent}.
	 * 
	 * @param claim
	 *            the {@link PartDEvent} that the {@link ExplanationOfBenefit} was
	 *            generated from
	 * @param eob
	 *            the {@link ExplanationOfBenefit} that was generated from the
	 *            specified {@link PartDEvent}
	 * @throws FHIRException
	 *             (indicates test failure)
	 */
	static void assertMatches(PartDEvent claim, ExplanationOfBenefit eob) throws FHIRException {
		TransformerTestUtils.assertNoEncodedOptionals(eob);

		Assert.assertEquals(TransformerUtils.buildEobId(ClaimType.PDE, claim.getEventId()),
				eob.getIdElement().getIdPart());

		TransformerTestUtils.assertIdentifierExists(TransformerConstants.CODING_SYSTEM_CCW_PDE_ID, claim.getEventId(),
				eob.getIdentifier());
		TransformerTestUtils.assertIdentifierExists(TransformerConstants.CODING_SYSTEM_CCW_CLAIM_GRP_ID,
				claim.getClaimGroupId().toPlainString(), eob.getIdentifier());
		Assert.assertEquals(TransformerUtils.referencePatient(claim.getBeneficiaryId()).getReference(),
				eob.getPatient().getReference());
		TransformerTestUtils.assertHasCoding(TransformerConstants.CODING_SYSTEM_FHIR_CLAIM_TYPE, "pharmacy",
				eob.getType());
		Assert.assertEquals(TransformerUtils.referencePatient(claim.getBeneficiaryId()).getReference(),
				eob.getPatient().getReference());
		Assert.assertEquals(Date.valueOf(claim.getPaymentDate().get()), eob.getPayment().getDate());

		Assert.assertEquals("01", claim.getServiceProviderIdQualiferCode());
		Assert.assertEquals("01", claim.getPrescriberIdQualifierCode());

		ItemComponent rxItem = eob.getItem().stream().filter(i -> i.getSequence() == 1).findAny().get();

		TransformerTestUtils.assertHasCoding(TransformerConstants.CODING_SYSTEM_FHIR_ACT, "RXDINV", rxItem.getDetail().get(0).getType());

		Assert.assertEquals(Date.valueOf(claim.getPrescriptionFillDate()), rxItem.getServicedDateType().getValue());

		TransformerTestUtils.assertReferenceEquals(TransformerConstants.CODING_SYSTEM_NPI_US,
				claim.getServiceProviderId(), eob.getOrganization());
		TransformerTestUtils.assertReferenceEquals(TransformerConstants.CODING_SYSTEM_NPI_US,
				claim.getServiceProviderId(),
				eob.getFacility());

		TransformerTestUtils.assertExtensionCodingEquals(eob.getFacility(),
				TransformerConstants.CODING_SYSTEM_CCW_PHRMCY_SRVC_TYPE_CD,
				TransformerConstants.CODING_SYSTEM_CCW_PHRMCY_SRVC_TYPE_CD, claim.getPharmacyTypeCode());

		// Default case has drug coverage status code as Covered
		TransformerTestUtils.assertAdjudicationEquals(TransformerConstants.CODED_ADJUDICATION_PART_D_COVERED,
				claim.getPartDPlanCoveredPaidAmount(), rxItem.getAdjudication());
		TransformerTestUtils.assertAdjudicationNotPresent(
				TransformerConstants.CODED_ADJUDICATION_PART_D_NONCOVERED_SUPPLEMENT,
				rxItem.getAdjudication());
		TransformerTestUtils.assertAdjudicationNotPresent(TransformerConstants.CODED_ADJUDICATION_PART_D_NONCOVERED_OTC,
				rxItem.getAdjudication());
		TransformerTestUtils.assertAdjudicationEquals(TransformerConstants.CODED_ADJUDICATION_PATIENT_PAY,
				claim.getPatientPaidAmount(),
				rxItem.getAdjudication());
		TransformerTestUtils.assertAdjudicationEquals(TransformerConstants.CODED_ADJUDICATION_OTHER_TROOP_AMOUNT,
				claim.getOtherTrueOutOfPocketPaidAmount(), rxItem.getAdjudication());
		TransformerTestUtils.assertAdjudicationEquals(TransformerConstants.CODED_ADJUDICATION_LOW_INCOME_SUBSIDY_AMOUNT,
				claim.getLowIncomeSubsidyPaidAmount(), rxItem.getAdjudication());
		TransformerTestUtils.assertAdjudicationEquals(
				TransformerConstants.CODED_ADJUDICATION_PATIENT_LIABILITY_REDUCED_AMOUNT,
				claim.getPatientLiabilityReductionOtherPaidAmount(), rxItem.getAdjudication());
		TransformerTestUtils.assertAdjudicationEquals(TransformerConstants.CODED_ADJUDICATION_TOTAL_COST,
				claim.getTotalPrescriptionCost(),
				rxItem.getAdjudication());
		TransformerTestUtils.assertAdjudicationEquals(TransformerConstants.CODED_ADJUDICATION_GAP_DISCOUNT_AMOUNT,
				claim.getGapDiscountAmount(),
				rxItem.getAdjudication());

		Coverage coverage = (Coverage) eob.getInsurance().getCoverage().getResource();

		TransformerTestUtils.assertIdentifierExists(TransformerConstants.CODING_SYSTEM_PDE_PLAN_CONTRACT_ID,
				claim.getPlanContractId(),
				coverage.getIdentifier());
		TransformerTestUtils.assertIdentifierExists(TransformerConstants.CODING_SYSTEM_PDE_PLAN_BENEFIT_PACKAGE_ID,
				claim.getPlanBenefitPackageId(), coverage.getIdentifier());
		Assert.assertEquals(TransformerConstants.COVERAGE_PLAN, coverage.getGrouping().getPlan());
		Assert.assertEquals(TransformerConstants.COVERAGE_PLAN_PART_D, coverage.getGrouping().getSubPlan());

	}
}

