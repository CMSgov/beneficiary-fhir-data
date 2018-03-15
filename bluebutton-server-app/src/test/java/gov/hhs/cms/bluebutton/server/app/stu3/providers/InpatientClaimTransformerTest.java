package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.exceptions.FHIRException;
import org.junit.Assert;
import org.junit.Test;

import gov.hhs.cms.bluebutton.data.codebook.data.CcwCodebookVariable;
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
		// Test to ensure group level fields between all claim types match
		TransformerTestUtils.assertEobCommonClaimHeaderData(eob, claim.getClaimId(), claim.getBeneficiaryId(),
				ClaimType.INPATIENT, claim.getClaimGroupId().toPlainString(), MedicareSegment.PART_A,
				Optional.of(claim.getDateFrom()), Optional.of(claim.getDateThrough()),
				Optional.of(claim.getPaymentAmount()), claim.getFinalAction());

		// test the common field provider number is set as expected in the EOB
		TransformerTestUtils.assertProviderNumber(eob, claim.getProviderNumber());

		TransformerTestUtils.assertAdjudicationTotalAmountEquals(CcwCodebookVariable.CLM_PASS_THRU_PER_DIEM_AMT,
				claim.getPassThruPerDiemAmount(), eob);
		TransformerTestUtils.assertAdjudicationTotalAmountEquals(CcwCodebookVariable.NCH_PROFNL_CMPNT_CHRG_AMT,
				claim.getProfessionalComponentCharge(), eob);
		TransformerTestUtils.assertAdjudicationTotalAmountEquals(CcwCodebookVariable.CLM_TOT_PPS_CPTL_AMT,
				claim.getClaimTotalPPSCapitalAmount(), eob);
		TransformerTestUtils.assertAdjudicationTotalAmountEquals(CcwCodebookVariable.IME_OP_CLM_VAL_AMT,
				claim.getIndirectMedicalEducationAmount(), eob);
		TransformerTestUtils.assertAdjudicationTotalAmountEquals(CcwCodebookVariable.DSH_OP_CLM_VAL_AMT,
				claim.getDisproportionateShareAmount(), eob);
		
		// test common eob information between Inpatient, HHA, Hospice and SNF claims are set as expected
		TransformerTestUtils.assertEobCommonGroupInpHHAHospiceSNFEquals(eob, claim.getClaimAdmissionDate(), 
				claim.getBeneficiaryDischargeDate(), Optional.of(claim.getUtilizationDayCount()));
		
		// test common benefit components between SNF and Inpatient claims are set as expected
		TransformerTestUtils.assertCommonGroupInpatientSNF(eob, claim.getCoinsuranceDayCount(),
				claim.getNonUtilizationDayCount(), claim.getDeductibleAmount(),
				claim.getPartACoinsuranceLiabilityAmount(), claim.getBloodPintsFurnishedQty(),
				claim.getNoncoveredCharge(), claim.getTotalDeductionAmount(),
				claim.getClaimPPSCapitalDisproportionateShareAmt(), claim.getClaimPPSCapitalExceptionAmount(),
				claim.getClaimPPSCapitalFSPAmount(), claim.getClaimPPSCapitalIMEAmount(),
				claim.getClaimPPSCapitalOutlierAmount(), claim.getClaimPPSOldCapitalHoldHarmlessAmount());
		
		// test common eob information between SNF and Inpatient claims are set as expected
		TransformerTestUtils.assertCommonEobInformationInpatientSNF(eob, claim.getNoncoveredStayFromDate(),
				claim.getNoncoveredStayThroughDate(), claim.getCoveredCareThoughDate(),
				claim.getMedicareBenefitsExhaustedDate(), claim.getDiagnosisRelatedGroupCd());

		TransformerTestUtils.assertAdjudicationTotalAmountEquals(CcwCodebookVariable.NCH_DRG_OUTLIER_APRVD_PMT_AMT,
				claim.getDrgOutlierApprovedPaymentAmount(), eob);

		// Test to ensure common group fields between Inpatient, Outpatient and SNF
		// match
		TransformerTestUtils.assertEobCommonGroupInpOutSNFEquals(eob, claim.getBloodDeductibleLiabilityAmount(),
				claim.getOperatingPhysicianNpi(), claim.getOtherPhysicianNpi(), claim.getClaimQueryCode(),
				claim.getMcoPaidSw());

		// Test to ensure common group fields between Inpatient, Outpatient HHA, Hospice
		// and SNF match
		TransformerTestUtils.assertEobCommonGroupInpOutHHAHospiceSNFEquals(eob, claim.getOrganizationNpi(),
				claim.getClaimFacilityTypeCode(), claim.getClaimFrequencyCode(), claim.getClaimNonPaymentReasonCode(),
				claim.getPatientDischargeStatusCode(), claim.getClaimServiceClassificationTypeCode(),
				claim.getClaimPrimaryPayerCode(), claim.getAttendingPhysicianNpi(), claim.getTotalChargeAmount(),
				claim.getPrimaryPayerPaidAmount(), claim.getFiscalIntermediaryNumber());

		Assert.assertEquals(9, eob.getDiagnosis().size());

		CCWProcedure ccwProcedure = new CCWProcedure(claim.getProcedure1Code(), claim.getProcedure1CodeVersion(),
				claim.getProcedure1Date());
		TransformerTestUtils.assertHasCoding(ccwProcedure.getFhirSystem().toString(),
				claim.getProcedure1Code().get(),
				eob.getProcedure().get(0).getProcedureCodeableConcept().getCoding());
		Assert.assertEquals(
				Date.from(claim.getProcedure1Date().get().atStartOfDay(ZoneId.systemDefault()).toInstant()),
				eob.getProcedure().get(0).getDate());

		Assert.assertEquals(1, eob.getItem().size());
		ItemComponent eobItem0 = eob.getItem().get(0);
		InpatientClaimLine claimLine1 = claim.getLines().get(0);
		Assert.assertEquals(claimLine1.getLineNumber(), new BigDecimal(eobItem0.getSequence()));

		Assert.assertEquals(claim.getProviderStateCode(), eobItem0.getLocationAddress().getState());

		TransformerTestUtils.assertHcpcsCodes(eobItem0, claimLine1.getHcpcsCode(), Optional.empty(), Optional.empty(),
				Optional.empty(), 0/* index */);

		// Test to ensure common group field coinsurance between Inpatient, HHA, Hospice and SNF match
		TransformerTestUtils.assertEobCommonGroupInpHHAHospiceSNFCoinsuranceEquals(eobItem0, claimLine1.getDeductibleCoinsuranceCd());
		
		// Test to ensure item level fields between Inpatient, Outpatient, HHA, Hopsice
		// and SNF match
		TransformerTestUtils.assertEobCommonItemRevenueEquals(eobItem0, eob, claimLine1.getRevenueCenter(),
				claimLine1.getRateAmount(), claimLine1.getTotalChargeAmount(), claimLine1.getNonCoveredChargeAmount(),
				claimLine1.getUnitCount(), claimLine1.getNationalDrugCodeQuantity(),
				claimLine1.getNationalDrugCodeQualifierCode(), claimLine1.getRevenueCenterRenderingPhysicianNPI(),
				0/* index */);
		
		// verify {@link
		// TransformerUtils#mapEobType(CodeableConcept,ClaimType,Optional,Optional)}
		// method worked as expected for this claim type
		TransformerTestUtils.assertMapEobType(eob.getType(), ClaimType.INPATIENT,
				Optional.of(org.hl7.fhir.dstu3.model.codesystems.ClaimType.INSTITUTIONAL),
				Optional.of(claim.getNearLineRecordIdCode()), Optional.of(claim.getClaimTypeCode()));
	}
		
}
