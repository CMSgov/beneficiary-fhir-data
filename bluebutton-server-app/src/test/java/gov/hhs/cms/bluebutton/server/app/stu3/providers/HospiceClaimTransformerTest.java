package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.exceptions.FHIRException;
import org.junit.Assert;
import org.junit.Test;

import gov.hhs.cms.bluebutton.data.model.rif.HospiceClaim;
import gov.hhs.cms.bluebutton.data.model.rif.HospiceClaimLine;
import gov.hhs.cms.bluebutton.data.model.rif.samples.StaticRifResource;
import gov.hhs.cms.bluebutton.data.model.rif.samples.StaticRifResourceGroup;
import gov.hhs.cms.bluebutton.server.app.ServerTestUtils;

/**
 * Unit tests for {@link HospiceClaimTransformer}.
 */
public final class HospiceClaimTransformerTest {
	/**
	 * Verifies that {@link HospiceClaimTransformer#transform(Object)} works as
	 * expected when run against the {@link StaticRifResource#SAMPLE_A_HOSPICE}
	 * {@link HospiceClaim}.
	 * 
	 * @throws FHIRException
	 *             (indicates test failure)
	 */
	@Test
	public void transformSampleARecord() throws FHIRException {
		List<Object> parsedRecords = ServerTestUtils
				.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
		HospiceClaim claim = parsedRecords.stream().filter(r -> r instanceof HospiceClaim).map(r -> (HospiceClaim) r)
				.findFirst().get();

		ExplanationOfBenefit eob = HospiceClaimTransformer.transform(claim);
		assertMatches(claim, eob);
	}

