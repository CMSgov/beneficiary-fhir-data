package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.dstu3.model.codesystems.ClaimCareteamrole;
import org.hl7.fhir.exceptions.FHIRException;
import org.junit.Assert;
import org.junit.Test;

import gov.hhs.cms.bluebutton.data.model.rif.SNFClaim;
import gov.hhs.cms.bluebutton.data.model.rif.SNFClaimLine;
import gov.hhs.cms.bluebutton.data.model.rif.samples.StaticRifResource;
import gov.hhs.cms.bluebutton.data.model.rif.samples.StaticRifResourceGroup;
import gov.hhs.cms.bluebutton.server.app.ServerTestUtils;

/**
 * Unit tests for {@link SNFClaimTransformer}.
 */
public final class SNFClaimTransformerTest {
	/**
	 * Verifies that {@link SNFClaimTransformer#transform(Object)} works as
	 * expected when run against the {@link StaticRifResource#SAMPLE_A_SNF}
	 * {@link SNFClaim}.
	 * 
	 * @throws FHIRException
	 *             (indicates test failure)
	 */
	@Test
	public void transformSampleARecord() throws FHIRException {
		List<Object> parsedRecords = ServerTestUtils
				.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
		SNFClaim claim = parsedRecords.stream().filter(r -> r instanceof SNFClaim).map(r -> (SNFClaim) r).findFirst()
				.get();

		ExplanationOfBenefit eob = SNFClaimTransformer.transform(claim);
		assertMatches(claim, eob);
	}

