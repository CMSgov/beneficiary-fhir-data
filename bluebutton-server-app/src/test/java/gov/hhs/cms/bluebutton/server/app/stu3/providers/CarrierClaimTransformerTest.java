package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.CareTeamComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.dstu3.model.codesystems.ClaimCareteamrole;
import org.hl7.fhir.exceptions.FHIRException;
import org.junit.Assert;
import org.junit.Test;

import gov.hhs.cms.bluebutton.data.model.rif.CarrierClaim;
import gov.hhs.cms.bluebutton.data.model.rif.CarrierClaimLine;
import gov.hhs.cms.bluebutton.data.model.rif.samples.StaticRifResource;
import gov.hhs.cms.bluebutton.data.model.rif.samples.StaticRifResourceGroup;
import gov.hhs.cms.bluebutton.server.app.ServerTestUtils;

/**
 * Unit tests for {@link CarrierClaimTransformer}.
 */
public final class CarrierClaimTransformerTest {
	/**
	 * Verifies that {@link CarrierClaimTransformer#transform(Object)} works as
	 * expected when run against the {@link StaticRifResource#SAMPLE_A_CARRIER}
	 * {@link CarrierClaim}.
	 * 
	 * @throws FHIRException
	 *             (indicates test failure)
	 */
	@Test
	public void transformSampleARecord() throws FHIRException {
		List<Object> parsedRecords = ServerTestUtils
				.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
		CarrierClaim claim = parsedRecords.stream().filter(r -> r instanceof CarrierClaim).map(r -> (CarrierClaim) r)
				.findFirst().get();

		ExplanationOfBenefit eob = CarrierClaimTransformer.transform(claim);
		assertMatches(claim, eob);
	}

