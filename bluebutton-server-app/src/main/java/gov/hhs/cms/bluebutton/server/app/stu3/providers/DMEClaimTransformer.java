package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.BenefitBalanceComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.BenefitComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ExplanationOfBenefitStatus;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.dstu3.model.Money;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Observation.ObservationStatus;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Quantity;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ReferralRequest;
import org.hl7.fhir.dstu3.model.ReferralRequest.ReferralRequestRequesterComponent;
import org.hl7.fhir.dstu3.model.ReferralRequest.ReferralRequestStatus;
import org.hl7.fhir.dstu3.model.TemporalPrecisionEnum;
import org.hl7.fhir.dstu3.model.codesystems.BenefitCategory;
import org.hl7.fhir.dstu3.model.codesystems.ClaimCareteamrole;

import com.justdavis.karl.misc.exceptions.BadCodeMonkeyException;

import gov.hhs.cms.bluebutton.data.model.rif.DMEClaim;
import gov.hhs.cms.bluebutton.data.model.rif.DMEClaimLine;
import gov.hhs.cms.bluebutton.data.model.rif.parse.InvalidRifValueException;
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

		eob.setType(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_CLAIM_TYPE,
				claimGroup.getClaimTypeCode()));
		eob.getType().addCoding().setSystem(TransformerConstants.CODING_CCW_RECORD_ID_CODE)
				.setCode(String.valueOf(claimGroup.getNearLineRecordIdCode()));
		/*
		 * Note: Can't add a org.hl7.fhir.dstu3.model.codesystems.ClaimType
		 * Coding as there is not currently an option there that works for DME
		 * claims.
		 */

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
		/*
		 * FIXME this should be mapped as an extension valueIdentifier instead
		 * of as a valueCodeableConcept
		 */
		TransformerUtils.addExtensionCoding(eob, TransformerConstants.EXTENSION_IDENTIFIER_CARRIER_NUMBER,
				TransformerConstants.EXTENSION_IDENTIFIER_CARRIER_NUMBER,
				claimGroup.getCarrierNumber());
		TransformerUtils.addExtensionCoding(eob, TransformerConstants.EXTENSION_CODING_CCW_CARR_PAYMENT_DENIAL,
				TransformerConstants.EXTENSION_CODING_CCW_CARR_PAYMENT_DENIAL,
				claimGroup.getPaymentDenialCode());
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

		/*
		 * Referrals are represented as contained resources, because otherwise
		 * updating them would require an extra roundtrip to the server (can't
		 * think of an intelligent client-specified ID for them).
		 */
		if (claimGroup.getReferringPhysicianNpi().isPresent()) {
			ReferralRequest referral = new ReferralRequest();
			referral.setStatus(ReferralRequestStatus.COMPLETED);
			referral.setSubject(TransformerUtils.referencePatient(claimGroup.getBeneficiaryId()));
			referral.setRequester(new ReferralRequestRequesterComponent(
					TransformerUtils.referencePractitioner(claimGroup.getReferringPhysicianNpi().get())));
			// Set the ReferralRequest as a contained resource in the EOB:
			eob.setReferral(new Reference(referral));
		}

		TransformerUtils.addExtensionCoding(eob, TransformerConstants.CODING_CCW_PROVIDER_ASSIGNMENT,
				TransformerConstants.CODING_CCW_PROVIDER_ASSIGNMENT,
				String.valueOf(claimGroup.getProviderAssignmentIndicator()));

		if (!claimGroup.getProviderPaymentAmount().equals(BigDecimal.ZERO)) {
			BenefitComponent providerPaymentAmount = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODED_ADJUDICATION_PROVIDER_PAYMENT_AMOUNT));
			providerPaymentAmount.setAllowed(
					new Money().setSystem(TransformerConstants.CODED_MONEY_USD)
							.setValue(claimGroup.getProviderPaymentAmount()));
			benefitBalances.getFinancial().add(providerPaymentAmount);
		}

		if (!claimGroup.getBeneficiaryPaymentAmount().equals(BigDecimal.ZERO)) {
			BenefitComponent beneficiaryPaymentAmount = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODED_ADJUDICATION_BENEFICIARY_PAYMENT_AMOUNT));
			beneficiaryPaymentAmount.setAllowed(
					new Money().setSystem(TransformerConstants.CODED_MONEY_USD)
							.setValue(claimGroup.getBeneficiaryPaymentAmount()));
			benefitBalances.getFinancial().add(beneficiaryPaymentAmount);
		}

		if (!claimGroup.getSubmittedChargeAmount().equals(BigDecimal.ZERO)) {
			BenefitComponent submittedChargeAmount = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODED_ADJUDICATION_SUBMITTED_CHARGE_AMOUNT));
			submittedChargeAmount.setAllowed(
					new Money().setSystem(TransformerConstants.CODED_MONEY_USD)
							.setValue(claimGroup.getSubmittedChargeAmount()));
			benefitBalances.getFinancial().add(submittedChargeAmount);
		}

		if (!claimGroup.getAllowedChargeAmount().equals(BigDecimal.ZERO)) {
			BenefitComponent allowedChargeAmount = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODED_ADJUDICATION_ALLOWED_CHARGE));
			allowedChargeAmount
					.setAllowed(new Money().setSystem(TransformerConstants.CODED_MONEY_USD)
							.setValue(claimGroup.getAllowedChargeAmount()));
			benefitBalances.getFinancial().add(allowedChargeAmount);
		}

		if (!claimGroup.getBeneficiaryPartBDeductAmount().equals(BigDecimal.ZERO)) {
			BenefitComponent beneficiaryPartBDeductAmount = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODED_ADJUDICATION_NCH_BENEFICIARY_PART_B_DEDUCTIBLE));
			beneficiaryPartBDeductAmount.setAllowed(
					new Money().setSystem(TransformerConstants.CODED_MONEY_USD)
							.setValue(claimGroup.getBeneficiaryPartBDeductAmount()));
			benefitBalances.getFinancial().add(beneficiaryPartBDeductAmount);
		}

		for (Diagnosis diagnosis : extractDiagnoses(claimGroup))
			TransformerUtils.addDiagnosisCode(eob, diagnosis);

		TransformerUtils.addExtensionCoding(eob, TransformerConstants.EXTENSION_IDENTIFIER_CLINICAL_TRIAL_NUMBER,
				TransformerConstants.EXTENSION_IDENTIFIER_CLINICAL_TRIAL_NUMBER,
				claimGroup.getClinicalTrialNumber().get());

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

			item.setCategory(
					TransformerUtils.createCodeableConcept(
							TransformerConstants.CODING_CCW_TYPE_SERVICE,
							String.valueOf(claimLine.getCmsServiceTypeCode())));

			item.setLocation(TransformerUtils.createCodeableConcept(
					TransformerConstants.CODING_CCW_PLACE_OF_SERVICE, claimLine.getPlaceOfServiceCode()));
			if (!claimLine.getProviderStateCode().isEmpty()) {
				TransformerUtils.addExtensionCoding(item.getLocation(),
						TransformerConstants.EXTENSION_CODING_CCW_PROVIDER_STATE,
						TransformerConstants.EXTENSION_CODING_CCW_PROVIDER_STATE,
						claimLine.getProviderStateCode());
			}

			if (claimLine.getFirstExpenseDate().isPresent() && claimLine.getLastExpenseDate().isPresent()) {
				TransformerUtils.validatePeriodDates(claimLine.getFirstExpenseDate(), claimLine.getLastExpenseDate());
				item.setServiced(new Period()
						.setStart((TransformerUtils.convertToDate(claimLine.getFirstExpenseDate().get())),
								TemporalPrecisionEnum.DAY)
						.setEnd((TransformerUtils.convertToDate(claimLine.getLastExpenseDate().get())),
								TemporalPrecisionEnum.DAY));
			}

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

			if (claimLine.getBetosCode().isPresent()) {
				TransformerUtils.addExtensionCoding(item, TransformerConstants.CODING_BETOS,
						TransformerConstants.CODING_BETOS, claimLine.getBetosCode().get());
			}

			item.addAdjudication()
					.setCategory(TransformerUtils.createCodeableConcept(
							TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
							TransformerConstants.CODED_ADJUDICATION_PAYMENT))
					.getAmount().setSystem(TransformerConstants.CODING_MONEY)
					.setCode(TransformerConstants.CODED_MONEY_USD)
					.setValue(claimLine.getPaymentAmount());

			item.addAdjudication()
					.setCategory(
							TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
									TransformerConstants.CODED_ADJUDICATION_BENEFICIARY_PAYMENT_AMOUNT))
					.getAmount().setSystem(TransformerConstants.CODING_MONEY)
					.setCode(TransformerConstants.CODED_MONEY_USD)
					.setValue(claimLine.getBeneficiaryPaymentAmount());

			item.addAdjudication()
					.setCategory(TransformerUtils.createCodeableConcept(
							TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
							TransformerConstants.CODED_ADJUDICATION_PROVIDER_PAYMENT_AMOUNT))
					.getAmount().setSystem(TransformerConstants.CODING_MONEY)
					.setCode(TransformerConstants.CODED_MONEY_USD)
					.setValue(claimLine.getProviderPaymentAmount());

			item.addAdjudication()
					.setCategory(TransformerUtils.createCodeableConcept(
							TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
							TransformerConstants.CODED_ADJUDICATION_DEDUCTIBLE))
					.getAmount().setSystem(TransformerConstants.CODING_MONEY)
					.setCode(TransformerConstants.CODED_MONEY_USD)
					.setValue(claimLine.getBeneficiaryPartBDeductAmount());

			if (claimLine.getPrimaryPayerCode().isPresent()) {
				TransformerUtils.addExtensionCoding(item, TransformerConstants.EXTENSION_CODING_PRIMARY_PAYER,
						TransformerConstants.EXTENSION_CODING_PRIMARY_PAYER,
						String.valueOf(claimLine.getPrimaryPayerCode().get()));
			}

			item.addAdjudication()
					.setCategory(
							TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
									TransformerConstants.CODED_ADJUDICATION_PRIMARY_PAYER_PAID_AMOUNT))
					.getAmount().setSystem(TransformerConstants.CODING_MONEY)
					.setCode(TransformerConstants.CODED_MONEY_USD)
					.setValue(claimLine.getPrimaryPayerPaidAmount());

			item.addAdjudication()
					.setCategory(
							TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
									TransformerConstants.CODED_ADJUDICATION_LINE_COINSURANCE_AMOUNT))
					.getAmount().setSystem(TransformerConstants.CODING_MONEY)
					.setCode(TransformerConstants.CODED_MONEY_USD)
					.setValue(claimLine.getCoinsuranceAmount());

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
									TransformerConstants.CODED_ADJUDICATION_SUBMITTED_CHARGE_AMOUNT))
					.getAmount().setSystem(TransformerConstants.CODING_MONEY)
					.setCode(TransformerConstants.CODED_MONEY_USD)
					.setValue(claimLine.getSubmittedChargeAmount());

			item.addAdjudication()
					.setCategory(
							TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
									TransformerConstants.CODED_ADJUDICATION_ALLOWED_CHARGE))
					.getAmount().setSystem(TransformerConstants.CODING_MONEY)
					.setCode(TransformerConstants.CODED_MONEY_USD)
					.setValue(claimLine.getAllowedChargeAmount());

			if (claimLine.getProcessingIndicatorCode().isPresent()) {
				TransformerUtils.addExtensionCoding(item,
						TransformerConstants.CODING_CCW_PROCESSING_INDICATOR,
						TransformerConstants.CODING_CCW_PROCESSING_INDICATOR,
						claimLine.getProcessingIndicatorCode().get());
			}

			if (claimLine.getPaymentCode().isPresent()) {
				TransformerUtils.addExtensionCoding(item,
						TransformerConstants.EXTENSION_CODING_CCW_PAYMENT_80_100_INDICATOR,
						TransformerConstants.EXTENSION_CODING_CCW_PAYMENT_80_100_INDICATOR,
						String.valueOf(claimLine.getPaymentCode().get()));
			}

			if (claimLine.getServiceDeductibleCode().isPresent()) {
				TransformerUtils.addExtensionCoding(item,
						TransformerConstants.EXTENSION_CODING_CCW_LINE_DEDUCTIBLE_SWITCH,
						TransformerConstants.EXTENSION_CODING_CCW_LINE_DEDUCTIBLE_SWITCH,
						String.valueOf(claimLine.getServiceDeductibleCode().get()));
			}


			Optional<Diagnosis> lineDiagnosis = extractDiagnosis(claimLine);
			if (lineDiagnosis.isPresent())
				TransformerUtils.addDiagnosisLink(eob, item, lineDiagnosis.get());

			item.addAdjudication()
					.setCategory(
							TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
									TransformerConstants.CODED_ADJUDICATION_LINE_PURCHASE_PRICE_AMOUNT))
					.getAmount().setSystem(TransformerConstants.CODING_MONEY)
					.setCode(TransformerConstants.CODED_MONEY_USD)
					.setValue(claimLine.getPurchasePriceAmount());

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

			if (claimLine.getHctHgbTestTypeCode().isPresent()
					&& claimLine.getHctHgbTestResult().compareTo(BigDecimal.ZERO) != 0) {
				Observation hctHgbObservation = new Observation();
				hctHgbObservation.setStatus(ObservationStatus.UNKNOWN);
				CodeableConcept hctHgbTestType = new CodeableConcept();
				hctHgbTestType.addCoding().setSystem(TransformerConstants.CODING_CCW_HCT_OR_HGB_TEST_TYPE)
						.setCode(claimLine.getHctHgbTestTypeCode().get());
				hctHgbObservation.setCode(hctHgbTestType);
				hctHgbObservation.setValue(new Quantity().setValue(claimLine.getHctHgbTestResult()));
				item.addExtension().setUrl(TransformerConstants.EXTENSION_CMS_HCT_OR_HGB_RESULTS)
						.setValue(new Reference(hctHgbObservation));
			} else if (!claimLine.getHctHgbTestTypeCode().isPresent()
					&& claimLine.getHctHgbTestResult().compareTo(BigDecimal.ZERO) == 0) {
				// Nothing to do here; don't map a non-existent Observation.
			} else {
				throw new InvalidRifValueException(String.format(
						"Inconsistent hctHgbTestTypeCode and hctHgbTestResult" + " values for claim '%s'.",
						claimGroup.getClaimId()));
			}

			if (claimLine.getNationalDrugCode().isPresent()) {
				TransformerUtils.addExtensionCoding(item, TransformerConstants.CODING_NDC,
						TransformerConstants.CODING_NDC, claimLine.getNationalDrugCode().get());
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

	/**
	 * @param claimLine
	 *            the {@link DMEClaimLine} to extract the {@link Diagnosis} from
	 * @return the {@link Diagnosis} that was present in the specified
	 *         {@link DMEClaimLine}, if any
	 */
	private static Optional<Diagnosis> extractDiagnosis(DMEClaimLine claimLine) {
		return Diagnosis.from(claimLine.getDiagnosisCode(), claimLine.getDiagnosisCodeVersion());
	}
}
