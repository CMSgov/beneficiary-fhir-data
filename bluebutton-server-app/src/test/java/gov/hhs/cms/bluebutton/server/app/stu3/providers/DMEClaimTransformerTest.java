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

import gov.hhs.cms.bluebutton.data.model.rif.DMEClaim;
import gov.hhs.cms.bluebutton.data.model.rif.DMEClaimLine;
import gov.hhs.cms.bluebutton.data.model.rif.samples.StaticRifResource;
import gov.hhs.cms.bluebutton.data.model.rif.samples.StaticRifResourceGroup;
import gov.hhs.cms.bluebutton.server.app.ServerTestUtils;

/**
 * Unit tests for {@link DMEClaimTransformer}.
 */
public final class DMEClaimTransformerTest {
	/**
	 * Verifies that {@link DMEClaimTransformer#transform(Object)} works as
	 * expected when run against the {@link StaticRifResource#SAMPLE_A_DME}
	 * {@link DMEClaim}.
	 * 
	 * @throws FHIRException
	 *             (indicates test failure)
	 */
	@Test
	public void transformSampleARecord() throws FHIRException {
		List<Object> parsedRecords = ServerTestUtils
				.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
		DMEClaim claim = parsedRecords.stream().filter(r -> r instanceof DMEClaim).map(r -> (DMEClaim) r).findFirst()
				.get();

		ExplanationOfBenefit eob = DMEClaimTransformer.transform(claim);
		assertMatches(claim, eob);
	}

