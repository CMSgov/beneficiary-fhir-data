package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.AdjudicationComponent;
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
import org.hl7.fhir.dstu3.model.SimpleQuantity;
import org.hl7.fhir.dstu3.model.TemporalPrecisionEnum;

import com.justdavis.karl.misc.exceptions.BadCodeMonkeyException;

import gov.hhs.cms.bluebutton.data.model.rif.CarrierClaim;
import gov.hhs.cms.bluebutton.data.model.rif.CarrierClaimLine;
import gov.hhs.cms.bluebutton.data.model.rif.parse.InvalidRifValueException;
import gov.hhs.cms.bluebutton.server.app.stu3.providers.Diagnosis.DiagnosisLabel;

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

		eob.setId(TransformerUtils.buildEobId(ClaimType.CARRIER, claimGroup.getClaimId()));
		eob.setType(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_SYSTEM_CCW_CLAIM_TYPE,
				claimGroup.getClaimTypeCode()));
		eob.addIdentifier().setSystem(TransformerConstants.CODING_SYSTEM_CCW_CLAIM_ID)
				.setValue(claimGroup.getClaimId());
		eob.addIdentifier().setSystem(TransformerConstants.CODING_SYSTEM_CCW_CLAIM_GRP_ID)
				.setValue(claimGroup.getClaimGroupId().toPlainString());

		eob.getInsurance().setCoverage(
				TransformerUtils.referenceCoverage(claimGroup.getBeneficiaryId(), TransformerConstants.COVERAGE_PLAN_PART_B));
		eob.setPatient(TransformerUtils.referencePatient(claimGroup.getBeneficiaryId()));
		eob.setStatus(ExplanationOfBenefitStatus.ACTIVE);

		TransformerUtils.addExtensionCoding(eob.getType(), TransformerConstants.CODING_SYSTEM_CCW_RECORD_ID_CD,
				TransformerConstants.CODING_SYSTEM_CCW_RECORD_ID_CD,
				String.valueOf(claimGroup.getNearLineRecordIdCode()));

		TransformerUtils.validatePeriodDates(claimGroup.getDateFrom(), claimGroup.getDateThrough());
		TransformerUtils.setPeriodStart(eob.getBillablePeriod(), claimGroup.getDateFrom());
		TransformerUtils.setPeriodEnd(eob.getBillablePeriod(), claimGroup.getDateThrough());

		eob.setDisposition(TransformerConstants.CODING_SYSTEM_CCW_CARR_CLAIM_DISPOSITION);
		TransformerUtils.addExtensionCoding(eob, TransformerConstants.CODING_SYSTEM_CCW_CARR_CARRIER_NUMBER,
				TransformerConstants.CODING_SYSTEM_CCW_CARR_CARRIER_NUMBER, claimGroup.getCarrierNumber());
		TransformerUtils.addExtensionCoding(eob, TransformerConstants.CODING_SYSTEM_CCW_CARR_PAYMENT_DENIAL_CD,
				TransformerConstants.CODING_SYSTEM_CCW_CARR_PAYMENT_DENIAL_CD, claimGroup.getPaymentDenialCode());
		eob.getPayment().setAmount((Money) new Money().setSystem(TransformerConstants.CODING_SYSTEM_MONEY_US)
				.setValue(claimGroup.getPaymentAmount()));

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
			referral.addRecipient(TransformerUtils.referencePractitioner(claimGroup.getReferringPhysicianNpi().get()));
			// Set the ReferralRequest as a contained resource in the EOB:
			eob.setReferral(new Reference(referral));
		}

		if (claimGroup.getProviderAssignmentIndicator().isPresent()) {
			TransformerUtils.addExtensionCoding(eob, TransformerConstants.CODING_SYSTEM_CCW_PROVIDER_ASSIGNMENT,
					TransformerConstants.CODING_SYSTEM_CCW_PROVIDER_ASSIGNMENT,
					String.valueOf(claimGroup.getProviderAssignmentIndicator().get()));
		}

		BenefitBalanceComponent benefitBalances = new BenefitBalanceComponent(
				TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BENEFIT_BALANCE_URL, "Medical"));
		eob.getBenefitBalance().add(benefitBalances);

		if (!claimGroup.getProviderPaymentAmount().equals(BigDecimal.ZERO)) {
			BenefitComponent providerPaymentAmount = new BenefitComponent(TransformerUtils.createCodeableConcept(
					TransformerConstants.BENEFIT_BALANCE_TYPE, TransformerConstants.CODED_ADJUDICATION_PAYMENT_B));
			providerPaymentAmount.setAllowed(new Money().setSystem(TransformerConstants.CODING_SYSTEM_MONEY_US)
					.setValue(claimGroup.getProviderPaymentAmount()));
			benefitBalances.getFinancial().add(providerPaymentAmount);
		}

		if (!claimGroup.getBeneficiaryPaymentAmount().equals(BigDecimal.ZERO)) {
			BenefitComponent beneficiaryPaymentAmount = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODED_ADJUDICATION_BENEFICIARY_PAYMENT_AMOUNT));
			beneficiaryPaymentAmount.setAllowed(new Money().setSystem(TransformerConstants.CODING_SYSTEM_MONEY_US)
					.setValue(claimGroup.getBeneficiaryPaymentAmount()));
			benefitBalances.getFinancial().add(beneficiaryPaymentAmount);
		}

		if (!claimGroup.getSubmittedChargeAmount().equals(BigDecimal.ZERO)) {
			BenefitComponent submittedChargeAmount = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODED_ADJUDICATION_SUBMITTED_CHARGE_AMOUNT));
			submittedChargeAmount.setAllowed(new Money().setSystem(TransformerConstants.CODING_SYSTEM_MONEY_US)
					.setValue(claimGroup.getSubmittedChargeAmount()));
			benefitBalances.getFinancial().add(submittedChargeAmount);
		}

		if (!claimGroup.getAllowedChargeAmount().equals(BigDecimal.ZERO)) {
			BenefitComponent allowedChargeAmount = new BenefitComponent(TransformerUtils.createCodeableConcept(
					TransformerConstants.BENEFIT_BALANCE_TYPE, TransformerConstants.CODED_ADJUDICATION_ALLOWED_CHARGE));
			allowedChargeAmount.setAllowed(new Money().setSystem(TransformerConstants.CODING_SYSTEM_MONEY_US)
					.setValue(claimGroup.getAllowedChargeAmount()));
			benefitBalances.getFinancial().add(allowedChargeAmount);
		}

		if (!claimGroup.getBeneficiaryPartBDeductAmount().equals(BigDecimal.ZERO)) {
			BenefitComponent beneficiaryPartBDeductAmount = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODED_ADJUDICATION_NCH_BENEFICIARY_PART_B_DEDUCTIBLE));
			beneficiaryPartBDeductAmount.setAllowed(new Money().setSystem(TransformerConstants.CODING_SYSTEM_MONEY_US)
					.setValue(claimGroup.getBeneficiaryPartBDeductAmount()));
			benefitBalances.getFinancial().add(beneficiaryPartBDeductAmount);
		}

		for (Diagnosis diagnosis : extractDiagnoses(claimGroup))
			TransformerUtils.addDiagnosisCode(eob, diagnosis);

		if (claimGroup.getClinicalTrialNumber().isPresent()) {
			TransformerUtils.addExtensionCoding(eob, TransformerConstants.CODING_SYSTEM_CCW_CARR_CLINICAL_TRIAL_NUMBER,
					TransformerConstants.CODING_SYSTEM_CCW_CARR_CLINICAL_TRIAL_NUMBER,
					claimGroup.getClinicalTrialNumber().get());
		}

		for (CarrierClaimLine claimLine : claimGroup.getLines()) {
			ItemComponent item = eob.addItem();
			item.setSequence(claimLine.getLineNumber().intValue());

			TransformerUtils.addExtensionCoding(item, TransformerConstants.CODING_SYSTEM_FHIR_EOB_ITEM_TYPE,
					TransformerConstants.CODING_SYSTEM_FHIR_EOB_ITEM_TYPE,
					TransformerConstants.CODED_EOB_ITEM_TYPE_CLINICAL_SERVICES_AND_PRODUCTS);

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
						TransformerConstants.CODING_SYSTEM_NPI_US, claimLine.getPerformingPhysicianNpi().get(),
						TransformerConstants.CARE_TEAM_ROLE_PRIMARY);
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
						TransformerUtils.createCodeableConcept(TransformerConstants.CODING_SYSTEM_CCW_CARR_PROVIDER_SPECIALTY_CD,
								"" + claimLine.getProviderSpecialityCode().get()));
				TransformerUtils.addExtensionCoding(performingCareTeamMember,
						TransformerConstants.CODING_SYSTEM_CCW_CARR_PROVIDER_TYPE_CD,
						TransformerConstants.CODING_SYSTEM_CCW_CARR_PROVIDER_TYPE_CD,
						"" + claimLine.getProviderTypeCode());

				TransformerUtils.addExtensionCoding(performingCareTeamMember,
						TransformerConstants.CODING_SYSTEM_CCW_CARR_PROVIDER_PARTICIPATING_CD,
						TransformerConstants.CODING_SYSTEM_CCW_CARR_PROVIDER_PARTICIPATING_CD,
						"" + claimLine.getProviderParticipatingIndCode().get());
				if (claimLine.getOrganizationNpi().isPresent()) {
					TransformerUtils.addExtensionCoding(performingCareTeamMember, TransformerConstants.CODING_SYSTEM_NPI_US,
							TransformerConstants.CODING_SYSTEM_NPI_US, "" + claimLine.getOrganizationNpi().get());
				}
			}

			item.setLocation(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_SYSTEM_FHIR_EOB_ITEM_LOCATION,
					claimLine.getPlaceOfServiceCode()));

			if (claimLine.getProviderStateCode().isPresent()) {
				TransformerUtils.addExtensionCoding(item.getLocation(), TransformerConstants.CODING_SYSTEM_CCW_CARR_PROVIDER_STATE_CD,
						TransformerConstants.CODING_SYSTEM_CCW_CARR_PROVIDER_STATE_CD,
						claimLine.getProviderStateCode().get());
			}

			if (claimLine.getProviderZipCode().isPresent()) {
				TransformerUtils.addExtensionCoding(item.getLocation(), TransformerConstants.CODING_SYSTEM_CCW_CARR_PROVIDER_ZIP_CD,
						TransformerConstants.CODING_SYSTEM_CCW_CARR_PROVIDER_ZIP_CD,
						claimLine.getProviderZipCode().get());
			}

			item.addAdjudication()
					.setCategory(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_SYSTEM_ADJUDICATION_CMS,
							TransformerConstants.CODED_ADJUDICATION_PHYSICIAN_ASSISTANT))
					.setReason(
							TransformerUtils.createCodeableConcept(TransformerConstants.CODING_SYSTEM_PHYSICIAN_ASSISTANT_ADJUDICATION,
									"" + claimLine.getReducedPaymentPhysicianAsstCode()));

			SimpleQuantity serviceCount = new SimpleQuantity();
			serviceCount.setValue(claimLine.getServiceCount());
			item.setQuantity(serviceCount);

			item.setCategory(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_SYSTEM_FHIR_EOB_ITEM_TYPE_SERVICE,
					"" + claimLine.getCmsServiceTypeCode()));

			TransformerUtils.addExtensionCoding(item.getLocation(), TransformerConstants.CODING_SYSTEM_CCW_PRICING_LOCALITY,
					TransformerConstants.CODING_SYSTEM_CCW_PRICING_LOCALITY, claimLine.getLinePricingLocalityCode());

			if (claimLine.getFirstExpenseDate().isPresent() && claimLine.getLastExpenseDate().isPresent()) {
				TransformerUtils.validatePeriodDates(claimLine.getFirstExpenseDate(), claimLine.getLastExpenseDate());
				item.setServiced(new Period()
						.setStart((TransformerUtils.convertToDate(claimLine.getFirstExpenseDate().get())), TemporalPrecisionEnum.DAY)
						.setEnd((TransformerUtils.convertToDate(claimLine.getLastExpenseDate().get())), TemporalPrecisionEnum.DAY));
			}

			if (claimLine.getHcpcsCode().isPresent()) {
				item.setService(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_SYSTEM_HCPCS,
						"" + claimGroup.getHcpcsYearCode().get(), claimLine.getHcpcsCode().get()));
			}
			if (claimLine.getHcpcsInitialModifierCode().isPresent()) {
				item.addModifier(TransformerUtils.createCodeableConcept(TransformerConstants.HCPCS_INITIAL_MODIFIER_CODE1,
						"" + claimGroup.getHcpcsYearCode().get(), claimLine.getHcpcsInitialModifierCode().get()));
			}
			if (claimLine.getHcpcsSecondModifierCode().isPresent()) {
				item.addModifier(TransformerUtils.createCodeableConcept(TransformerConstants.HCPCS_INITIAL_MODIFIER_CODE2,
						"" + claimGroup.getHcpcsYearCode().get(), claimLine.getHcpcsSecondModifierCode().get()));
			}
			if (claimLine.getBetosCode().isPresent()) {
				TransformerUtils.addExtensionCoding(item, TransformerConstants.CODING_SYSTEM_BETOS,
						TransformerConstants.CODING_SYSTEM_BETOS, claimLine.getBetosCode().get());
			}

			TransformerUtils.addExtensionCoding(item, TransformerConstants.CODING_SYSTEM_CMS_LINE_DEDUCTIBLE_SWITCH,
					TransformerConstants.CODING_SYSTEM_CMS_LINE_DEDUCTIBLE_SWITCH,
					"" + claimLine.getServiceDeductibleCode().get());

			AdjudicationComponent adjudicationForPayment = item.addAdjudication();
			adjudicationForPayment
					.setCategory(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_SYSTEM_ADJUDICATION_CMS,
							TransformerConstants.CODED_ADJUDICATION_PAYMENT))
					.getAmount().setSystem(TransformerConstants.CODING_SYSTEM_MONEY)
					.setCode(TransformerConstants.CODING_SYSTEM_MONEY_US).setValue(claimLine.getPaymentAmount());
			TransformerUtils.addExtensionCoding(adjudicationForPayment,
					TransformerConstants.CODING_SYSTEM_CMS_LINE_PAYMENT_INDICATOR_SWITCH,
					TransformerConstants.CODING_SYSTEM_CMS_LINE_PAYMENT_INDICATOR_SWITCH,
					"" + claimLine.getPaymentCode().get());

			item.addAdjudication()
					.setCategory(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_SYSTEM_ADJUDICATION_CMS,
							TransformerConstants.CODED_ADJUDICATION_BENEFICIARY_PAYMENT_AMOUNT))
					.getAmount().setSystem(TransformerConstants.CODING_SYSTEM_MONEY)
					.setCode(TransformerConstants.CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.getBeneficiaryPaymentAmount());

			item.addAdjudication()
					.setCategory(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_SYSTEM_ADJUDICATION_CMS,
							TransformerConstants.CODED_ADJUDICATION_PAYMENT_B))
					.getAmount().setSystem(TransformerConstants.CODING_SYSTEM_MONEY)
					.setCode(TransformerConstants.CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.getProviderPaymentAmount());

			item.addAdjudication()
					.setCategory(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_SYSTEM_ADJUDICATION_CMS,
							TransformerConstants.CODED_ADJUDICATION_DEDUCTIBLE))
					.getAmount().setSystem(TransformerConstants.CODING_SYSTEM_MONEY)
					.setCode(TransformerConstants.CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.getBeneficiaryPartBDeductAmount());

			if (claimLine.getPrimaryPayerCode().isPresent()) {
				TransformerUtils.addExtensionCoding(item, TransformerConstants.CODING_SYSTEM_PRIMARY_PAYER_CD,
						TransformerConstants.CODING_SYSTEM_PRIMARY_PAYER_CD,
						String.valueOf(claimLine.getPrimaryPayerCode().get()));
			}

			item.addAdjudication()
					.setCategory(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_SYSTEM_ADJUDICATION_CMS,
							TransformerConstants.CODED_ADJUDICATION_PRIMARY_PAYER_PAID_AMOUNT))
					.getAmount().setSystem(TransformerConstants.CODING_SYSTEM_MONEY)
					.setCode(TransformerConstants.CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.getPrimaryPayerPaidAmount());

			item.addAdjudication()
					.setCategory(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_SYSTEM_ADJUDICATION_CMS,
							TransformerConstants.CODED_ADJUDICATION_LINE_COINSURANCE_AMOUNT))
					.getAmount().setSystem(TransformerConstants.CODING_SYSTEM_MONEY)
					.setCode(TransformerConstants.CODING_SYSTEM_MONEY_US).setValue(claimLine.getCoinsuranceAmount());

			item.addAdjudication()
					.setCategory(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_SYSTEM_ADJUDICATION_CMS,
							TransformerConstants.CODED_ADJUDICATION_SUBMITTED_CHARGE_AMOUNT))
					.getAmount().setSystem(TransformerConstants.CODING_SYSTEM_MONEY)
					.setCode(TransformerConstants.CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.getSubmittedChargeAmount());

			item.addAdjudication()
					.setCategory(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_SYSTEM_ADJUDICATION_CMS,
							TransformerConstants.CODED_ADJUDICATION_ALLOWED_CHARGE))
					.getAmount().setSystem(TransformerConstants.CODING_SYSTEM_MONEY)
					.setCode(TransformerConstants.CODING_SYSTEM_MONEY_US).setValue(claimLine.getAllowedChargeAmount());

			if (claimLine.getMtusCode().isPresent()) {
				TransformerUtils.addExtensionCoding(item, TransformerConstants.CODING_SYSTEM_MTUS_CD,
						TransformerConstants.CODING_SYSTEM_MTUS_CD, String.valueOf(claimLine.getMtusCode().get()));
			}

			if (!claimLine.getMtusCount().equals(BigDecimal.ZERO)) {
				TransformerUtils.addExtensionCoding(item, TransformerConstants.CODING_SYSTEM_MTUS_COUNT,
						TransformerConstants.CODING_SYSTEM_MTUS_COUNT, String.valueOf(claimLine.getMtusCount()));
			}

			item.addAdjudication()
					.setCategory(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_SYSTEM_ADJUDICATION_CMS,
							TransformerConstants.CODED_ADJUDICATION_LINE_PROCESSING_INDICATOR))
					.setReason(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_SYSTEM_CMS_LINE_PROCESSING_INDICATOR,
							claimLine.getProcessingIndicatorCode().get()));

			Optional<Diagnosis> lineDiagnosis = extractDiagnosis(claimLine);
			if (lineDiagnosis.isPresent())
				TransformerUtils.addDiagnosisLink(eob, item, lineDiagnosis.get());

			if (claimLine.getNationalDrugCode().isPresent()) {
				TransformerUtils.addExtensionCoding(item, TransformerConstants.CODING_SYSTEM_NDC, TransformerConstants.CODING_SYSTEM_NDC,
						claimLine.getNationalDrugCode().get());
			}

			if (claimLine.getHctHgbTestTypeCode().isPresent()
					&& claimLine.getHctHgbTestResult().compareTo(BigDecimal.ZERO) != 0) {
				Observation hctHgbObservation = new Observation();
				hctHgbObservation.setStatus(ObservationStatus.UNKNOWN);
				CodeableConcept hctHgbTestType = new CodeableConcept();
				hctHgbTestType.addCoding().setSystem(TransformerConstants.CODING_SYSTEM_CMS_HCT_OR_HGB_TEST_TYPE)
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

			if (claimLine.getCliaLabNumber().isPresent()) {
				TransformerUtils.addExtensionCoding(item.getLocation(), TransformerConstants.CODING_SYSTEM_CLIA_LAB_NUM,
						TransformerConstants.CODING_SYSTEM_CLIA_LAB_NUM, claimLine.getCliaLabNumber().get());
			}
		}

		return eob;
	}

	/**
	 * @param claim
	 *            the {@link CarrierClaim} to extract the {@link Diagnosis}es
	 *            from
	 * @return the {@link Diagnosis}es that can be extracted from the specified
	 *         {@link CarrierClaim}
	 */
	private static List<Diagnosis> extractDiagnoses(CarrierClaim claim) {
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
	 *            the {@link CarrierClaimLine} to extract the {@link Diagnosis}
	 *            from
	 * @return the {@link Diagnosis} that was present in the specified
	 *         {@link CarrierClaimLine}, if any
	 */
	private static Optional<Diagnosis> extractDiagnosis(CarrierClaimLine claimLine) {
		return Diagnosis.from(claimLine.getDiagnosisCode(), claimLine.getDiagnosisCodeVersion());
	}
}
