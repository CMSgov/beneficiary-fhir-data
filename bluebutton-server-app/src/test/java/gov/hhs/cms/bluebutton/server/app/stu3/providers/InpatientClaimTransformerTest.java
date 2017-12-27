package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.exceptions.FHIRException;
import org.junit.Assert;
import org.junit.Test;

import gov.hhs.cms.bluebutton.data.model.rif.InpatientClaim;
import gov.hhs.cms.bluebutton.data.model.rif.InpatientClaimLine;
import gov.hhs.cms.bluebutton.data.model.rif.samples.StaticRifResource;
import gov.hhs.cms.bluebutton.data.model.rif.samples.StaticRifResourceGroup;
import gov.hhs.cms.bluebutton.server.app.ServerTestUtils;

/**
 * Unit tests for {@link InpatientClaimTransformer}.
 */
public final class InpatientClaimTransformerTest {
	/**
	 * Verifies that {@link InpatientClaimTransformer#transform(Object)} works
	 * as expected when run against the
	 * {@link StaticRifResource#SAMPLE_A_INPATIENT} {@link InpatientClaim}.
	 * 
	 * @throws FHIRException
	 *             (indicates test failure)
	 */
	@Test
	public void transformSampleARecord() throws FHIRException {
		List<Object> parsedRecords = ServerTestUtils
				.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
		InpatientClaim claim = parsedRecords.stream().filter(r -> r instanceof InpatientClaim)
				.map(r -> (InpatientClaim) r).findFirst().get();

		ExplanationOfBenefit eob = InpatientClaimTransformer.transform(claim);
		assertMatches(claim, eob);
	}