	/**
	 * Verifies that the {@link ExplanationOfBenefit} "looks like" it should, if
	 * it were produced from the specified {@link HospiceClaim}.
	 * 
	 * @param claim
	 *            the {@link HospiceClaim} that the {@link ExplanationOfBenefit}
	 *            was generated from
	 * @param eob
	 *            the {@link ExplanationOfBenefit} that was generated from the
	 *            specified {@link HospiceClaim}
	 * @throws FHIRException
	 *             (indicates test failure)
	 */
	static void assertMatches(HospiceClaim claim, ExplanationOfBenefit eob) throws FHIRException {
		TransformerTestUtils.assertNoEncodedOptionals(eob);

		Assert.assertEquals(TransformerUtils.buildEobId(ClaimType.HOSPICE, claim.getClaimId()),
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

		TransformerTestUtils.assertReferenceIdentifierEquals(TransformerConstants.CODING_SYSTEM_PROVIDER_NUMBER,
				claim.getProviderNumber(), eob.getProvider());

		TransformerTestUtils.assertExtensionCodingEquals(eob,
				TransformerConstants.CODING_SYSTEM_CCW_INP_PAYMENT_DENIAL_CD,
				TransformerConstants.CODING_SYSTEM_CCW_INP_PAYMENT_DENIAL_CD,
				claim.getClaimNonPaymentReasonCode().get());
		Assert.assertEquals(claim.getPaymentAmount(), eob.getPayment().getAmount().getValue());
		Assert.assertEquals(claim.getTotalChargeAmount(), eob.getTotalCost().getValue());

		Assert.assertTrue(eob.getInformation().stream()
				.anyMatch(i -> TransformerTestUtils.isCodeInConcept(i.getCategory(),
						TransformerConstants.CODING_SYSTEM_PATIENT_STATUS_CD,
						String.valueOf(claim.getPatientStatusCd().get()))));

		TransformerTestUtils.assertBenefitBalanceUsedEquals(TransformerConstants.BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODING_SYSTEM_UTILIZATION_DAY_COUNT, claim.getUtilizationDayCount().intValue(),
				eob.getBenefitBalanceFirstRep().getFinancial());

		TransformerTestUtils.assertBenefitBalanceEquals(TransformerConstants.BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODED_ADJUDICATION_PRIMARY_PAYER_PAID_AMOUNT, claim.getPrimaryPayerPaidAmount(),
				eob.getBenefitBalanceFirstRep().getFinancial());

		TransformerTestUtils.assertHasCoding(TransformerConstants.CODING_SYSTEM_CCW_CLAIM_TYPE,
				claim.getClaimTypeCode(), eob.getType());

		TransformerTestUtils.assertDateEquals(claim.getClaimHospiceStartDate().get(),
				eob.getHospitalization().getStartElement());
		TransformerTestUtils.assertDateEquals(claim.getBeneficiaryDischargeDate().get(),
				eob.getHospitalization().getEndElement());

		TransformerTestUtils.assertCareTeamEquals(claim.getAttendingPhysicianNpi().get(),
				TransformerConstants.CARE_TEAM_ROLE_PRIMARY, eob);

		TransformerTestUtils.assertReferenceIdentifierEquals(TransformerConstants.CODING_SYSTEM_NPI_US,
				claim.getOrganizationNpi().get(), eob.getOrganization());
		TransformerTestUtils.assertReferenceIdentifierEquals(TransformerConstants.CODING_SYSTEM_NPI_US,
				claim.getOrganizationNpi().get(), eob.getFacility());

		TransformerTestUtils.assertExtensionCodingEquals(eob.getFacility(),
				TransformerConstants.CODING_SYSTEM_CCW_FACILITY_TYPE_CD,
				TransformerConstants.CODING_SYSTEM_CCW_FACILITY_TYPE_CD,
				String.valueOf(claim.getClaimFacilityTypeCode()));

		TransformerTestUtils.assertExtensionCodingEquals(eob.getType(),
				TransformerConstants.CODING_SYSTEM_CCW_CLAIM_SERVICE_CLASSIFICATION_TYPE_CD,
				TransformerConstants.CODING_SYSTEM_CCW_CLAIM_SERVICE_CLASSIFICATION_TYPE_CD,
				String.valueOf(claim.getClaimServiceClassificationTypeCode()));

		Assert.assertEquals(4, eob.getDiagnosis().size());
		Assert.assertEquals(1, eob.getItem().size());
		ItemComponent eobItem0 = eob.getItem().get(0);
		HospiceClaimLine claimLine1 = claim.getLines().get(0);
		Assert.assertEquals(claimLine1.getLineNumber(), new BigDecimal(eobItem0.getSequence()));

		TransformerTestUtils.assertExtensionCodingEquals(eobItem0,
				TransformerConstants.CODING_SYSTEM_FHIR_EOB_ITEM_TYPE,
				TransformerConstants.CODING_SYSTEM_FHIR_EOB_ITEM_TYPE,
				(TransformerConstants.CODED_EOB_ITEM_TYPE_CLINICAL_SERVICES_AND_PRODUCTS));

		Assert.assertEquals(claim.getProviderStateCode(), eobItem0.getLocationAddress().getState());

		TransformerTestUtils.assertHasCoding(TransformerConstants.CODING_SYSTEM_REVENUE_CENTER,
				claimLine1.getRevenueCenterCode(), eobItem0.getRevenue());

		TransformerTestUtils.assertHasCoding(TransformerConstants.CODING_SYSTEM_HCPCS, claimLine1.getHcpcsCode().get(),
				eobItem0.getService());
		TransformerTestUtils.assertHasCoding(TransformerConstants.HCPCS_INITIAL_MODIFIER_CODE1,
				claimLine1.getHcpcsInitialModifierCode().get(), eobItem0.getModifier().get(0));
		Assert.assertFalse(claimLine1.getHcpcsSecondModifierCode().isPresent());

		TransformerTestUtils.assertAdjudicationEquals(TransformerConstants.CODED_ADJUDICATION_RATE_AMOUNT,
				claimLine1.getRateAmount(), eobItem0.getAdjudication());
		TransformerTestUtils.assertAdjudicationEquals(TransformerConstants.CODED_ADJUDICATION_PROVIDER_PAYMENT_AMOUNT,
				claimLine1.getProviderPaymentAmount(), eobItem0.getAdjudication());
		TransformerTestUtils.assertAdjudicationEquals(
				TransformerConstants.CODED_ADJUDICATION_BENEFICIARY_PAYMENT_AMOUNT,
				claimLine1.getBenficiaryPaymentAmount(),
				eobItem0.getAdjudication());
		TransformerTestUtils.assertAdjudicationEquals(TransformerConstants.CODED_ADJUDICATION_PAYMENT,
				claimLine1.getPaymentAmount(), eobItem0.getAdjudication());
		TransformerTestUtils.assertAdjudicationEquals(TransformerConstants.CODED_ADJUDICATION_TOTAL_CHARGE_AMOUNT,
				claimLine1.getTotalChargeAmount(), eobItem0.getAdjudication());
		TransformerTestUtils.assertAdjudicationEquals(TransformerConstants.CODED_ADJUDICATION_NONCOVERED_CHARGE,
				claimLine1.getNonCoveredChargeAmount().get(), eobItem0.getAdjudication());

		TransformerTestUtils.assertExtensionCodingEquals(eobItem0.getRevenue(),
				TransformerConstants.CODING_SYSTEM_DEDUCTIBLE_COINSURANCE_CD,
				TransformerConstants.CODING_SYSTEM_DEDUCTIBLE_COINSURANCE_CD,
				String.valueOf(claimLine1.getDeductibleCoinsuranceCd().get()));

		TransformerTestUtils.assertCareTeamEquals(claimLine1.getRevenueCenterRenderingPhysicianNPI().get(),
				TransformerConstants.CARE_TEAM_ROLE_PRIMARY, eob);

	}
}
