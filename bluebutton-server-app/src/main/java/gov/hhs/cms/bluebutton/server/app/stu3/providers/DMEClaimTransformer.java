package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.BenefitBalanceComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.BenefitComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ExplanationOfBenefitStatus;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.dstu3.model.Money;
import org.hl7.fhir.dstu3.model.codesystems.BenefitCategory;
import org.hl7.fhir.dstu3.model.codesystems.ClaimCareteamrole;

import com.justdavis.karl.misc.exceptions.BadCodeMonkeyException;

import gov.hhs.cms.bluebutton.data.model.rif.DMEClaim;
import gov.hhs.cms.bluebutton.data.model.rif.DMEClaimLine;
import gov.hhs.cms.bluebutton.server.app.stu3.providers.Diagnosis.DiagnosisLabel;

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

		eob.setId(TransformerUtils.buildEobId(ClaimType.DME, claimGroup.getClaimId()));
		eob.setType(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_CLAIM_TYPE,
				claimGroup.getClaimTypeCode()));
		eob.addIdentifier().setSystem(TransformerConstants.CODING_CCW_CLAIM_ID)
				.setValue(claimGroup.getClaimId());
		eob.addIdentifier().setSystem(TransformerConstants.CODING_CCW_CLAIM_GROUP_ID)
				.setValue(claimGroup.getClaimGroupId().toPlainString());

		// map eob type codes into FHIR
		TransformerUtils.mapEobType(eob, ClaimType.DME, Optional.of(claimGroup.getNearLineRecordIdCode()), 
				Optional.of(claimGroup.getClaimTypeCode()));
		
		eob.getInsurance()
				.setCoverage(TransformerUtils.referenceCoverage(claimGroup.getBeneficiaryId(), MedicareSegment.PART_B));
		eob.setPatient(TransformerUtils.referencePatient(claimGroup.getBeneficiaryId()));
		eob.setStatus(ExplanationOfBenefitStatus.ACTIVE);

		if (claimGroup.getClinicalTrialNumber().isPresent()) {
			/*
			 * FIXME this should be mapped as an extension valueIdentifier
			 * instead of as a valueCodeableConcept
			 */
			TransformerUtils.addExtensionCoding(eob, TransformerConstants.EXTENSION_IDENTIFIER_CLINICAL_TRIAL_NUMBER,
					TransformerConstants.EXTENSION_IDENTIFIER_CLINICAL_TRIAL_NUMBER,
					claimGroup.getClinicalTrialNumber().get());
		}

		TransformerUtils.validatePeriodDates(claimGroup.getDateFrom(), claimGroup.getDateThrough());
		TransformerUtils.setPeriodStart(eob.getBillablePeriod(), claimGroup.getDateFrom());
		TransformerUtils.setPeriodEnd(eob.getBillablePeriod(), claimGroup.getDateThrough());

		eob.setDisposition(TransformerConstants.CODED_EOB_DISPOSITION);

		eob.getPayment()
				.setAmount((Money) new Money().setSystem(TransformerConstants.CODED_MONEY_USD)
						.setValue(claimGroup.getPaymentAmount()));

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
		TransformerUtils.mapEobCommonGroupCarrierDME(eob, claimGroup.getBeneficiaryId(), claimGroup.getCarrierNumber(),
				claimGroup.getClinicalTrialNumber(), claimGroup.getBeneficiaryPartBDeductAmount(),
				claimGroup.getPaymentDenialCode(), claimGroup.getReferringPhysicianNpi(),
				Optional.of(claimGroup.getProviderAssignmentIndicator()), claimGroup.getProviderPaymentAmount(),
				claimGroup.getBeneficiaryPaymentAmount(), claimGroup.getSubmittedChargeAmount(),
				claimGroup.getAllowedChargeAmount());

		for (Diagnosis diagnosis : extractDiagnoses(claimGroup))
			TransformerUtils.addDiagnosisCode(eob, diagnosis);

		for (DMEClaimLine claimLine : claimGroup.getLines()) {
			ItemComponent item = eob.addItem();
			item.setSequence(claimLine.getLineNumber().intValue());

			/*
			 * Per Michelle at GDIT, and also Tony Dean at OEDA, the performing
			 * provider _should_ always be present. However, we've found some
			 * examples in production where it's not for some claim lines. (This
			 * is annoying, as it's present on other lines in the same claim,
			 * and the data indicates that the same NPI probably applies to the
			 * lines where it's not specified. Still, it's not safe to guess at
			 * this, so we'll leave it blank.)
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

			if (claimLine.getHcpcsCode().isPresent()) {
				item.setService(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_HCPCS,
						"" + claimGroup.getHcpcsYearCode().get(), claimLine.getHcpcsCode().get()));
			}
			if (claimLine.getHcpcsInitialModifierCode().isPresent()) {
				item.addModifier(TransformerUtils.createCodeableConcept(
						TransformerConstants.CODING_HCPCS, "" + claimGroup.getHcpcsYearCode().get(),
						claimLine.getHcpcsInitialModifierCode().get()));
			}
			if (claimLine.getHcpcsSecondModifierCode().isPresent()) {
				item.addModifier(TransformerUtils.createCodeableConcept(
						TransformerConstants.CODING_HCPCS, "" + claimGroup.getHcpcsYearCode().get(),
						claimLine.getHcpcsSecondModifierCode().get()));
			}

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

			if (!claimLine.getScreenSavingsAmount().equals(BigDecimal.ZERO)) {
				/*
				 * FIXME this should be mapped as an extension valueQuantity
				 * instead of as a valueCodeableConcept
				 */
				TransformerUtils.addExtensionCoding(item, TransformerConstants.EXTENSION_SCREEN_SAVINGS,
						TransformerConstants.EXTENSION_SCREEN_SAVINGS,
						String.valueOf(claimLine.getScreenSavingsAmount().get()));
			}

			if (claimLine.getMtusCode().isPresent()) {
				TransformerUtils.addExtensionCoding(item, TransformerConstants.EXTENSION_CODING_MTUS,
						TransformerConstants.EXTENSION_CODING_MTUS,
						String.valueOf(claimLine.getMtusCode().get()));
			}

			if (!claimLine.getMtusCount().equals(BigDecimal.ZERO)) {
				/*
				 * FIXME this should be mapped as a valueQuantity, not a
				 * valueCoding
				 */
				TransformerUtils.addExtensionCoding(item, TransformerConstants.EXTENSION_MTUS_COUNT,
						TransformerConstants.EXTENSION_MTUS_COUNT,
						String.valueOf(claimLine.getMtusCount()));
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
					claimLine.getCmsServiceTypeCode(), claimLine.getNationalDrugCode());

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

	/**
	 * @param claim
	 *            the {@link DMEClaim} to extract the {@link Diagnosis}es from
	 * @return the {@link Diagnosis}es that can be extracted from the specified
	 *         {@link DMEClaim}
	 */
	private static List<Diagnosis> extractDiagnoses(DMEClaim claim) {
		List<Diagnosis> diagnoses = new LinkedList<>();

		/*
		 * Seems silly, but allows the block below to be simple one-liners,
		 * rather than requiring if-blocks.
		 */
		Consumer<Optional<Diagnosis>> diagnosisAdder = d -> {
			if (d.isPresent())
				diagnoses.add(d.get());
		};

		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosisPrincipalCode(),
				claim.getDiagnosisPrincipalCodeVersion(), DiagnosisLabel.PRINCIPAL));
		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosis1Code(), claim.getDiagnosis1CodeVersion()));
		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosis2Code(), claim.getDiagnosis2CodeVersion()));
		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosis3Code(), claim.getDiagnosis3CodeVersion()));
		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosis4Code(), claim.getDiagnosis4CodeVersion()));
		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosis5Code(), claim.getDiagnosis5CodeVersion()));
		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosis6Code(), claim.getDiagnosis6CodeVersion()));
		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosis7Code(), claim.getDiagnosis7CodeVersion()));
		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosis8Code(), claim.getDiagnosis8CodeVersion()));
		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosis9Code(), claim.getDiagnosis9CodeVersion()));
		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosis10Code(), claim.getDiagnosis10CodeVersion()));
		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosis11Code(), claim.getDiagnosis11CodeVersion()));
		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosis12Code(), claim.getDiagnosis12CodeVersion()));

		return diagnoses;
	}

}
