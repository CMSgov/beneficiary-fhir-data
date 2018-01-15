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
		TransformerTestUtils.assertNoEncodedOptionals(eob);

		Assert.assertEquals(TransformerUtils.buildEobId(ClaimType.DME, claim.getClaimId()),
				eob.getIdElement().getIdPart());

		TransformerTestUtils.assertIdentifierExists(TransformerConstants.CODING_CCW_CLAIM_ID, claim.getClaimId(),
				eob.getIdentifier());
		TransformerTestUtils.assertIdentifierExists(TransformerConstants.CODING_CCW_CLAIM_GROUP_ID,
				claim.getClaimGroupId().toPlainString(), eob.getIdentifier());
		Assert.assertEquals(TransformerUtils.referencePatient(claim.getBeneficiaryId()).getReference(),
				eob.getPatient().getReference());
		TransformerTestUtils.assertExtensionCodingEquals(eob.getType(),
				TransformerConstants.CODING_CCW_RECORD_ID_CODE, TransformerConstants.CODING_CCW_RECORD_ID_CODE,
				"" + claim.getNearLineRecordIdCode());
		TransformerTestUtils.assertHasCoding(TransformerConstants.CODING_CCW_CLAIM_TYPE,
				claim.getClaimTypeCode(), eob.getType());
		Assert.assertEquals(
				TransformerUtils.referenceCoverage(claim.getBeneficiaryId(), MedicareSegment.PART_B).getReference(),
				eob.getInsurance().getCoverage().getReference());
		Assert.assertEquals("active", eob.getStatus().toCode());

		TransformerTestUtils.assertDateEquals(claim.getDateFrom(), eob.getBillablePeriod().getStartElement());
		TransformerTestUtils.assertDateEquals(claim.getDateThrough(), eob.getBillablePeriod().getEndElement());

		Assert.assertEquals(TransformerConstants.CODED_EOB_DISPOSITION, eob.getDisposition());

		// Test to ensure common group fields between Carrier and DME match
		TransformerTestUtils.assertEobCommonGroupCarrierDMEEquals(eob, claim.getBeneficiaryId(),
				claim.getCarrierNumber(),
				claim.getClinicalTrialNumber(), claim.getBeneficiaryPartBDeductAmount(), claim.getPaymentDenialCode(),
				claim.getReferringPhysicianNpi(), Optional.of(claim.getProviderAssignmentIndicator()),
				claim.getProviderPaymentAmount(), claim.getBeneficiaryPaymentAmount(), claim.getSubmittedChargeAmount(),
				claim.getAllowedChargeAmount());

		Assert.assertEquals(claim.getPaymentAmount(), eob.getPayment().getAmount().getValue());
		TransformerTestUtils.assertHasCoding(TransformerConstants.CODING_CCW_CLAIM_TYPE,
				claim.getClaimTypeCode(), eob.getType());
		Assert.assertEquals("active", eob.getStatus().toCode());

		TransformerTestUtils.assertBenefitBalanceEquals(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
				TransformerConstants.CODED_ADJUDICATION_PRIMARY_PAYER_PAID_AMOUNT, claim.getPrimaryPayerPaidAmount(),
				eob.getBenefitBalanceFirstRep().getFinancial());

		Assert.assertEquals(3, eob.getDiagnosis().size());
		Assert.assertEquals(1, eob.getItem().size());
		ItemComponent eobItem0 = eob.getItem().get(0);
		DMEClaimLine claimLine1 = claim.getLines().get(0);
		Assert.assertEquals(claimLine1.getLineNumber(), new BigDecimal(eobItem0.getSequence()));

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

		TransformerTestUtils.assertHasCoding(TransformerConstants.CODING_HCPCS,
				claimLine1.getHcpcsInitialModifierCode().get(),
				eobItem0.getModifier().get(0));
		Assert.assertFalse(claimLine1.getHcpcsSecondModifierCode().isPresent());

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

		TransformerTestUtils.assertExtensionCodingEquals(eobItem0, TransformerConstants.EXTENSION_CODING_MTUS,
				TransformerConstants.EXTENSION_CODING_MTUS, String.valueOf(claimLine1.getMtusCode().get()));

		TransformerTestUtils.assertExtensionCodingEquals(eobItem0, TransformerConstants.EXTENSION_MTUS_COUNT,
				TransformerConstants.EXTENSION_MTUS_COUNT, String.valueOf(claimLine1.getMtusCount()));

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