	/**
	 * Verifies that the {@link ExplanationOfBenefit} "looks like" it should, if
	 * it were produced from the specified {@link InpatientClaim}.
	 * 
	 * @param claim
	 *            the {@link InpatientClaim} that the {@link ExplanationOfBenefit} was
	 *            generated from
	 * @param eob
	 *            the {@link ExplanationOfBenefit} that was generated from the
	 *            specified {@link InpatientClaim}
	 * @throws FHIRException
	 *             (indicates test failure)
	 */
	static void assertMatches(InpatientClaim claim, ExplanationOfBenefit eob) throws FHIRException {
		TransformerTestUtils.assertNoEncodedOptionals(eob);

		Assert.assertEquals(TransformerUtils.buildEobId(ClaimType.INPATIENT, claim.getClaimId()),
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
		Assert.assertEquals(
				TransformerUtils.referenceCoverage(claim.getBeneficiaryId(), MedicareSegment.PART_A).getReference(),
				eob.getInsurance().getCoverage().getReference());
		Assert.assertEquals("active", eob.getStatus().toCode());

		TransformerTestUtils.assertDateEquals(claim.getDateFrom(), eob.getBillablePeriod().getStartElement());
		TransformerTestUtils.assertDateEquals(claim.getDateThrough(), eob.getBillablePeriod().getEndElement());

		TransformerTestUtils.assertExtensionCodingEquals(eob.getBillablePeriod(), TransformerConstants.CODING_SYSTEM_QUERY_CD,
				TransformerConstants.CODING_SYSTEM_QUERY_CD, String.valueOf(claim.getClaimQueryCode()));

		Assert.assertEquals(claim.getPaymentAmount(), eob.getPayment().getAmount().getValue());
		Assert.assertEquals(claim.getTotalChargeAmount(), eob.getTotalCost().getValue());

		TransformerTestUtils.assertDateEquals(claim.getClaimAdmissionDate().get(), eob.getHospitalization().getStartElement());
		TransformerTestUtils.assertDateEquals(claim.getBeneficiaryDischargeDate().get(), eob.getHospitalization().getEndElement());

		TransformerTestUtils.assertBenefitBalanceEquals(TransformerConstants.BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODED_ADJUDICATION_PRIMARY_PAYER_PAID_AMOUNT,
				claim.getPrimaryPayerPaidAmount(), eob.getBenefitBalanceFirstRep().getFinancial());
		TransformerTestUtils.assertBenefitBalanceEquals(TransformerConstants.BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODING_BENEFIT_DEDUCTIBLE_AMT_URL, claim.getDeductibleAmount(),
				eob.getBenefitBalanceFirstRep().getFinancial());
		TransformerTestUtils.assertBenefitBalanceEquals(TransformerConstants.BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODING_CLAIM_PASS_THRU_PER_DIEM_AMT, claim.getPassThruPerDiemAmount(),
				eob.getBenefitBalanceFirstRep().getFinancial());
		TransformerTestUtils.assertBenefitBalanceEquals(TransformerConstants.BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODING_NCH_BENEFIT_COIN_AMT_URL, claim.getPartACoinsuranceLiabilityAmount(),
				eob.getBenefitBalanceFirstRep().getFinancial());
		TransformerTestUtils.assertBenefitBalanceEquals(TransformerConstants.BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODING_NCH_BENEFIT_BLOOD_DED_AMT_URL, claim.getBloodDeductibleLiabilityAmount(),
				eob.getBenefitBalanceFirstRep().getFinancial());
		TransformerTestUtils.assertBenefitBalanceEquals(TransformerConstants.BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODING_NCH_PROFFESIONAL_CHARGE_URL, claim.getProfessionalComponentCharge(),
				eob.getBenefitBalanceFirstRep().getFinancial());
		TransformerTestUtils.assertBenefitBalanceEquals(TransformerConstants.BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODING_NCH_INPATIENT_NONCOVERED_CHARGE_URL, claim.getNoncoveredCharge(),
				eob.getBenefitBalanceFirstRep().getFinancial());
		TransformerTestUtils.assertBenefitBalanceEquals(TransformerConstants.BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODING_NCH_INPATIENT_TOTAL_AMT_URL, claim.getTotalDeductionAmount(),
				eob.getBenefitBalanceFirstRep().getFinancial());
		TransformerTestUtils.assertBenefitBalanceEquals(TransformerConstants.BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODING_CLAIM_TOTAL_PPS_CAPITAL_AMT_URL, claim.getClaimTotalPPSCapitalAmount().get(),
				eob.getBenefitBalanceFirstRep().getFinancial());
		TransformerTestUtils.assertBenefitBalanceEquals(TransformerConstants.BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODING_CLAIM_PPS_CAPITAL_FEDERAL_PORTION_AMT_URL, claim.getClaimPPSCapitalFSPAmount().get(),
				eob.getBenefitBalanceFirstRep().getFinancial());
		TransformerTestUtils.assertBenefitBalanceEquals(TransformerConstants.BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODING_CLAIM_PPS_CAPITAL_OUTLIER_AMT_URL, claim.getClaimPPSCapitalOutlierAmount().get(),
				eob.getBenefitBalanceFirstRep().getFinancial());
		TransformerTestUtils.assertBenefitBalanceEquals(TransformerConstants.BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODING_CLAIM_PPS_CAPITAL_DISPROPORTIONAL_SHARE_AMT_URL,
				claim.getClaimPPSCapitalDisproportionateShareAmt().get(), eob.getBenefitBalanceFirstRep().getFinancial());
		TransformerTestUtils.assertBenefitBalanceEquals(TransformerConstants.BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODING_CLAIM_PPS_CAPITAL_INDIRECT_MEDICAL_EDU_AMT_URL,
				claim.getClaimPPSCapitalIMEAmount().get(), eob.getBenefitBalanceFirstRep().getFinancial());
		TransformerTestUtils.assertBenefitBalanceEquals(TransformerConstants.BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODING_CLAIM_PPS_CAPITAL_EXCEPTION_AMT_URL, claim.getClaimPPSCapitalExceptionAmount().get(),
				eob.getBenefitBalanceFirstRep().getFinancial());
		TransformerTestUtils.assertBenefitBalanceEquals(TransformerConstants.BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODING_CLAIM_PPS_OLD_CAPITAL_HOLD_HARMLESS_AMT_URL,
				claim.getClaimPPSOldCapitalHoldHarmlessAmount().get(),
				eob.getBenefitBalanceFirstRep().getFinancial());

		TransformerTestUtils.assertBenefitBalanceUsedEquals(TransformerConstants.BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODING_SYSTEM_UTILIZATION_DAY_COUNT, claim.getUtilizationDayCount().intValue(),
				eob.getBenefitBalanceFirstRep().getFinancial());
		TransformerTestUtils.assertBenefitBalanceUsedEquals(TransformerConstants.BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODING_SYSTEM_COINSURANCE_DAY_COUNT, claim.getCoinsuranceDayCount().intValue(),
				eob.getBenefitBalanceFirstRep().getFinancial());
		TransformerTestUtils.assertBenefitBalanceEquals(TransformerConstants.BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODING_SYSTEM_NON_UTILIZATION_DAY_COUNT,
				claim.getNonUtilizationDayCount().intValue(),
				eob.getBenefitBalanceFirstRep().getFinancial());
		TransformerTestUtils.assertBenefitBalanceUsedEquals(TransformerConstants.BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODING_SYSTEM_BLOOD_PINTS_FURNISHED_QTY, claim.getBloodPintsFurnishedQty().intValue(),
				eob.getBenefitBalanceFirstRep().getFinancial());

		TransformerTestUtils.assertBenefitBalanceEquals(TransformerConstants.BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODING_NCH_DRUG_OUTLIER_APPROVED_PAYMENT_AMT_URL,
				claim.getDrgOutlierApprovedPaymentAmount().get(), eob.getBenefitBalanceFirstRep().getFinancial());

		TransformerTestUtils.assertReferenceIdentifierEquals(TransformerConstants.CODING_SYSTEM_NPI_US, claim.getOrganizationNpi().get(),
				eob.getOrganization());
		TransformerTestUtils.assertReferenceIdentifierEquals(TransformerConstants.CODING_SYSTEM_NPI_US, claim.getOrganizationNpi().get(),
				eob.getFacility());

		TransformerTestUtils.assertExtensionCodingEquals(eob.getFacility(), TransformerConstants.CODING_SYSTEM_CCW_FACILITY_TYPE_CD,
				TransformerConstants.CODING_SYSTEM_CCW_FACILITY_TYPE_CD, String.valueOf(claim.getClaimFacilityTypeCode()));

		TransformerTestUtils.assertExtensionCodingEquals(eob.getType(),
				TransformerConstants.CODING_SYSTEM_CCW_CLAIM_SERVICE_CLASSIFICATION_TYPE_CD,
				TransformerConstants.CODING_SYSTEM_CCW_CLAIM_SERVICE_CLASSIFICATION_TYPE_CD,
				String.valueOf(claim.getClaimServiceClassificationTypeCode()));

		TransformerTestUtils.assertCareTeamEquals(claim.getAttendingPhysicianNpi().get(), TransformerConstants.CARE_TEAM_ROLE_PRIMARY, eob);
		TransformerTestUtils.assertCareTeamEquals(claim.getOperatingPhysicianNpi().get(), TransformerConstants.CARE_TEAM_ROLE_ASSISTING, eob);
		TransformerTestUtils.assertCareTeamEquals(claim.getOtherPhysicianNpi().get(), TransformerConstants.CARE_TEAM_ROLE_OTHER, eob);

		Assert.assertTrue(eob.getInformation().stream()
				.anyMatch(i -> TransformerTestUtils.isCodeInConcept(i.getCategory(),
						TransformerConstants.CODING_SYSTEM_DIAGNOSIS_RELATED_GROUP_CD,
						String.valueOf(claim.getDiagnosisRelatedGroupCd().get()))));

		Assert.assertEquals(9, eob.getDiagnosis().size());

		CCWProcedure ccwProcedure = new CCWProcedure(claim.getProcedure1Code(), claim.getProcedure1CodeVersion(),
				claim.getProcedure1Date());
		TransformerTestUtils.assertHasCoding(ccwProcedure.getFhirSystem().toString(),
				claim.getProcedure1Code().get(),
				eob.getProcedure().get(0).getProcedureCodeableConcept());
		Assert.assertEquals(
				Date.from(claim.getProcedure1Date().get().atStartOfDay(ZoneId.systemDefault()).toInstant()),
				eob.getProcedure().get(0).getDate());

		Assert.assertEquals(1, eob.getItem().size());
		ItemComponent eobItem0 = eob.getItem().get(0);
		InpatientClaimLine claimLine1 = claim.getLines().get(0);
		Assert.assertEquals(claimLine1.getLineNumber(), new BigDecimal(eobItem0.getSequence()));

		TransformerTestUtils.assertExtensionCodingEquals(eobItem0, TransformerConstants.CODING_SYSTEM_FHIR_EOB_ITEM_TYPE,
				TransformerConstants.CODING_SYSTEM_FHIR_EOB_ITEM_TYPE,
				(TransformerConstants.CODED_EOB_ITEM_TYPE_CLINICAL_SERVICES_AND_PRODUCTS));

		Assert.assertEquals(claim.getProviderStateCode(), eobItem0.getLocationAddress().getState());

		TransformerTestUtils.assertCareTeamEquals(claimLine1.getRevenueCenterRenderingPhysicianNPI().get(),
				TransformerConstants.CARE_TEAM_ROLE_PRIMARY, eob);

		TransformerTestUtils.assertHasCoding(TransformerConstants.CODING_SYSTEM_REVENUE_CENTER,
				claimLine1.getRevenueCenter(), eobItem0.getRevenue());

		TransformerTestUtils.assertHasCoding(TransformerConstants.CODING_SYSTEM_HCPCS, claimLine1.getHcpcsCode().get(),
				eobItem0.getService());

		TransformerTestUtils.assertAdjudicationEquals(TransformerConstants.CODED_ADJUDICATION_RATE_AMOUNT,
				claimLine1.getRateAmount(),
				eobItem0.getAdjudication());

		TransformerTestUtils.assertAdjudicationEquals(TransformerConstants.CODED_ADJUDICATION_NONCOVERED_CHARGE,
				claimLine1.getNonCoveredChargeAmount(),
				eobItem0.getAdjudication());
		TransformerTestUtils.assertAdjudicationEquals(TransformerConstants.CODED_ADJUDICATION_TOTAL_CHARGE_AMOUNT,
				claimLine1.getTotalChargeAmount(),
				eobItem0.getAdjudication());

		TransformerTestUtils.assertExtensionCodingEquals(eobItem0.getRevenue(), TransformerConstants.CODING_SYSTEM_DEDUCTIBLE_COINSURANCE_CD,
				TransformerConstants.CODING_SYSTEM_DEDUCTIBLE_COINSURANCE_CD,
				String.valueOf(claimLine1.getDeductibleCoinsuranceCd().get()));
	}
		
}
