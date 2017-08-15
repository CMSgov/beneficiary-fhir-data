package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

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
		TransformerTestUtils.assertNoEncodedOptionals(eob);

		Assert.assertEquals(TransformerUtils.buildEobId(ClaimType.HHA, claim.getClaimId()),
				eob.getIdElement().getIdPart());

		TransformerTestUtils.assertIdentifierExists(TransformerConstants.CODING_SYSTEM_CCW_CLAIM_ID, claim.getClaimId(),
				eob.getIdentifier());
		TransformerTestUtils.assertIdentifierExists(TransformerConstants.CODING_SYSTEM_CCW_CLAIM_GRP_ID,
				claim.getClaimGroupId().toPlainString(), eob.getIdentifier());
		Assert.assertEquals(TransformerUtils.referencePatient(claim.getBeneficiaryId()).getReference(),
				eob.getPatient().getReference());
		TransformerTestUtils.assertExtensionCodingEquals(eob.getType(),
				TransformerConstants.CODING_SYSTEM_CCW_RECORD_ID_CD,
				TransformerConstants.CODING_SYSTEM_CCW_RECORD_ID_CD, "" + claim.getNearLineRecordIdCode());
		TransformerTestUtils.assertHasCoding(TransformerConstants.CODING_SYSTEM_CCW_CLAIM_TYPE,
				claim.getClaimTypeCode(), eob.getType());
		Assert.assertEquals("active", eob.getStatus().toCode());

		TransformerTestUtils.assertDateEquals(claim.getDateFrom(), eob.getBillablePeriod().getStartElement());
		TransformerTestUtils.assertDateEquals(claim.getDateThrough(), eob.getBillablePeriod().getEndElement());

		TransformerTestUtils.assertReferenceIdentifierEquals(TransformerConstants.CODING_SYSTEM_PROVIDER_NUMBER, claim.getProviderNumber(),
				eob.getProvider());

		TransformerTestUtils.assertExtensionCodingEquals(eob, TransformerConstants.CODING_SYSTEM_CCW_INP_PAYMENT_DENIAL_CD,
				TransformerConstants.CODING_SYSTEM_CCW_INP_PAYMENT_DENIAL_CD, claim.getClaimNonPaymentReasonCode().get());
		Assert.assertEquals(claim.getPaymentAmount(), eob.getPayment().getAmount().getValue());
		Assert.assertEquals(claim.getTotalChargeAmount(), eob.getTotalCost().getValue());
		
		TransformerTestUtils.assertExtensionCodingEquals(eob.getType(),
				TransformerConstants.CODING_SYSTEM_CCW_CLAIM_SERVICE_CLASSIFICATION_TYPE_CD,
				TransformerConstants.CODING_SYSTEM_CCW_CLAIM_SERVICE_CLASSIFICATION_TYPE_CD,
				String.valueOf(claim.getClaimServiceClassificationTypeCode()));
		
		TransformerTestUtils.assertBenefitBalanceEquals(TransformerConstants.BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODED_ADJUDICATION_PRIMARY_PAYER_PAID_AMOUNT,
				claim.getPrimaryPayerPaidAmount(), eob.getBenefitBalanceFirstRep().getFinancial());

		TransformerTestUtils.assertReferenceIdentifierEquals(TransformerConstants.CODING_SYSTEM_NPI_US, claim.getOrganizationNpi().get(),
				eob.getOrganization());
		TransformerTestUtils.assertReferenceIdentifierEquals(TransformerConstants.CODING_SYSTEM_NPI_US, claim.getOrganizationNpi().get(),
				eob.getFacility());

		TransformerTestUtils.assertExtensionCodingEquals(eob.getFacility(), TransformerConstants.CODING_SYSTEM_CCW_FACILITY_TYPE_CD,
				TransformerConstants.CODING_SYSTEM_CCW_FACILITY_TYPE_CD, String.valueOf(claim.getClaimFacilityTypeCode()));

		TransformerTestUtils.assertCareTeamEquals(claim.getAttendingPhysicianNpi().get(), TransformerConstants.CARE_TEAM_ROLE_PRIMARY, eob);

		Assert.assertEquals(4, eob.getDiagnosis().size());

		TransformerTestUtils.assertBenefitBalanceUsedEquals(TransformerConstants.BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODING_SYSTEM_HHA_VISIT_COUNT, claim.getTotalVisitCount().intValue(),
				eob.getBenefitBalanceFirstRep().getFinancial());

		TransformerTestUtils.assertDateEquals(claim.getCareStartDate().get(), eob.getHospitalization().getStartElement());

		Assert.assertEquals(1, eob.getItem().size());
		ItemComponent eobItem0 = eob.getItem().get(0);
		HHAClaimLine claimLine1 = claim.getLines().get(0);
		Assert.assertEquals(claimLine1.getLineNumber(), new BigDecimal(eobItem0.getSequence()));

		TransformerTestUtils.assertExtensionCodingEquals(eobItem0, TransformerConstants.CODING_SYSTEM_FHIR_EOB_ITEM_TYPE,
				TransformerConstants.CODING_SYSTEM_FHIR_EOB_ITEM_TYPE,
				(TransformerConstants.CODED_EOB_ITEM_TYPE_CLINICAL_SERVICES_AND_PRODUCTS));

		Assert.assertEquals(claim.getProviderStateCode(), eobItem0.getLocationAddress().getState());

		TransformerTestUtils.assertAdjudicationReasonEquals(TransformerConstants.CODED_ADJUDICATION_1ST_ANSI_CD,
				TransformerConstants.CODING_SYSTEM_ADJUDICATION_CMS, claimLine1.getRevCntr1stAnsiCd().get(),
				eobItem0.getAdjudication());

		TransformerTestUtils.assertHasCoding(TransformerConstants.CODING_SYSTEM_HCPCS, claimLine1.getHcpcsCode().get(),
				eobItem0.getService());
		TransformerTestUtils.assertHasCoding(TransformerConstants.HCPCS_INITIAL_MODIFIER_CODE1,
				claimLine1.getHcpcsInitialModifierCode().get(),
				eobItem0.getModifier().get(0));
		Assert.assertFalse(claimLine1.getHcpcsSecondModifierCode().isPresent());
			
		TransformerTestUtils.assertAdjudicationEquals(TransformerConstants.CODED_ADJUDICATION_RATE_AMOUNT,
				claimLine1.getRateAmount(),
				eobItem0.getAdjudication());
		TransformerTestUtils.assertAdjudicationEquals(TransformerConstants.CODED_ADJUDICATION_PAYMENT,
				claimLine1.getPaymentAmount(),
				eobItem0.getAdjudication());
		TransformerTestUtils.assertAdjudicationEquals(TransformerConstants.CODED_ADJUDICATION_TOTAL_CHARGE_AMOUNT,
				claimLine1.getTotalChargeAmount(),
				eobItem0.getAdjudication());
		TransformerTestUtils.assertAdjudicationEquals(TransformerConstants.CODED_ADJUDICATION_NONCOVERED_CHARGE,
				claimLine1.getNonCoveredChargeAmount(), eobItem0.getAdjudication());
		
		TransformerTestUtils.assertExtensionCodingEquals(eobItem0.getRevenue(), TransformerConstants.CODING_SYSTEM_DEDUCTIBLE_COINSURANCE_CD,
				TransformerConstants.CODING_SYSTEM_DEDUCTIBLE_COINSURANCE_CD,
				String.valueOf(claimLine1.getDeductibleCoinsuranceCd().get()));
		
		TransformerTestUtils.assertCareTeamEquals(claimLine1.getRevenueCenterRenderingPhysicianNPI().get(),
				TransformerConstants.CARE_TEAM_ROLE_PRIMARY, eob);

	}
}