	/**
	 * Verifies that the {@link ExplanationOfBenefit} "looks like" it should, if
	 * it were produced from the specified {@link SNFClaim}.
	 * 
	 * @param claim
	 *            the {@link SNFClaim} that the {@link ExplanationOfBenefit} was
	 *            generated from
	 * @param eob
	 *            the {@link ExplanationOfBenefit} that was generated from the
	 *            specified {@link SNFClaim}
	 * @throws FHIRException
	 *             (indicates test failure)
	 */
	static void assertMatches(SNFClaim claim, ExplanationOfBenefit eob) throws FHIRException {
		TransformerTestUtils.assertNoEncodedOptionals(eob);

		Assert.assertEquals(TransformerUtils.buildEobId(ClaimType.SNF, claim.getClaimId()),
				eob.getIdElement().getIdPart());

		TransformerTestUtils.assertIdentifierExists(TransformerConstants.CODING_CCW_CLAIM_ID, claim.getClaimId(),
				eob.getIdentifier());
		TransformerTestUtils.assertIdentifierExists(TransformerConstants.CODING_CCW_CLAIM_GROUP_ID,
				claim.getClaimGroupId().toPlainString(), eob.getIdentifier());
		Assert.assertEquals(TransformerUtils.referencePatient(claim.getBeneficiaryId()).getReference(),
				eob.getPatient().getReference());
		TransformerTestUtils.assertExtensionCodingEquals(eob.getType(), TransformerConstants.CODING_CCW_RECORD_ID_CODE,
				TransformerConstants.CODING_CCW_RECORD_ID_CODE, "" + claim.getNearLineRecordIdCode());
		TransformerTestUtils.assertHasCoding(TransformerConstants.CODING_CCW_CLAIM_TYPE,
				claim.getClaimTypeCode(), eob.getType());
		Assert.assertEquals(
				TransformerUtils.referenceCoverage(claim.getBeneficiaryId(), MedicareSegment.PART_A).getReference(),
				eob.getInsurance().getCoverage().getReference());

		Assert.assertEquals("active", eob.getStatus().toCode());

		TransformerTestUtils.assertDateEquals(claim.getDateFrom(), eob.getBillablePeriod().getStartElement());
		TransformerTestUtils.assertDateEquals(claim.getDateThrough(), eob.getBillablePeriod().getEndElement());

		TransformerTestUtils.assertExtensionCodingEquals(eob.getBillablePeriod(),
				TransformerConstants.EXTENSION_CODING_CLAIM_QUERY, TransformerConstants.EXTENSION_CODING_CLAIM_QUERY,
				String.valueOf(claim.getClaimQueryCode()));

		TransformerTestUtils.assertReferenceIdentifierEquals(TransformerConstants.IDENTIFIER_CMS_PROVIDER_NUMBER,
				claim.getProviderNumber(), eob.getProvider());
		TransformerTestUtils.assertExtensionCodingEquals(eob,
				TransformerConstants.EXTENSION_CODING_CCW_PAYMENT_DENIAL_REASON,
				TransformerConstants.EXTENSION_CODING_CCW_PAYMENT_DENIAL_REASON,
				claim.getClaimNonPaymentReasonCode().get());

		Assert.assertEquals(claim.getPaymentAmount(), eob.getPayment().getAmount().getValue());
		Assert.assertEquals(claim.getTotalChargeAmount(), eob.getTotalCost().getValue());

		TransformerTestUtils.assertBenefitBalanceEquals(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODED_ADJUDICATION_PRIMARY_PAYER_PAID_AMOUNT, claim.getPrimaryPayerPaidAmount(),
				eob.getBenefitBalanceFirstRep().getFinancial());

		TransformerTestUtils.assertReferenceIdentifierEquals(TransformerConstants.CODING_NPI_US,
				claim.getOrganizationNpi().get(), eob.getOrganization());
		TransformerTestUtils.assertReferenceIdentifierEquals(TransformerConstants.CODING_NPI_US,
				claim.getOrganizationNpi().get(), eob.getFacility());

		TransformerTestUtils.assertExtensionCodingEquals(eob.getFacility(),
				TransformerConstants.EXTENSION_CODING_CCW_FACILITY_TYPE,
				TransformerConstants.EXTENSION_CODING_CCW_FACILITY_TYPE,
				String.valueOf(claim.getClaimFacilityTypeCode()));

		TransformerTestUtils.assertCareTeamEquals(claim.getAttendingPhysicianNpi().get(),
				ClaimCareteamrole.PRIMARY.toCode(), eob);
		TransformerTestUtils.assertCareTeamEquals(claim.getOperatingPhysicianNpi().get(),
				ClaimCareteamrole.ASSIST.toCode(), eob);
		TransformerTestUtils.assertCareTeamEquals(claim.getOtherPhysicianNpi().get(),
				ClaimCareteamrole.OTHER.toCode(), eob);

		TransformerTestUtils.assertBenefitBalanceEquals(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODED_ADJUDICATION_PRIMARY_PAYER_PAID_AMOUNT, claim.getPrimaryPayerPaidAmount(),
				eob.getBenefitBalanceFirstRep().getFinancial());
		TransformerTestUtils.assertBenefitBalanceEquals(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_DEDUCTIBLE, claim.getDeductibleAmount(),
				eob.getBenefitBalanceFirstRep().getFinancial());
		TransformerTestUtils.assertBenefitBalanceEquals(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_COINSURANCE_LIABILITY, claim.getPartACoinsuranceLiabilityAmount(),
				eob.getBenefitBalanceFirstRep().getFinancial());
		TransformerTestUtils.assertBenefitBalanceEquals(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_BLOOD_DEDUCTIBLE_LIABILITY, claim.getBloodDeductibleLiabilityAmount(),
				eob.getBenefitBalanceFirstRep().getFinancial());
		TransformerTestUtils.assertBenefitBalanceEquals(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_NONCOVERED_CHARGE, claim.getNoncoveredCharge(),
				eob.getBenefitBalanceFirstRep().getFinancial());
		TransformerTestUtils.assertBenefitBalanceEquals(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_TOTAL_DEDUCTION, claim.getTotalDeductionAmount(),
				eob.getBenefitBalanceFirstRep().getFinancial());
		TransformerTestUtils.assertBenefitBalanceEquals(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_PPS_CAPITAL_FEDRERAL_PORTION,
				claim.getClaimPPSCapitalFSPAmount().get(), eob.getBenefitBalanceFirstRep().getFinancial());
		TransformerTestUtils.assertBenefitBalanceEquals(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_PPS_CAPITAL_OUTLIER,
				claim.getClaimPPSCapitalOutlierAmount().get(),
				eob.getBenefitBalanceFirstRep().getFinancial());
		TransformerTestUtils.assertBenefitBalanceEquals(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_PPS_CAPITAL_DISPROPORTIONAL_SHARE,
				claim.getClaimPPSCapitalDisproportionateShareAmt().get(),
				eob.getBenefitBalanceFirstRep().getFinancial());
		TransformerTestUtils.assertBenefitBalanceEquals(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_PPS_CAPITAL_INDIRECT_MEDICAL_EDU,
				claim.getClaimPPSCapitalIMEAmount().get(), eob.getBenefitBalanceFirstRep().getFinancial());
		TransformerTestUtils.assertBenefitBalanceEquals(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_PPS_CAPITAL_EXCEPTION,
				claim.getClaimPPSCapitalExceptionAmount().get(), eob.getBenefitBalanceFirstRep().getFinancial());
		TransformerTestUtils.assertBenefitBalanceEquals(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_PPS_OLD_CAPITAL_HOLD_HARMLESS,
				claim.getClaimPPSOldCapitalHoldHarmlessAmount().get(),
				eob.getBenefitBalanceFirstRep().getFinancial());

		TransformerTestUtils.assertBenefitBalanceUsedEquals(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_SYSTEM_UTILIZATION_DAY_COUNT, claim.getUtilizationDayCount().intValue(),
				eob.getBenefitBalanceFirstRep().getFinancial());
		TransformerTestUtils.assertBenefitBalanceUsedEquals(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODING_CCW_COINSURANCE_DAY_COUNT, claim.getCoinsuranceDayCount().intValue(),
				eob.getBenefitBalanceFirstRep().getFinancial());
		TransformerTestUtils.assertBenefitBalanceEquals(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_NON_UTILIZATION_DAY_COUNT,
				claim.getNonUtilizationDayCount().intValue(),
				eob.getBenefitBalanceFirstRep().getFinancial());
		TransformerTestUtils.assertBenefitBalanceUsedEquals(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_BLOOD_PINTS_FURNISHED,
				claim.getBloodPintsFurnishedQty().intValue(),
				eob.getBenefitBalanceFirstRep().getFinancial());

		TransformerTestUtils.assertInformationPeriodEquals(TransformerConstants.CODING_BBAPI_BENEFIT_COVERAGE_DATE,
				TransformerConstants.CODED_BENEFIT_COVERAGE_DATE_QUALIFIED, claim.getQualifiedStayFromDate().get(),
				claim.getQualifiedStayThroughDate().get(), eob.getInformation());

		TransformerTestUtils.assertInformationPeriodEquals(TransformerConstants.CODING_BBAPI_BENEFIT_COVERAGE_DATE,
				TransformerConstants.CODED_BENEFIT_COVERAGE_DATE_NONCOVERED, claim.getNoncoveredStayFromDate().get(),
				claim.getNoncoveredStayThroughDate().get(), eob.getInformation());

		TransformerTestUtils.assertInformationDateEquals(TransformerConstants.CODING_BBAPI_BENEFIT_COVERAGE_DATE,
				TransformerConstants.CODED_BENEFIT_COVERAGE_DATE_EXHAUSTED,
				claim.getMedicareBenefitsExhaustedDate().get(),
				eob.getInformation());

		TransformerTestUtils.assertDateEquals(claim.getClaimAdmissionDate().get(),
				eob.getHospitalization().getStartElement());
		TransformerTestUtils.assertDateEquals(claim.getBeneficiaryDischargeDate().get(),
				eob.getHospitalization().getEndElement());

		Assert.assertTrue(eob.getInformation().stream()
				.anyMatch(i -> TransformerTestUtils.isCodeInConcept(i.getCategory(),
						TransformerConstants.CODING_CCW_DIAGNOSIS_RELATED_GROUP,
						String.valueOf(claim.getDiagnosisRelatedGroupCd().get()))));

		Assert.assertEquals(5, eob.getDiagnosis().size());

		CCWProcedure ccwProcedure = new CCWProcedure(claim.getProcedure1Code(), claim.getProcedure1CodeVersion(),
				claim.getProcedure1Date().get());
		TransformerTestUtils.assertHasCoding(ccwProcedure.getFhirSystem().toString(), claim.getProcedure1Code().get(),
				eob.getProcedure().get(0).getProcedureCodeableConcept());
		Assert.assertEquals(Date.from(claim.getProcedure1Date().get().atStartOfDay(ZoneId.systemDefault()).toInstant()),
				eob.getProcedure().get(0).getDate());

		Assert.assertEquals(1, eob.getItem().size());
		SNFClaimLine claimLine1 = claim.getLines().get(0);
		ItemComponent eobItem0 = eob.getItem().get(0);
		Assert.assertEquals(claimLine1.getLineNumber(),
				new BigDecimal(eobItem0.getSequence()));

		TransformerTestUtils.assertExtensionCodingEquals(eobItem0,
				TransformerConstants.CODING_FHIR_ACT_INVOICE_GROUP,
				TransformerConstants.CODING_FHIR_ACT_INVOICE_GROUP,
				(TransformerConstants.CODED_ACT_INVOICE_GROUP_CLINICAL_SERVICES_AND_PRODUCTS));

		Assert.assertEquals(claim.getProviderStateCode(),
				eobItem0.getLocationAddress().getState());

		TransformerTestUtils.assertHasCoding(TransformerConstants.CODING_CMS_REVENUE_CENTER,
				claimLine1.getRevenueCenter(), eobItem0.getRevenue());
		TransformerTestUtils.assertHasCoding(TransformerConstants.CODING_HCPCS, claimLine1.getHcpcsCode().get(),
				eobItem0.getService());
		TransformerTestUtils.assertAdjudicationEquals(TransformerConstants.CODED_ADJUDICATION_RATE_AMOUNT,
				claimLine1.getRateAmount(), eobItem0.getAdjudication());
		TransformerTestUtils.assertAdjudicationEquals(TransformerConstants.CODED_ADJUDICATION_NONCOVERED_CHARGE,
				claimLine1.getNonCoveredChargeAmount(), eobItem0.getAdjudication());
		TransformerTestUtils.assertAdjudicationEquals(TransformerConstants.CODED_ADJUDICATION_TOTAL_CHARGE_AMOUNT,
				claimLine1.getTotalChargeAmount(), eobItem0.getAdjudication());

		TransformerTestUtils.assertExtensionCodingEquals(eobItem0.getRevenue(),
				TransformerConstants.EXTENSION_CODING_CCW_DEDUCTIBLE_COINSURANCE_CODE,
				TransformerConstants.EXTENSION_CODING_CCW_DEDUCTIBLE_COINSURANCE_CODE,
				String.valueOf(claimLine1.getDeductibleCoinsuranceCd().get()));

		TransformerTestUtils.assertCareTeamEquals(claimLine1.getRevenueCenterRenderingPhysicianNPI().get(),
				ClaimCareteamrole.PRIMARY.toCode(), eob);

	}
}
