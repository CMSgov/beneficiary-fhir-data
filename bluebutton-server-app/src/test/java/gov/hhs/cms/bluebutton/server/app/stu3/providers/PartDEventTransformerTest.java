package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.sql.Date;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.dstu3.model.codesystems.V3ActCode;
import org.hl7.fhir.exceptions.FHIRException;
import org.junit.Assert;
import org.junit.Test;

import gov.hhs.cms.bluebutton.data.codebook.data.CcwCodebookVariable;
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
		// Test to ensure group level fields between all claim types match
		TransformerTestUtils.assertEobCommonClaimHeaderData(eob, claim.getEventId(), claim.getBeneficiaryId(),
				ClaimType.PDE, claim.getClaimGroupId().toPlainString(), MedicareSegment.PART_D,
				Optional.empty(), Optional.empty(), Optional.empty(), claim.getFinalAction());

		TransformerTestUtils.assertExtensionIdentifierEquals(CcwCodebookVariable.PLAN_CNTRCT_REC_ID,
				claim.getPlanContractId(), eob.getInsurance().getCoverage());
		TransformerTestUtils.assertExtensionIdentifierEquals(CcwCodebookVariable.PLAN_PBP_REC_NUM,
				claim.getPlanBenefitPackageId(), eob.getInsurance().getCoverage());

		Assert.assertEquals("01", claim.getServiceProviderIdQualiferCode());
		Assert.assertEquals("01", claim.getPrescriberIdQualifierCode());

		ItemComponent rxItem = eob.getItem().stream().filter(i -> i.getSequence() == 1).findAny().get();

		TransformerTestUtils.assertHasCoding(TransformerConstants.CODING_NDC, claim.getNationalDrugCode(),
				rxItem.getService().getCoding());

		TransformerTestUtils.assertHasCoding(V3ActCode.RXDINV.getSystem(),
				V3ActCode.RXDINV.toCode(), rxItem.getDetail().get(0).getType().getCoding());

		Assert.assertEquals(Date.valueOf(claim.getPrescriptionFillDate()), rxItem.getServicedDateType().getValue());

		TransformerTestUtils.assertReferenceEquals(TransformerConstants.CODING_NPI_US,
				claim.getServiceProviderId(), eob.getOrganization());
		TransformerTestUtils.assertReferenceEquals(TransformerConstants.CODING_NPI_US,
				claim.getServiceProviderId(),
				eob.getFacility());

		TransformerTestUtils.assertExtensionCodingEquals(CcwCodebookVariable.PHRMCY_SRVC_TYPE_CD,
				claim.getPharmacyTypeCode(), eob.getFacility());

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

		Assert.assertTrue(eob.getInformation().stream()
				.anyMatch(i -> TransformerTestUtils.isCodeInConcept(CcwCodebookVariable.RX_ORGN_CD,
						claim.getPrescriptionOriginationCode(), i.getCategory())));

		TransformerTestUtils.assertAdjudicationEquals(TransformerConstants.CODED_ADJUDICATION_GAP_DISCOUNT_AMOUNT,
				claim.getGapDiscountAmount(),
				rxItem.getAdjudication());
		
		TransformerTestUtils.assertExtensionQuantityEquals(CcwCodebookVariable.FILL_NUM, claim.getFillNumber(),
				rxItem.getQuantity());
		
		TransformerTestUtils.assertExtensionQuantityEquals(CcwCodebookVariable.DAYS_SUPLY_NUM, claim.getDaysSupply(),
				rxItem.getQuantity());
	
		// verify {@link
		// TransformerUtils#mapEobType(CodeableConcept,ClaimType,Optional,Optional)}
		// method worked as expected for this claim type
		TransformerTestUtils.assertMapEobType(eob.getType(), ClaimType.PDE,
				Optional.of(org.hl7.fhir.dstu3.model.codesystems.ClaimType.PHARMACY), Optional.empty(),
				Optional.empty());
	}
}