	/**
	 * Verifies that {@link CarrierClaimTransformer#transform(Object)} works as
	 * expected when run against the {@link StaticRifResource#SAMPLE_U_CARRIER}
	 * {@link CarrierClaim}.
	 *
	 * @throws FHIRException
	 *             (indicates test failure)
	 */
	@Test
	public void transformSampleURecord() throws FHIRException {
		List<Object> parsedRecords = ServerTestUtils
				.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_U.getResources()));
		CarrierClaim claim = parsedRecords.stream().filter(r -> r instanceof CarrierClaim).map(r -> (CarrierClaim) r)
				.findFirst().get();

		ExplanationOfBenefit eob = CarrierClaimTransformer.transform(claim);
		assertMatches(claim, eob);
	}
	
	/**
	 * Verifies that the {@link ExplanationOfBenefit} "looks like" it should, if
	 * it were produced from the specified {@link CarrierClaim}.
	 * 
	 * @param claim
	 *            the {@link CarrierClaim} that the {@link ExplanationOfBenefit}
	 *            was generated from
	 * @param eob
	 *            the {@link ExplanationOfBenefit} that was generated from the
	 *            specified {@link CarrierClaim}
	 * @throws FHIRException
	 *             (indicates test failure)
	 */
	static void assertMatches(CarrierClaim claim, ExplanationOfBenefit eob) throws FHIRException {
		// Test to ensure group level fields between all claim types match
		TransformerTestUtils.assertEobCommonClaimHeaderData(eob, claim.getClaimId(), claim.getBeneficiaryId(),
				ClaimType.CARRIER, claim.getClaimGroupId().toPlainString(), MedicareSegment.PART_B,
				Optional.of(claim.getDateFrom()), Optional.of(claim.getDateThrough()),
				Optional.of(claim.getPaymentAmount()), claim.getFinalAction());

		// Test to ensure common group fields between Carrier and DME match
		TransformerTestUtils.assertEobCommonGroupCarrierDMEEquals(eob, claim.getBeneficiaryId(),
				claim.getCarrierNumber(),
				claim.getClinicalTrialNumber(), claim.getBeneficiaryPartBDeductAmount(), claim.getPaymentDenialCode(),
				claim.getReferringPhysicianNpi(), claim.getProviderAssignmentIndicator(),
				claim.getProviderPaymentAmount(), claim.getBeneficiaryPaymentAmount(), claim.getSubmittedChargeAmount(),
				claim.getAllowedChargeAmount());

		Assert.assertEquals(6, eob.getDiagnosis().size());
		Assert.assertEquals(1, eob.getItem().size());


		CarrierClaimLine claimLine1 = claim.getLines().get(0);
		ItemComponent eobItem0 = eob.getItem().get(0);
		Assert.assertEquals(claimLine1.getLineNumber(), new BigDecimal(eobItem0.getSequence()));

		TransformerTestUtils.assertExtensionCodingEquals(eobItem0,
				TransformerConstants.CODING_FHIR_ACT_INVOICE_GROUP,
				TransformerConstants.CODING_FHIR_ACT_INVOICE_GROUP,
				(TransformerConstants.CODED_ACT_INVOICE_GROUP_CLINICAL_SERVICES_AND_PRODUCTS));

		TransformerTestUtils.assertCareTeamEquals(claimLine1.getPerformingPhysicianNpi().get(),
				ClaimCareteamrole.PRIMARY.toCode(), eob);
		CareTeamComponent performingCareTeamEntry = TransformerTestUtils.findCareTeamEntryForProviderIdentifier(
				claimLine1.getPerformingPhysicianNpi().get(), eob.getCareTeam());
		TransformerTestUtils.assertHasCoding(TransformerConstants.CODING_CCW_PROVIDER_SPECIALTY,
				claimLine1.getProviderSpecialityCode().get(), performingCareTeamEntry.getQualification());
		TransformerTestUtils.assertExtensionCodingEquals(performingCareTeamEntry,
				TransformerConstants.EXTENSION_CODING_CCW_PROVIDER_TYPE,
				TransformerConstants.EXTENSION_CODING_CCW_PROVIDER_TYPE, "" + claimLine1.getProviderTypeCode());
		TransformerTestUtils.assertExtensionCodingEquals(performingCareTeamEntry,
				TransformerConstants.EXTENSION_CODING_CCW_PROVIDER_PARTICIPATING,
				TransformerConstants.EXTENSION_CODING_CCW_PROVIDER_PARTICIPATING,
				"" + claimLine1.getProviderParticipatingIndCode().get());
		TransformerTestUtils.assertExtensionCodingEquals(performingCareTeamEntry,
				TransformerConstants.CODING_NPI_US, TransformerConstants.CODING_NPI_US,
				"" + claimLine1.getOrganizationNpi().get());

		TransformerTestUtils.assertExtensionCodingEquals(eobItem0.getLocation(),
				TransformerConstants.EXTENSION_CODING_CCW_PROVIDER_STATE,
				TransformerConstants.EXTENSION_CODING_CCW_PROVIDER_STATE, claimLine1.getProviderStateCode().get());
		TransformerTestUtils.assertExtensionCodingEquals(eobItem0.getLocation(),
				TransformerConstants.EXTENSION_CODING_CCW_PROVIDER_ZIP,
				TransformerConstants.EXTENSION_CODING_CCW_PROVIDER_ZIP, claimLine1.getProviderZipCode().get());

		TransformerTestUtils.assertExtensionCodingEquals(eobItem0.getLocation(),
				TransformerConstants.EXTENSION_CODING_CCW_PRICING_LOCALITY,
				TransformerConstants.EXTENSION_CODING_CCW_PRICING_LOCALITY, "15");

		TransformerTestUtils.assertHasCoding(TransformerConstants.CODING_HCPCS,
				"" + claim.getHcpcsYearCode().get(), claimLine1.getHcpcsCode().get(), eobItem0.getService());
		Assert.assertEquals(1, eobItem0.getModifier().size());
		TransformerTestUtils.assertHcpcsCodes(eobItem0, claimLine1.getHcpcsCode(),
				claimLine1.getHcpcsInitialModifierCode(), claimLine1.getHcpcsSecondModifierCode(), claim.getHcpcsYearCode(),
				0/* index */);

		TransformerTestUtils.assertExtensionCodingEquals(eobItem0, TransformerConstants.EXTENSION_CODING_UNIT_IND,
				TransformerConstants.EXTENSION_CODING_UNIT_IND, String.valueOf(claimLine1.getMtusCode().get()));

		TransformerTestUtils.assertExtensionCodingEquals(eobItem0, TransformerConstants.EXTENSION_DME_UNIT,
				TransformerConstants.EXTENSION_DME_UNIT, String.valueOf(claimLine1.getMtusCount()));

		TransformerTestUtils.assertAdjudicationReasonEquals(TransformerConstants.CODED_ADJUDICATION_PHYSICIAN_ASSISTANT,
				TransformerConstants.CODING_CCW_PHYSICIAN_ASSISTANT_ADJUDICATION,
				"" + claimLine1.getReducedPaymentPhysicianAsstCode(), eobItem0.getAdjudication());

		TransformerTestUtils.assertExtensionCodingEquals(eobItem0.getLocation(),
				TransformerConstants.EXTENSION_IDENTIFIER_CCW_CLIA_LAB_NUM, TransformerConstants.EXTENSION_IDENTIFIER_CCW_CLIA_LAB_NUM,
				claimLine1.getCliaLabNumber().get());

		// verify {@link
		// TransformerUtils#mapEobType(CodeableConcept,ClaimType,Optional,Optional)}
		// method worked as expected for this claim type
		TransformerTestUtils.assertMapEobType(eob.getType(), ClaimType.CARRIER,
				Optional.of(org.hl7.fhir.dstu3.model.codesystems.ClaimType.PROFESSIONAL),
				Optional.of(claim.getNearLineRecordIdCode()), Optional.of(claim.getClaimTypeCode()));

    // Test to ensure common item fields between Carrier and DME match
		TransformerTestUtils.assertEobCommonItemCarrierDMEEquals(eobItem0, eob, claimLine1.getServiceCount(),
				claimLine1.getPlaceOfServiceCode(),
				claimLine1.getFirstExpenseDate(),
				claimLine1.getLastExpenseDate(), claimLine1.getBeneficiaryPaymentAmount(),
				claimLine1.getProviderPaymentAmount(), claimLine1.getBeneficiaryPartBDeductAmount(),
				claimLine1.getPrimaryPayerCode(), claimLine1.getPrimaryPayerPaidAmount(), claimLine1.getBetosCode(),
				claimLine1.getPaymentAmount(), claimLine1.getPaymentCode(), claimLine1.getCoinsuranceAmount(),
				claimLine1.getSubmittedChargeAmount(), claimLine1.getAllowedChargeAmount(),
				claimLine1.getProcessingIndicatorCode(), claimLine1.getServiceDeductibleCode(),
				claimLine1.getDiagnosisCode(),
				claimLine1.getDiagnosisCodeVersion(), 
				claimLine1.getHctHgbTestTypeCode(), claimLine1.getHctHgbTestResult(),
				claimLine1.getCmsServiceTypeCode(), claimLine1.getNationalDrugCode());
	}
}