	/**
	 * Verifies that the {@link ExplanationOfBenefit} "looks like" it should, if
	 * it were produced from the specified {@link DMEClaim}.
	 * 
	 * @param claim
	 *            the {@link DMEClaim} that the {@link ExplanationOfBenefit} was
	 *            generated from
	 * @param eob
	 *            the {@link ExplanationOfBenefit} that was generated from the
	 *            specified {@link DMEClaim}
	 * @throws FHIRException
	 *             (indicates test failure)
	 */
	static void assertMatches(DMEClaim claim, ExplanationOfBenefit eob) throws FHIRException {
		// Test to ensure group level fields between all claim types match
				TransformerTestUtils.assertEobCommonClaimHeaderData(eob, claim.getClaimId(), claim.getBeneficiaryId(),
				ClaimType.DME, claim.getClaimGroupId().toPlainString(), MedicareSegment.PART_B,
						Optional.of(claim.getDateFrom()), Optional.of(claim.getDateThrough()),
				Optional.of(claim.getPaymentAmount()), claim.getFinalAction());

		// Test to ensure common group fields between Carrier and DME match
		TransformerTestUtils.assertEobCommonGroupCarrierDMEEquals(eob, claim.getCarrierNumber(),
				claim.getClinicalTrialNumber(), claim.getBeneficiaryPartBDeductAmount(), claim.getPaymentDenialCode(),
				Optional.of(claim.getProviderAssignmentIndicator()), claim.getProviderPaymentAmount(),
				claim.getBeneficiaryPaymentAmount(), claim.getSubmittedChargeAmount(),
				claim.getAllowedChargeAmount());

		TransformerTestUtils.assertBenefitBalanceEquals(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODED_ADJUDICATION_PRIMARY_PAYER_PAID_AMOUNT, claim.getPrimaryPayerPaidAmount(),
				eob.getBenefitBalanceFirstRep().getFinancial());

		Assert.assertEquals(3, eob.getDiagnosis().size());
		Assert.assertEquals(1, eob.getItem().size());
		ItemComponent eobItem0 = eob.getItem().get(0);
		DMEClaimLine claimLine1 = claim.getLines().get(0);
		Assert.assertEquals(claimLine1.getLineNumber(), new BigDecimal(eobItem0.getSequence()));

		TransformerTestUtils.assertExtensionIdentifierEqualsString(
				eobItem0.getExtensionsByUrl(TransformerConstants.EXTENSION_IDENTIFIER_DME_PROVIDER_BILLING_NUMBER),
				claimLine1.getProviderBillingNumber().get());

		TransformerTestUtils.assertCareTeamEquals(claimLine1.getProviderNPI().get(),
				ClaimCareteamrole.PRIMARY.toCode(), eob);
		CareTeamComponent performingCareTeamEntry = TransformerTestUtils.findCareTeamEntryForProviderIdentifier(
				claimLine1.getProviderNPI().get(), eob.getCareTeam());
		TransformerTestUtils.assertHasCoding(TransformerConstants.CODING_CCW_PROVIDER_SPECIALTY,
				claimLine1.getProviderSpecialityCode().get(), performingCareTeamEntry.getQualification());
		TransformerTestUtils.assertExtensionCodingEquals(performingCareTeamEntry,
				TransformerConstants.EXTENSION_CODING_CCW_PROVIDER_PARTICIPATING,
				TransformerConstants.EXTENSION_CODING_CCW_PROVIDER_PARTICIPATING,
				"" + claimLine1.getProviderParticipatingIndCode().get());

		TransformerTestUtils.assertExtensionCodingEquals(eobItem0,
				TransformerConstants.CODING_FHIR_ACT_INVOICE_GROUP,
				TransformerConstants.CODING_FHIR_ACT_INVOICE_GROUP,
				(TransformerConstants.CODED_ACT_INVOICE_GROUP_CLINICAL_SERVICES_AND_PRODUCTS));

		TransformerTestUtils.assertExtensionCodingEquals(eobItem0.getLocation(),
				TransformerConstants.EXTENSION_CODING_CCW_PROVIDER_STATE,
				TransformerConstants.EXTENSION_CODING_CCW_PROVIDER_STATE, claimLine1.getProviderStateCode());
		
		TransformerTestUtils.assertHcpcsCodes(eobItem0, claimLine1.getHcpcsCode(),
				claimLine1.getHcpcsInitialModifierCode(), claimLine1.getHcpcsSecondModifierCode(), claim.getHcpcsYearCode(),
				0/* index */);
		TransformerTestUtils.assertHasCoding(TransformerConstants.CODING_HCPCS, "" + claim.getHcpcsYearCode().get(),
				claimLine1.getHcpcsCode().get(), eobItem0.getService());

		TransformerTestUtils.assertAdjudicationEquals(
				TransformerConstants.CODED_ADJUDICATION_LINE_PRIMARY_PAYER_ALLOWED_CHARGE,
				claimLine1.getPrimaryPayerAllowedChargeAmount(), eobItem0.getAdjudication());

		TransformerTestUtils.assertAdjudicationEquals(
				TransformerConstants.CODED_ADJUDICATION_LINE_PURCHASE_PRICE_AMOUNT, claimLine1.getPurchasePriceAmount(),
				eobItem0.getAdjudication());

		TransformerTestUtils.assertExtensionCodingEquals(eobItem0.getLocation(),
				TransformerConstants.EXTENSION_CODING_CCW_PRICING_STATE_CD,
				TransformerConstants.EXTENSION_CODING_CCW_PRICING_STATE_CD, claimLine1.getPricingStateCode().get());

		TransformerTestUtils.assertExtensionCodingEquals(eobItem0.getLocation(),
				TransformerConstants.EXTENSION_CODING_CMS_SUPPLIER_TYPE,
				TransformerConstants.EXTENSION_CODING_CMS_SUPPLIER_TYPE,
				String.valueOf(claimLine1.getSupplierTypeCode().get()));

		TransformerTestUtils.assertExtensionCodingEquals(eobItem0,
				TransformerConstants.EXTENSION_SCREEN_SAVINGS, TransformerConstants.EXTENSION_SCREEN_SAVINGS,
				String.valueOf(claimLine1.getScreenSavingsAmount().get()));

		TransformerTestUtils.assertExtensionCodingEquals(eobItem0, TransformerConstants.EXTENSION_CODING_UNIT_IND,
				TransformerConstants.EXTENSION_CODING_UNIT_IND, String.valueOf(claimLine1.getMtusCode().get()));

		TransformerTestUtils.assertExtensionValueQuantityEquals(
				eobItem0.getExtensionsByUrl(TransformerConstants.EXTENSION_DME_UNIT),
				TransformerConstants.EXTENSION_DME_UNIT, TransformerConstants.EXTENSION_DME_UNIT,
				claimLine1.getMtusCount());

		TransformerTestUtils.assertExtensionCodingEquals(eobItem0, TransformerConstants.CODING_NDC,
				TransformerConstants.CODING_NDC, claimLine1.getNationalDrugCode().get());
		
		// verify {@link
		// TransformerUtils#mapEobType(CodeableConcept,ClaimType,Optional,Optional)}
		// method worked as expected for this claim type
		TransformerTestUtils.assertMapEobType(eob.getType(), ClaimType.DME,
				// FUTURE there currently is not an equivalent CODING_FHIR_CLAIM_TYPE mapping
				// for this claim type. If added then the Optional empty parameter below should
				// be updated to match expected result.
				Optional.empty(), Optional.of(claim.getNearLineRecordIdCode()), Optional.of(claim.getClaimTypeCode()));
		
		// Test to ensure common item fields between Carrier and DME match
		TransformerTestUtils.assertEobCommonItemCarrierDMEEquals(eobItem0, eob, claim.getBeneficiaryId(),
				claimLine1.getServiceCount(), claimLine1.getPlaceOfServiceCode(),
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
				claimLine1.getCmsServiceTypeCode(), claimLine1.getNationalDrugCode(),
				Optional.of(claim.getReferringPhysicianNpi().get()));
	}
}
