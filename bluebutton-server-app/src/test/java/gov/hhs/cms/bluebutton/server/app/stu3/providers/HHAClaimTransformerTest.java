package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.exceptions.FHIRException;
import org.junit.Assert;
import org.junit.Test;

import gov.hhs.cms.bluebutton.data.model.rif.HHAClaim;
import gov.hhs.cms.bluebutton.data.model.rif.HHAClaimLine;
import gov.hhs.cms.bluebutton.data.model.rif.samples.StaticRifResource;
import gov.hhs.cms.bluebutton.data.model.rif.samples.StaticRifResourceGroup;
import gov.hhs.cms.bluebutton.server.app.ServerTestUtils;

/**
 * Unit tests for {@link HHAClaimTransformer}.
 */
public final class HHAClaimTransformerTest {
	/**
	 * Verifies that {@link HHAClaimTransformer#transform(Object)} works as
	 * expected when run against the {@link StaticRifResource#SAMPLE_A_HHA}
	 * {@link HHAClaim}.
	 * 
	 * @throws FHIRException
	 *             (indicates test failure)
	 */
	@Test
	public void transformSampleARecord() throws FHIRException {
		List<Object> parsedRecords = ServerTestUtils
				.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
		HHAClaim claim = parsedRecords.stream().filter(r -> r instanceof HHAClaim).map(r -> (HHAClaim) r).findFirst()
				.get();

		ExplanationOfBenefit eob = HHAClaimTransformer.transform(claim);
		assertMatches(claim, eob);
	}

	/**
	 * Verifies that the {@link ExplanationOfBenefit} "looks like" it should, if
	 * it were produced from the specified {@link HHAClaim}.
	 * 
	 * @param claim
	 *            the {@link HHAClaim} that the {@link ExplanationOfBenefit} was
	 *            generated from
	 * @param eob
	 *            the {@link ExplanationOfBenefit} that was generated from the
	 *            specified {@link HHAClaim}
	 * @throws FHIRException
	 *             (indicates test failure)
	 */
	static void assertMatches(HHAClaim claim, ExplanationOfBenefit eob) throws FHIRException {
		// Test to ensure group level fields between all claim types match
		TransformerTestUtils.assertEobCommonClaimHeaderData(eob, claim.getClaimId(), claim.getBeneficiaryId(),
				ClaimType.HHA, claim.getClaimGroupId().toPlainString(), MedicareSegment.PART_B,
				Optional.of(claim.getDateFrom()), Optional.of(claim.getDateThrough()),
				Optional.of(claim.getPaymentAmount()), claim.getFinalAction());

		// test the common field provider number is set as expected in the EOB
		TransformerTestUtils.assertProviderNumber(eob, claim.getProviderNumber());

		// Test to ensure common group fields between Inpatient, Outpatient HHA, Hospice
		// and SNF match
		TransformerTestUtils.assertEobCommonGroupInpOutHHAHospiceSNFEquals(eob, claim.getOrganizationNpi(),
				claim.getClaimFacilityTypeCode(), claim.getClaimFrequencyCode(), claim.getClaimNonPaymentReasonCode(),
				claim.getPatientDischargeStatusCode(), claim.getClaimServiceClassificationTypeCode(),
				claim.getClaimPrimaryPayerCode(), claim.getAttendingPhysicianNpi(), claim.getTotalChargeAmount(),
				claim.getPrimaryPayerPaidAmount());

		Assert.assertEquals(4, eob.getDiagnosis().size());

		TransformerTestUtils.assertBenefitBalanceUsedEquals(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_VISIT_COUNT, claim.getTotalVisitCount().intValue(),
				eob.getBenefitBalanceFirstRep().getFinancial());

		TransformerTestUtils.assertDateEquals(claim.getCareStartDate().get(), eob.getHospitalization().getStartElement());

		Assert.assertEquals(1, eob.getItem().size());
		ItemComponent eobItem0 = eob.getItem().get(0);
		HHAClaimLine claimLine1 = claim.getLines().get(0);
		Assert.assertEquals(claimLine1.getLineNumber(), new BigDecimal(eobItem0.getSequence()));

		TransformerTestUtils.assertExtensionCodingEquals(eobItem0, TransformerConstants.CODING_FHIR_ACT_INVOICE_GROUP,
				TransformerConstants.CODING_FHIR_ACT_INVOICE_GROUP,
				(TransformerConstants.CODED_ACT_INVOICE_GROUP_CLINICAL_SERVICES_AND_PRODUCTS));

		Assert.assertEquals(claim.getProviderStateCode(), eobItem0.getLocationAddress().getState());

		TransformerTestUtils.assertAdjudicationReasonEquals(TransformerConstants.CODED_ADJUDICATION_1ST_ANSI_CD,
				TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY, claimLine1.getRevCntr1stAnsiCd().get(),
				eobItem0.getAdjudication());

		TransformerTestUtils.assertHcpcsModiferCodes(eobItem0, claimLine1.getHcpcsInitialModifierCode(),
				claimLine1.getHcpcsSecondModifierCode(), Optional.empty(), 0/* index */, claimLine1.getHcpcsCode());
		TransformerTestUtils.assertAdjudicationEquals(TransformerConstants.CODED_ADJUDICATION_RATE_AMOUNT,
				claimLine1.getRateAmount(),
				eobItem0.getAdjudication());
		TransformerTestUtils.assertAdjudicationEquals(TransformerConstants.CODED_ADJUDICATION_PAYMENT,
				claimLine1.getPaymentAmount(),
				eobItem0.getAdjudication());
		
		TransformerTestUtils.assertExtensionCodingEquals(eobItem0.getRevenue(), TransformerConstants.EXTENSION_CODING_CCW_DEDUCTIBLE_COINSURANCE_CODE,
				TransformerConstants.EXTENSION_CODING_CCW_DEDUCTIBLE_COINSURANCE_CODE,
				String.valueOf(claimLine1.getDeductibleCoinsuranceCd().get()));

		// Test to ensure item level fields between Inpatient, Outpatient, HHA, Hopsice
		// and SNF match
		TransformerTestUtils.assertEobCommonItemRevenueEquals(eobItem0, eob, claimLine1.getRevenueCenterCode(),
				claimLine1.getRateAmount(), claimLine1.getTotalChargeAmount(), claimLine1.getNonCoveredChargeAmount(),
				claimLine1.getUnitCount(), claimLine1.getNationalDrugCodeQuantity(),
				claimLine1.getNationalDrugCodeQualifierCode(), claimLine1.getRevenueCenterRenderingPhysicianNPI());

		// verify {@link
		// TransformerUtils#mapEobType(CodeableConcept,ClaimType,Optional,Optional)}
		// method worked as expected for this claim type
		TransformerTestUtils.assertMapEobType(eob.getType(), ClaimType.HHA,
				// FUTURE there currently is not an equivalent CODING_FHIR_CLAIM_TYPE mapping
				// for this claim type. If added then the Optional empty parameter below should
				// be updated to match expected result.
				Optional.empty(), Optional.of(claim.getNearLineRecordIdCode()), Optional.of(claim.getClaimTypeCode()));
	}
}

