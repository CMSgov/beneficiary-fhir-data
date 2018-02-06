package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.math.BigDecimal;
import java.util.Optional;

import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.BenefitBalanceComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.codesystems.BenefitCategory;
import org.hl7.fhir.dstu3.model.codesystems.ClaimCareteamrole;

import com.justdavis.karl.misc.exceptions.BadCodeMonkeyException;

import gov.hhs.cms.bluebutton.data.model.rif.CarrierClaim;
import gov.hhs.cms.bluebutton.data.model.rif.CarrierClaimLine;

/**
 * Transforms CCW {@link CarrierClaim} instances into FHIR
 * {@link ExplanationOfBenefit} resources.
 */
final class CarrierClaimTransformer {
	/**
	 * @param claim
	 *            the CCW {@link CarrierClaim} to transform
	 * @return a FHIR {@link ExplanationOfBenefit} resource that represents the
	 *         specified {@link CarrierClaim}
	 */
	static ExplanationOfBenefit transform(Object claim) {
		if (!(claim instanceof CarrierClaim))
			throw new BadCodeMonkeyException();
		return transformClaim((CarrierClaim) claim);
	}

	/**
	 * @param claimGroup
	 *            the CCW {@link CarrierClaim} to transform
	 * @return a FHIR {@link ExplanationOfBenefit} resource that represents the
	 *         specified {@link CarrierClaim}
	 */
	private static ExplanationOfBenefit transformClaim(CarrierClaim claimGroup) {
		ExplanationOfBenefit eob = new ExplanationOfBenefit();
		
		// Common group level fields between all claim types
		TransformerUtils.mapEobCommonClaimHeaderData(eob, claimGroup.getClaimId(), claimGroup.getBeneficiaryId(),
				ClaimType.CARRIER, claimGroup.getClaimGroupId().toPlainString(), 
				MedicareSegment.PART_B, Optional.of(claimGroup.getDateFrom()), 
				Optional.of(claimGroup.getDateThrough()), Optional.of(claimGroup.getPaymentAmount()),
				claimGroup.getFinalAction());
		
		// map eob type codes into FHIR
		TransformerUtils.mapEobType(eob, ClaimType.CARRIER, Optional.of(claimGroup.getNearLineRecordIdCode()), 
				Optional.of(claimGroup.getClaimTypeCode()));
		
		BenefitBalanceComponent benefitBalances = new BenefitBalanceComponent(
				TransformerUtils.createCodeableConcept(TransformerConstants.CODING_FHIR_BENEFIT_BALANCE,
						BenefitCategory.MEDICAL.toCode()));
		eob.getBenefitBalance().add(benefitBalances);


		// Common group level fields between Carrier and DME
		TransformerUtils.mapEobCommonGroupCarrierDME(eob, claimGroup.getCarrierNumber(),
				claimGroup.getClinicalTrialNumber(), claimGroup.getBeneficiaryPartBDeductAmount(),
				claimGroup.getPaymentDenialCode(), claimGroup.getProviderAssignmentIndicator(),
				claimGroup.getProviderPaymentAmount(), claimGroup.getBeneficiaryPaymentAmount(),
				claimGroup.getSubmittedChargeAmount(), claimGroup.getAllowedChargeAmount());

		for (Diagnosis diagnosis : TransformerUtils.extractDiagnoses1Thru12(claimGroup.getDiagnosisPrincipalCode(),
				claimGroup.getDiagnosisPrincipalCodeVersion(), 
				claimGroup.getDiagnosis1Code(), claimGroup.getDiagnosis1CodeVersion(), claimGroup.getDiagnosis2Code(),
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

		for (CarrierClaimLine claimLine : claimGroup.getLines()) {
			ItemComponent item = eob.addItem();
			item.setSequence(claimLine.getLineNumber().intValue());

			TransformerUtils.addExtensionCoding(item, TransformerConstants.CODING_FHIR_ACT_INVOICE_GROUP,
					TransformerConstants.CODING_FHIR_ACT_INVOICE_GROUP,
					TransformerConstants.CODED_ACT_INVOICE_GROUP_CLINICAL_SERVICES_AND_PRODUCTS);

			/*
			 * Per Michelle at GDIT, and also Tony Dean at OEDA, the performing
			 * provider _should_ always be present. However, we've found some
			 * examples in production where it's not for some claim lines. (This
			 * is annoying, as it's present on other lines in the same claim,
			 * and the data indicates that the same NPI probably applies to the
			 * lines where it's not specified. Still, it's not safe to guess at
			 * this, so we'll leave it blank.)
			 */
			if (claimLine.getPerformingPhysicianNpi().isPresent()) {
				ExplanationOfBenefit.CareTeamComponent performingCareTeamMember = TransformerUtils.addCareTeamPractitioner(eob, item,
						TransformerConstants.CODING_NPI_US, claimLine.getPerformingPhysicianNpi().get(),
								ClaimCareteamrole.PRIMARY.toCode());
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

				performingCareTeamMember.setQualification(
						TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_PROVIDER_SPECIALTY,
								"" + claimLine.getProviderSpecialityCode().get()));
				TransformerUtils.addExtensionCoding(performingCareTeamMember,
						TransformerConstants.EXTENSION_CODING_CCW_PROVIDER_TYPE,
						TransformerConstants.EXTENSION_CODING_CCW_PROVIDER_TYPE,
						"" + claimLine.getProviderTypeCode());

				TransformerUtils.addExtensionCoding(performingCareTeamMember,
						TransformerConstants.EXTENSION_CODING_CCW_PROVIDER_PARTICIPATING,
						TransformerConstants.EXTENSION_CODING_CCW_PROVIDER_PARTICIPATING,
						"" + claimLine.getProviderParticipatingIndCode().get());
				if (claimLine.getOrganizationNpi().isPresent()) {
					TransformerUtils.addExtensionCoding(performingCareTeamMember, TransformerConstants.CODING_NPI_US,
							TransformerConstants.CODING_NPI_US, "" + claimLine.getOrganizationNpi().get());
				}
			}

			item.addAdjudication()
					.setCategory(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
							TransformerConstants.CODED_ADJUDICATION_PHYSICIAN_ASSISTANT))
					.setReason(
							TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_PHYSICIAN_ASSISTANT_ADJUDICATION,
									"" + claimLine.getReducedPaymentPhysicianAsstCode()));
			
			// set hcpcs modifier codes for the claim
			TransformerUtils.setHcpcsModifierCodes(item, claimLine.getHcpcsCode(),
					claimLine.getHcpcsInitialModifierCode(), claimLine.getHcpcsSecondModifierCode(), claimGroup.getHcpcsYearCode());

			if (claimLine.getMtusCode().isPresent()) {
				TransformerUtils.addExtensionCoding(item, TransformerConstants.EXTENSION_CODING_MTUS_IND,
						TransformerConstants.EXTENSION_CODING_MTUS_IND, String.valueOf(claimLine.getMtusCode().get()));
			}

			if (!claimLine.getMtusCount().equals(BigDecimal.ZERO)) {
				/*
				 * FIXME this should be mapped as a valueQuantity, not a
				 * valueCoding
				 */
				TransformerUtils.addExtensionCoding(item, TransformerConstants.EXTENSION_MTUS_CNT,
						TransformerConstants.EXTENSION_MTUS_CNT, String.valueOf(claimLine.getMtusCount()));
			}

			// Common item level fields between Carrier and DME
			TransformerUtils.mapEobCommonItemCarrierDME(item, eob, claimGroup.getClaimId(), claimLine.getServiceCount(),
					claimLine.getPlaceOfServiceCode(), claimLine.getFirstExpenseDate(),
					claimLine.getLastExpenseDate(), claimLine.getBeneficiaryPaymentAmount(),
					claimLine.getProviderPaymentAmount(), claimLine.getBeneficiaryPartBDeductAmount(),
					claimLine.getPrimaryPayerCode(), claimLine.getPrimaryPayerPaidAmount(), claimLine.getBetosCode(),
					claimLine.getPaymentAmount(), claimLine.getPaymentCode(), claimLine.getCoinsuranceAmount(),
					claimLine.getSubmittedChargeAmount(), claimLine.getAllowedChargeAmount(),
					claimLine.getProcessingIndicatorCode(), claimLine.getServiceDeductibleCode(),
					claimLine.getDiagnosisCode(),
					claimLine.getDiagnosisCodeVersion(), 
					claimLine.getHctHgbTestTypeCode(), claimLine.getHctHgbTestResult(),
					claimLine.getCmsServiceTypeCode(), claimLine.getNationalDrugCode(), claimGroup.getBeneficiaryId(),
					claimGroup.getReferringPhysicianNpi(), claimLine.getPerformingPhysicianNpi());

			if (claimLine.getProviderStateCode().isPresent()) {
				TransformerUtils.addExtensionCoding(item.getLocation(),
					TransformerConstants.EXTENSION_CODING_CCW_PROVIDER_STATE,
					TransformerConstants.EXTENSION_CODING_CCW_PROVIDER_STATE, claimLine.getProviderStateCode().get());
			}

			if (claimLine.getProviderZipCode().isPresent()) {
				TransformerUtils.addExtensionCoding(item.getLocation(),
					TransformerConstants.EXTENSION_CODING_CCW_PROVIDER_ZIP,
					TransformerConstants.EXTENSION_CODING_CCW_PROVIDER_ZIP, claimLine.getProviderZipCode().get());
			}
			TransformerUtils.addExtensionCoding(item.getLocation(),
					TransformerConstants.EXTENSION_CODING_CCW_PRICING_LOCALITY,
					TransformerConstants.EXTENSION_CODING_CCW_PRICING_LOCALITY, claimLine.getLinePricingLocalityCode());
			if (claimLine.getCliaLabNumber().isPresent()) {
				Extension cliaLabNumberExtension = item.getLocation().addExtension();
				cliaLabNumberExtension.setUrl(TransformerConstants.EXTENSION_IDENTIFIER_CCW_CLIA_LAB_NUM);
				cliaLabNumberExtension.setValue(new Identifier()
								.setSystem(TransformerConstants.EXTENSION_IDENTIFIER_CCW_CLIA_LAB_NUM)
								.setValue(claimLine.getCliaLabNumber().get()));
			}
		}

		return eob;
	}

}
