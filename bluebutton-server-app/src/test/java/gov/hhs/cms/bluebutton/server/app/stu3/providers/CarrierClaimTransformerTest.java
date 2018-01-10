package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.AdjudicationComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.CareTeamComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ReferralRequest;
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
		TransformerTestUtils.assertNoEncodedOptionals(eob);

		Assert.assertEquals(TransformerUtils.buildEobId(ClaimType.CARRIER, claim.getClaimId()),
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
		Assert.assertEquals("active", eob.getStatus().toCode());
		TransformerTestUtils.assertDateEquals(claim.getDateFrom(), eob.getBillablePeriod().getStartElement());
		TransformerTestUtils.assertDateEquals(claim.getDateThrough(), eob.getBillablePeriod().getEndElement());
		Assert.assertEquals(TransformerConstants.CODED_EOB_DISPOSITION, eob.getDisposition());

		// Test to ensure common group fields between Carrier and DME match
		TransformerTestUtils.assertEobCommonGroupCarrierDMEEquals(eob, claim.getCarrierNumber(),
				claim.getClinicalTrialNumber());

		TransformerTestUtils.assertExtensionCodingEquals(eob,
				TransformerConstants.EXTENSION_CODING_CCW_CARR_PAYMENT_DENIAL,
				TransformerConstants.EXTENSION_CODING_CCW_CARR_PAYMENT_DENIAL, claim.getPaymentDenialCode());
		Assert.assertEquals(claim.getPaymentAmount(), eob.getPayment().getAmount().getValue());

		Assert.assertEquals(
				TransformerUtils.referenceCoverage(claim.getBeneficiaryId(), MedicareSegment.PART_B).getReference(),
				eob.getInsurance().getCoverage().getReference());

		ReferralRequest referral = (ReferralRequest) eob.getReferral().getResource();
		Assert.assertEquals(TransformerUtils.referencePatient(claim.getBeneficiaryId()).getReference(),
				referral.getSubject().getReference());
		TransformerTestUtils.assertReferenceIdentifierEquals(TransformerConstants.CODING_NPI_US,
				claim.getReferringPhysicianNpi().get(), referral.getRequester().getAgent());
		Assert.assertEquals(1, referral.getRecipient().size());
		TransformerTestUtils.assertReferenceIdentifierEquals(TransformerConstants.CODING_NPI_US,
				claim.getReferringPhysicianNpi().get(), referral.getRecipientFirstRep());

		TransformerTestUtils.assertExtensionCodingEquals(eob,
				TransformerConstants.CODING_CCW_PROVIDER_ASSIGNMENT,
				TransformerConstants.CODING_CCW_PROVIDER_ASSIGNMENT, "A");

		TransformerTestUtils.assertBenefitBalanceEquals(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODED_ADJUDICATION_PROVIDER_PAYMENT_AMOUNT, claim.getProviderPaymentAmount(),
				eob.getBenefitBalanceFirstRep().getFinancial());
		TransformerTestUtils.assertBenefitBalanceEquals(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODED_ADJUDICATION_SUBMITTED_CHARGE_AMOUNT, claim.getSubmittedChargeAmount(),
				eob.getBenefitBalanceFirstRep().getFinancial());
		TransformerTestUtils.assertBenefitBalanceEquals(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODED_ADJUDICATION_ALLOWED_CHARGE, claim.getAllowedChargeAmount(),
				eob.getBenefitBalanceFirstRep().getFinancial());

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

		Assert.assertEquals(claimLine1.getServiceCount(), eobItem0.getQuantity().getValue());

		TransformerTestUtils.assertHasCoding(TransformerConstants.CODING_CCW_TYPE_SERVICE,
				"" + claimLine1.getCmsServiceTypeCode(), eobItem0.getCategory());

		TransformerTestUtils.assertHasCoding(TransformerConstants.CODING_CCW_PLACE_OF_SERVICE,
				claimLine1.getPlaceOfServiceCode(), eobItem0.getLocationCodeableConcept());
		TransformerTestUtils.assertExtensionCodingEquals(eobItem0.getLocation(),
				TransformerConstants.EXTENSION_CODING_CCW_PRICING_LOCALITY,
				TransformerConstants.EXTENSION_CODING_CCW_PRICING_LOCALITY, "15");

		TransformerTestUtils.assertHasCoding(TransformerConstants.CODING_HCPCS,
				"" + claim.getHcpcsYearCode().get(), claimLine1.getHcpcsCode().get(), eobItem0.getService());
		Assert.assertEquals(1, eobItem0.getModifier().size());
		TransformerTestUtils.assertHasCoding(TransformerConstants.CODING_HCPCS,
				"" + claim.getHcpcsYearCode().get(), claimLine1.getHcpcsInitialModifierCode().get(),
				eobItem0.getModifier().get(0));

		TransformerTestUtils.assertExtensionCodingEquals(eobItem0, TransformerConstants.CODING_BETOS,
				TransformerConstants.CODING_BETOS, claimLine1.getBetosCode().get());

		TransformerTestUtils.assertExtensionCodingEquals(eobItem0,
				TransformerConstants.EXTENSION_CODING_CCW_LINE_DEDUCTIBLE_SWITCH,
				TransformerConstants.EXTENSION_CODING_CCW_LINE_DEDUCTIBLE_SWITCH,
				"" + claimLine1.getServiceDeductibleCode().get());

		TransformerTestUtils.assertAdjudicationEquals(TransformerConstants.CODED_ADJUDICATION_PAYMENT,
				claimLine1.getPaymentAmount(), eobItem0.getAdjudication());
		AdjudicationComponent adjudicationForPayment = eobItem0.getAdjudication().stream()
				.filter(a -> TransformerTestUtils.isCodeInConcept(a.getCategory(),
						TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
						TransformerConstants.CODED_ADJUDICATION_PAYMENT))
				.findAny().get();
		TransformerTestUtils.assertExtensionCodingEquals(adjudicationForPayment,
				TransformerConstants.EXTENSION_CODING_CCW_PAYMENT_80_100_INDICATOR,
				TransformerConstants.EXTENSION_CODING_CCW_PAYMENT_80_100_INDICATOR,
				"" + claimLine1.getPaymentCode().get());
		TransformerTestUtils.assertAdjudicationEquals(
				TransformerConstants.CODED_ADJUDICATION_BENEFICIARY_PAYMENT_AMOUNT,
				claimLine1.getBeneficiaryPaymentAmount(), eobItem0.getAdjudication());
		TransformerTestUtils.assertAdjudicationEquals(TransformerConstants.CODED_ADJUDICATION_PROVIDER_PAYMENT_AMOUNT,
				claimLine1.getProviderPaymentAmount(), eobItem0.getAdjudication());
		TransformerTestUtils.assertAdjudicationEquals(TransformerConstants.CODED_ADJUDICATION_DEDUCTIBLE,
				claimLine1.getBeneficiaryPartBDeductAmount(), eobItem0.getAdjudication());
		TransformerTestUtils.assertAdjudicationEquals(TransformerConstants.CODED_ADJUDICATION_PRIMARY_PAYER_PAID_AMOUNT,
				claimLine1.getPrimaryPayerPaidAmount(), eobItem0.getAdjudication());
		TransformerTestUtils.assertAdjudicationEquals(TransformerConstants.CODED_ADJUDICATION_LINE_COINSURANCE_AMOUNT,
				claimLine1.getCoinsuranceAmount(), eobItem0.getAdjudication());
		TransformerTestUtils.assertAdjudicationEquals(TransformerConstants.CODED_ADJUDICATION_SUBMITTED_CHARGE_AMOUNT,
				claimLine1.getSubmittedChargeAmount(), eobItem0.getAdjudication());
		TransformerTestUtils.assertAdjudicationEquals(TransformerConstants.CODED_ADJUDICATION_ALLOWED_CHARGE,
				claimLine1.getAllowedChargeAmount(), eobItem0.getAdjudication());

		TransformerTestUtils.assertExtensionCodingEquals(eobItem0, TransformerConstants.EXTENSION_CODING_MTUS,
				TransformerConstants.EXTENSION_CODING_MTUS, String.valueOf(claimLine1.getMtusCode().get()));

		TransformerTestUtils.assertExtensionCodingEquals(eobItem0, TransformerConstants.EXTENSION_MTUS_COUNT,
				TransformerConstants.EXTENSION_MTUS_COUNT, String.valueOf(claimLine1.getMtusCount()));

		TransformerTestUtils.assertAdjudicationReasonEquals(TransformerConstants.CODED_ADJUDICATION_PHYSICIAN_ASSISTANT,
				TransformerConstants.CODING_CCW_PHYSICIAN_ASSISTANT_ADJUDICATION,
				"" + claimLine1.getReducedPaymentPhysicianAsstCode(), eobItem0.getAdjudication());
		TransformerTestUtils.assertAdjudicationReasonEquals(
				TransformerConstants.CODED_ADJUDICATION_LINE_PROCESSING_INDICATOR,
				TransformerConstants.CODING_CCW_PROCESSING_INDICATOR,
				claimLine1.getProcessingIndicatorCode().get(), eobItem0.getAdjudication());

		TransformerTestUtils.assertDiagnosisLinkPresent(
				Diagnosis.from(claimLine1.getDiagnosisCode(), claimLine1.getDiagnosisCodeVersion()), eob, eobItem0);

		TransformerTestUtils.assertExtensionCodingEquals(eobItem0, TransformerConstants.CODING_NDC,
				TransformerConstants.CODING_NDC, claimLine1.getNationalDrugCode().get());

		List<Extension> hctHgbObservationExtension = eobItem0
				.getExtensionsByUrl(TransformerConstants.EXTENSION_CMS_HCT_OR_HGB_RESULTS);
		Assert.assertEquals(1, hctHgbObservationExtension.size());
		Assert.assertTrue(hctHgbObservationExtension.get(0).getValue() instanceof Reference);
		Reference hctHgbReference = (Reference) hctHgbObservationExtension.get(0).getValue();
		Assert.assertTrue(hctHgbReference.getResource() instanceof Observation);
		Observation hctHgbObservation = (Observation) hctHgbReference.getResource();
		TransformerTestUtils.assertCodingEquals(TransformerConstants.CODING_CCW_HCT_OR_HGB_TEST_TYPE,
				claimLine1.getHctHgbTestTypeCode().get(), hctHgbObservation.getCode().getCodingFirstRep());
		Assert.assertEquals(claimLine1.getHctHgbTestResult(), hctHgbObservation.getValueQuantity().getValue());

		TransformerTestUtils.assertExtensionCodingEquals(eobItem0.getLocation(),
				TransformerConstants.EXTENSION_IDENTIFIER_CCW_CLIA_LAB_NUM, TransformerConstants.EXTENSION_IDENTIFIER_CCW_CLIA_LAB_NUM,
				claimLine1.getCliaLabNumber().get());

		// Test to ensure common item fields between Carrier and DME match
		TransformerUtils.mapEobCommonItemCarrierDME(eobItem0, claimLine1.getFirstExpenseDate(),
				claimLine1.getLastExpenseDate());
	}
}
