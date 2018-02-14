package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.math.BigDecimal;
import java.util.Optional;

import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.BenefitBalanceComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.BenefitComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.dstu3.model.Money;
import org.hl7.fhir.dstu3.model.codesystems.BenefitCategory;
import org.hl7.fhir.dstu3.model.codesystems.ClaimCareteamrole;

import com.justdavis.karl.misc.exceptions.BadCodeMonkeyException;

import gov.hhs.cms.bluebutton.data.model.rif.DMEClaim;
import gov.hhs.cms.bluebutton.data.model.rif.DMEClaimLine;

/**
 * Transforms CCW {@link DMEClaim} instances into FHIR
 * {@link ExplanationOfBenefit} resources.
 */
final class DMEClaimTransformer {
	/**
	 * @param claim
	 *            the CCW {@link DMEClaim} to transform
	 * @return a FHIR {@link ExplanationOfBenefit} resource that represents the
	 *         specified {@link DMEClaim}
	 */
	static ExplanationOfBenefit transform(Object claim) {
		if (!(claim instanceof DMEClaim))
			throw new BadCodeMonkeyException();
		return transformClaim((DMEClaim) claim);
	}

	/**
	 * @param claimGroup
	 *            the CCW {@link DMEClaim} to transform
	 * @return a FHIR {@link ExplanationOfBenefit} resource that represents the
	 *         specified {@link DMEClaim}
	 */
	private static ExplanationOfBenefit transformClaim(DMEClaim claimGroup) {
		ExplanationOfBenefit eob = new ExplanationOfBenefit();

		// Common group level fields between all claim types
		TransformerUtils.mapEobCommonClaimHeaderData(eob, claimGroup.getClaimId(), claimGroup.getBeneficiaryId(),
				ClaimType.DME, claimGroup.getClaimGroupId().toPlainString(), MedicareSegment.PART_B,
				Optional.of(claimGroup.getDateFrom()), Optional.of(claimGroup.getDateThrough()),
				Optional.of(claimGroup.getPaymentAmount()), claimGroup.getFinalAction());

		// map eob type codes into FHIR
		TransformerUtils.mapEobType(eob, ClaimType.DME, Optional.of(claimGroup.getNearLineRecordIdCode()), 
				Optional.of(claimGroup.getClaimTypeCode()));

		BenefitBalanceComponent benefitBalances = new BenefitBalanceComponent(
				TransformerUtils.createCodeableConcept(TransformerConstants.CODING_FHIR_BENEFIT_BALANCE,
						BenefitCategory.MEDICAL.toCode()));
		eob.getBenefitBalance().add(benefitBalances);

		if (claimGroup.getPrimaryPayerPaidAmount() != null) {
			BenefitComponent primaryPayerPaidAmount = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODED_ADJUDICATION_PRIMARY_PAYER_PAID_AMOUNT));
			primaryPayerPaidAmount.setAllowed(
					new Money().setSystem(TransformerConstants.CODED_MONEY_USD)
							.setValue(claimGroup.getPrimaryPayerPaidAmount()));
			benefitBalances.getFinancial().add(primaryPayerPaidAmount);
		}

		// Common group level fields between Carrier and DME
		TransformerUtils.mapEobCommonGroupCarrierDME(eob, claimGroup.getCarrierNumber(),
				claimGroup.getClinicalTrialNumber(), claimGroup.getBeneficiaryPartBDeductAmount(),
				claimGroup.getPaymentDenialCode(), Optional.of(claimGroup.getProviderAssignmentIndicator()),
				claimGroup.getProviderPaymentAmount(), claimGroup.getBeneficiaryPaymentAmount(),
				claimGroup.getSubmittedChargeAmount(), claimGroup.getAllowedChargeAmount());

		for (Diagnosis diagnosis : TransformerUtils.extractDiagnoses1Thru12(claimGroup.getDiagnosisPrincipalCode(),
				claimGroup.getDiagnosisPrincipalCodeVersion(), claimGroup.getDiagnosis1Code(),
				claimGroup.getDiagnosis1CodeVersion(), claimGroup.getDiagnosis2Code(),
				claimGroup.getDiagnosis2CodeVersion(), claimGroup.getDiagnosis3Code(),
				claimGroup.getDiagnosis3CodeVersion(), claimGroup.getDiagnosis4Code(),
				claimGroup.getDiagnosis4CodeVersion(), claimGroup.getDiagnosis5Code(),
				claimGroup.getDiagnosis5CodeVersion(), claimGroup.getDiagnosis6Code(),
				claimGroup.getDiagnosis6CodeVersion(), claimGroup.getDiagnosis7Code(),
				claimGroup.getDiagnosis7CodeVersion(), claimGroup.getDiagnosis8Code(),
				claimGroup.getDiagnosis8CodeVersion(), claimGroup.getDiagnosis9Code(),
				claimGroup.getDiagnosis9CodeVersion(), claimGroup.getDiagnosis10Code(),
				claimGroup.getDiagnosis10CodeVersion(), claimGroup.getDiagnosis11Code(),
				claimGroup.getDiagnosis11CodeVersion(), claimGroup.getDiagnosis12Code(),
				claimGroup.getDiagnosis12CodeVersion()))
			TransformerUtils.addDiagnosisCode(eob, diagnosis);

		for (DMEClaimLine claimLine : claimGroup.getLines()) {
			ItemComponent item = eob.addItem();
			item.setSequence(claimLine.getLineNumber().intValue());

			/*
			 * add an extension for the provider billing number as there is not a good place
			 * to map this in the existing FHIR specification
			 */
			if (claimLine.getProviderBillingNumber().isPresent()) {
				TransformerUtils.addExtensionValueIdentifier(item,
						TransformerConstants.EXTENSION_IDENTIFIER_DME_PROVIDER_BILLING_NUMBER,
						TransformerConstants.EXTENSION_IDENTIFIER_DME_PROVIDER_BILLING_NUMBER,
						claimLine.getProviderBillingNumber().get());
			}

			/*
			 * Per Michelle at GDIT, and also Tony Dean at OEDA, the performing provider
			 * _should_ always be present. However, we've found some examples in production
			 * where it's not for some claim lines. (This is annoying, as it's present on
			 * other lines in the same claim, and the data indicates that the same NPI
			 * probably applies to the lines where it's not specified. Still, it's not safe
			 * to guess at this, so we'll leave it blank.)
			 */
			if (claimLine.getProviderNPI().isPresent()) {
				ExplanationOfBenefit.CareTeamComponent performingCareTeamMember = TransformerUtils
						.addCareTeamPractitioner(eob, item, TransformerConstants.CODING_NPI_US,
								claimLine.getProviderNPI().get(), ClaimCareteamrole.PRIMARY.toCode());
				performingCareTeamMember.setResponsible(true);

				/*
				 * The provider's "specialty" and "type" code are equivalent.
				 * However, the "specialty" codes are more granular, and seem to
				 * better match the example FHIR
				 * `http://hl7.org/fhir/ex-providerqualification` code set.
				 * Accordingly, we map the "specialty" codes to the
				 * `qualification` field here, and stick the "type" code into an
				 * extension. TODO: suggest that the spec allows more than one
				 * `qualification` entry.
				 */
				performingCareTeamMember.setQualification(TransformerUtils.createCodeableConcept(
						TransformerConstants.CODING_CCW_PROVIDER_SPECIALTY,
						"" + claimLine.getProviderSpecialityCode().get()));

				TransformerUtils.addExtensionCoding(performingCareTeamMember,
						TransformerConstants.EXTENSION_CODING_CCW_PROVIDER_PARTICIPATING,
						TransformerConstants.EXTENSION_CODING_CCW_PROVIDER_PARTICIPATING,
						"" + claimLine.getProviderParticipatingIndCode().get());
			}

			TransformerUtils.addExtensionCoding(item, TransformerConstants.CODING_FHIR_ACT_INVOICE_GROUP,
					TransformerConstants.CODING_FHIR_ACT_INVOICE_GROUP,
					TransformerConstants.CODED_ACT_INVOICE_GROUP_CLINICAL_SERVICES_AND_PRODUCTS);

			// set hcpcs modifier codes for the claim
			TransformerUtils.setHcpcsModifierCodes(item, claimLine.getHcpcsCode(),
					claimLine.getHcpcsInitialModifierCode(), claimLine.getHcpcsSecondModifierCode(), claimGroup.getHcpcsYearCode());

			item.addAdjudication()
					.setCategory(
							TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
									TransformerConstants.CODED_ADJUDICATION_LINE_PRIMARY_PAYER_ALLOWED_CHARGE))
					.getAmount().setSystem(TransformerConstants.CODING_MONEY)
					.setCode(TransformerConstants.CODED_MONEY_USD)
					.setValue(claimLine.getPrimaryPayerAllowedChargeAmount());

			item.addAdjudication()
					.setCategory(
							TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
									TransformerConstants.CODED_ADJUDICATION_LINE_PURCHASE_PRICE_AMOUNT))
					.getAmount().setSystem(TransformerConstants.CODING_MONEY)
					.setCode(TransformerConstants.CODED_MONEY_USD)
					.setValue(claimLine.getPurchasePriceAmount());

			if (claimLine.getHcpcsThirdModifierCode().isPresent()) {
				item.addModifier(
						TransformerUtils.createCodeableConcept(TransformerConstants.CODING_HCPCS,
								"" + claimGroup.getHcpcsYearCode().get(), claimLine.getHcpcsThirdModifierCode().get()));
			}
			if (claimLine.getHcpcsFourthModifierCode().isPresent()) {
				item.addModifier(TransformerUtils.createCodeableConcept(
						TransformerConstants.CODING_HCPCS, "" + claimGroup.getHcpcsYearCode().get(),
						claimLine.getHcpcsFourthModifierCode().get()));
			}

			if (claimLine.getScreenSavingsAmount().isPresent()) {
				TransformerUtils.addExtensionCoding(item, TransformerConstants.EXTENSION_SCREEN_SAVINGS,
						TransformerConstants.EXTENSION_SCREEN_SAVINGS,
						String.valueOf(claimLine.getScreenSavingsAmount().get()));
			}

			if (claimLine.getMtusCode().isPresent()) {
				TransformerUtils.addExtensionCoding(item, TransformerConstants.EXTENSION_CODING_UNIT_IND,
						TransformerConstants.EXTENSION_CODING_UNIT_IND,
						String.valueOf(claimLine.getMtusCode().get()));
			}

			if (!claimLine.getMtusCount().equals(BigDecimal.ZERO)) {
				TransformerUtils.addExtensionValueQuantity(item, TransformerConstants.EXTENSION_DME_UNIT,
						TransformerConstants.EXTENSION_DME_UNIT,
						claimLine.getMtusCount());
			}

			// Common item level fields between Carrier and DME
			TransformerUtils.mapEobCommonItemCarrierDME(item, eob, claimGroup.getClaimId(),
					claimLine.getServiceCount(), claimLine.getPlaceOfServiceCode(),
					claimLine.getFirstExpenseDate(),
					claimLine.getLastExpenseDate(), claimLine.getBeneficiaryPaymentAmount(),
					claimLine.getProviderPaymentAmount(), claimLine.getBeneficiaryPartBDeductAmount(),
					claimLine.getPrimaryPayerCode(), claimLine.getPrimaryPayerPaidAmount(), claimLine.getBetosCode(),
					claimLine.getPaymentAmount(), claimLine.getPaymentCode(), claimLine.getCoinsuranceAmount(),
					claimLine.getSubmittedChargeAmount(), claimLine.getAllowedChargeAmount(),
					claimLine.getProcessingIndicatorCode(), claimLine.getServiceDeductibleCode(),
					claimLine.getDiagnosisCode(), claimLine.getDiagnosisCodeVersion(),
					claimLine.getHctHgbTestTypeCode(), claimLine.getHctHgbTestResult(),
					claimLine.getCmsServiceTypeCode(), claimLine.getNationalDrugCode(), claimGroup.getBeneficiaryId(),
					claimGroup.getReferringPhysicianNpi());

			if (!claimLine.getProviderStateCode().isEmpty()) {
				TransformerUtils.addExtensionCoding(item.getLocation(),
						TransformerConstants.EXTENSION_CODING_CCW_PROVIDER_STATE,
						TransformerConstants.EXTENSION_CODING_CCW_PROVIDER_STATE, claimLine.getProviderStateCode());
			}
			if (claimLine.getPricingStateCode().isPresent()) {
				TransformerUtils.addExtensionCoding(item.getLocation(),
						TransformerConstants.EXTENSION_CODING_CCW_PRICING_STATE_CD,
						TransformerConstants.EXTENSION_CODING_CCW_PRICING_STATE_CD,
						claimLine.getPricingStateCode().get());
			}

			if (claimLine.getSupplierTypeCode().isPresent()) {
				TransformerUtils.addExtensionCoding(item.getLocation(),
						TransformerConstants.EXTENSION_CODING_CMS_SUPPLIER_TYPE,
						TransformerConstants.EXTENSION_CODING_CMS_SUPPLIER_TYPE,
						String.valueOf(claimLine.getSupplierTypeCode().get()));
			}
		}
		return eob;
	}

}
