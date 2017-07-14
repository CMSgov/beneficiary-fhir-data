package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Coverage;
import org.hl7.fhir.dstu3.model.DomainResource;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.AdjudicationComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.BenefitBalanceComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.BenefitComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.CareTeamComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.DiagnosisComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ExplanationOfBenefitStatus;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Money;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Observation.ObservationStatus;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.Quantity;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ReferralRequest;
import org.hl7.fhir.dstu3.model.ReferralRequest.ReferralRequestRequesterComponent;
import org.hl7.fhir.dstu3.model.ReferralRequest.ReferralRequestStatus;
import org.hl7.fhir.dstu3.model.SimpleQuantity;
import org.hl7.fhir.dstu3.model.TemporalPrecisionEnum;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private static final Logger LOGGER = LoggerFactory.getLogger(CarrierClaimTransformer.class);

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
	static ExplanationOfBenefit transformClaim(CarrierClaim claimGroup) {
		ExplanationOfBenefit eob = new ExplanationOfBenefit();

		eob.setId(TransformerUtils.buildEobId(ClaimType.CARRIER, claimGroup.getClaimId()));
		eob.setType(createCodeableConcept(TransformerConstants.CODING_SYSTEM_CCW_CLAIM_TYPE,
				claimGroup.getClaimTypeCode()));
		eob.addIdentifier().setSystem(TransformerConstants.CODING_SYSTEM_CCW_CLAIM_ID)
				.setValue(claimGroup.getClaimId());
		eob.addIdentifier().setSystem(TransformerConstants.CODING_SYSTEM_CCW_CLAIM_GRP_ID)
				.setValue(claimGroup.getClaimGroupId().toPlainString());

		eob.getInsurance().setCoverage(
				referenceCoverage(claimGroup.getBeneficiaryId(), TransformerConstants.COVERAGE_PLAN_PART_B));
		eob.setPatient(referencePatient(claimGroup.getBeneficiaryId()));
		eob.setStatus(ExplanationOfBenefitStatus.ACTIVE);

		addExtensionCoding(eob.getType(), TransformerConstants.CODING_SYSTEM_CCW_RECORD_ID_CD,
				TransformerConstants.CODING_SYSTEM_CCW_RECORD_ID_CD,
				String.valueOf(claimGroup.getNearLineRecordIdCode()));

		validatePeriodDates(claimGroup.getDateFrom(), claimGroup.getDateThrough());
		setPeriodStart(eob.getBillablePeriod(), claimGroup.getDateFrom());
		setPeriodEnd(eob.getBillablePeriod(), claimGroup.getDateThrough());

		eob.setDisposition(TransformerConstants.CODING_SYSTEM_CCW_CARR_CLAIM_DISPOSITION);
		addExtensionCoding(eob, TransformerConstants.CODING_SYSTEM_CCW_CARR_CARRIER_NUMBER,
				TransformerConstants.CODING_SYSTEM_CCW_CARR_CARRIER_NUMBER, claimGroup.getCarrierNumber());
		addExtensionCoding(eob, TransformerConstants.CODING_SYSTEM_CCW_CARR_PAYMENT_DENIAL_CD,
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
			referral.setSubject(referencePatient(claimGroup.getBeneficiaryId()));
			referral.setRequester(new ReferralRequestRequesterComponent(
					referencePractitioner(claimGroup.getReferringPhysicianNpi().get())));
			referral.addRecipient(referencePractitioner(claimGroup.getReferringPhysicianNpi().get()));
			// Set the ReferralRequest as a contained resource in the EOB:
			eob.setReferral(new Reference(referral));
		}

		if (claimGroup.getProviderAssignmentIndicator().isPresent()) {
			addExtensionCoding(eob, TransformerConstants.CODING_SYSTEM_CCW_PROVIDER_ASSIGNMENT,
					TransformerConstants.CODING_SYSTEM_CCW_PROVIDER_ASSIGNMENT,
					String.valueOf(claimGroup.getProviderAssignmentIndicator().get()));
		}

		BenefitBalanceComponent benefitBalances = new BenefitBalanceComponent(
				createCodeableConcept(TransformerConstants.CODING_BENEFIT_BALANCE_URL, "Medical"));
		eob.getBenefitBalance().add(benefitBalances);

		if (!claimGroup.getProviderPaymentAmount().equals(BigDecimal.ZERO)) {
			BenefitComponent providerPaymentAmount = new BenefitComponent(createCodeableConcept(
					TransformerConstants.BENEFIT_BALANCE_TYPE, TransformerConstants.CODED_ADJUDICATION_PAYMENT_B));
			providerPaymentAmount.setAllowed(new Money().setSystem(TransformerConstants.CODING_SYSTEM_MONEY_US)
					.setValue(claimGroup.getProviderPaymentAmount()));
			benefitBalances.getFinancial().add(providerPaymentAmount);
		}

		if (!claimGroup.getBeneficiaryPaymentAmount().equals(BigDecimal.ZERO)) {
			BenefitComponent beneficiaryPaymentAmount = new BenefitComponent(
					createCodeableConcept(TransformerConstants.BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODED_ADJUDICATION_BENEFICIARY_PAYMENT_AMOUNT));
			beneficiaryPaymentAmount.setAllowed(new Money().setSystem(TransformerConstants.CODING_SYSTEM_MONEY_US)
					.setValue(claimGroup.getBeneficiaryPaymentAmount()));
			benefitBalances.getFinancial().add(beneficiaryPaymentAmount);
		}

		if (!claimGroup.getSubmittedChargeAmount().equals(BigDecimal.ZERO)) {
			BenefitComponent submittedChargeAmount = new BenefitComponent(
					createCodeableConcept(TransformerConstants.BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODED_ADJUDICATION_SUBMITTED_CHARGE_AMOUNT));
			submittedChargeAmount.setAllowed(new Money().setSystem(TransformerConstants.CODING_SYSTEM_MONEY_US)
					.setValue(claimGroup.getSubmittedChargeAmount()));
			benefitBalances.getFinancial().add(submittedChargeAmount);
		}

		if (!claimGroup.getAllowedChargeAmount().equals(BigDecimal.ZERO)) {
			BenefitComponent allowedChargeAmount = new BenefitComponent(createCodeableConcept(
					TransformerConstants.BENEFIT_BALANCE_TYPE, TransformerConstants.CODED_ADJUDICATION_ALLOWED_CHARGE));
			allowedChargeAmount.setAllowed(new Money().setSystem(TransformerConstants.CODING_SYSTEM_MONEY_US)
					.setValue(claimGroup.getAllowedChargeAmount()));
			benefitBalances.getFinancial().add(allowedChargeAmount);
		}

		if (!claimGroup.getBeneficiaryPartBDeductAmount().equals(BigDecimal.ZERO)) {
			BenefitComponent beneficiaryPartBDeductAmount = new BenefitComponent(
					createCodeableConcept(TransformerConstants.BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODED_ADJUDICATION_NCH_BENEFICIARY_PART_B_DEDUCTIBLE));
			beneficiaryPartBDeductAmount.setAllowed(new Money().setSystem(TransformerConstants.CODING_SYSTEM_MONEY_US)
					.setValue(claimGroup.getBeneficiaryPartBDeductAmount()));
			benefitBalances.getFinancial().add(beneficiaryPartBDeductAmount);
		}

		for (Diagnosis diagnosis : extractDiagnoses(claimGroup))
			addDiagnosisCode(eob, diagnosis);

		if (claimGroup.getClinicalTrialNumber().isPresent()) {
			addExtensionCoding(eob, TransformerConstants.CODING_SYSTEM_CCW_CARR_CLINICAL_TRIAL_NUMBER,
					TransformerConstants.CODING_SYSTEM_CCW_CARR_CLINICAL_TRIAL_NUMBER,
					claimGroup.getClinicalTrialNumber().get());
		}

		for (CarrierClaimLine claimLine : claimGroup.getLines()) {
			ItemComponent item = eob.addItem();
			item.setSequence(claimLine.getLineNumber().intValue());

			addExtensionCoding(item, TransformerConstants.CODING_SYSTEM_FHIR_EOB_ITEM_TYPE,
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
				ExplanationOfBenefit.CareTeamComponent performingCareTeamMember = addCareTeamPractitioner(eob, item,
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
						createCodeableConcept(TransformerConstants.CODING_SYSTEM_CCW_CARR_PROVIDER_SPECIALTY_CD,
								"" + claimLine.getProviderSpecialityCode().get()));
				addExtensionCoding(performingCareTeamMember,
						TransformerConstants.CODING_SYSTEM_CCW_CARR_PROVIDER_TYPE_CD,
						TransformerConstants.CODING_SYSTEM_CCW_CARR_PROVIDER_TYPE_CD,
						"" + claimLine.getProviderTypeCode());

				addExtensionCoding(performingCareTeamMember,
						TransformerConstants.CODING_SYSTEM_CCW_CARR_PROVIDER_PARTICIPATING_CD,
						TransformerConstants.CODING_SYSTEM_CCW_CARR_PROVIDER_PARTICIPATING_CD,
						"" + claimLine.getProviderParticipatingIndCode().get());
				if (claimLine.getOrganizationNpi().isPresent()) {
					addExtensionCoding(performingCareTeamMember, TransformerConstants.CODING_SYSTEM_NPI_US,
							TransformerConstants.CODING_SYSTEM_NPI_US, "" + claimLine.getOrganizationNpi().get());
				}
			}

			item.setLocation(createCodeableConcept(TransformerConstants.CODING_SYSTEM_FHIR_EOB_ITEM_LOCATION,
					claimLine.getPlaceOfServiceCode()));

			if (claimLine.getProviderStateCode().isPresent()) {
				addExtensionCoding(item.getLocation(), TransformerConstants.CODING_SYSTEM_CCW_CARR_PROVIDER_STATE_CD,
						TransformerConstants.CODING_SYSTEM_CCW_CARR_PROVIDER_STATE_CD,
						claimLine.getProviderStateCode().get());
			}

			if (claimLine.getProviderZipCode().isPresent()) {
				addExtensionCoding(item.getLocation(), TransformerConstants.CODING_SYSTEM_CCW_CARR_PROVIDER_ZIP_CD,
						TransformerConstants.CODING_SYSTEM_CCW_CARR_PROVIDER_ZIP_CD,
						claimLine.getProviderZipCode().get());
			}

			item.addAdjudication()
					.setCategory(createCodeableConcept(TransformerConstants.CODING_SYSTEM_ADJUDICATION_CMS,
							TransformerConstants.CODED_ADJUDICATION_PHYSICIAN_ASSISTANT))
					.setReason(
							createCodeableConcept(TransformerConstants.CODING_SYSTEM_PHYSICIAN_ASSISTANT_ADJUDICATION,
									"" + claimLine.getReducedPaymentPhysicianAsstCode()));

			SimpleQuantity serviceCount = new SimpleQuantity();
			serviceCount.setValue(claimLine.getServiceCount());
			item.setQuantity(serviceCount);

			item.setCategory(createCodeableConcept(TransformerConstants.CODING_SYSTEM_FHIR_EOB_ITEM_TYPE_SERVICE,
					"" + claimLine.getCmsServiceTypeCode()));

			addExtensionCoding(item.getLocation(), TransformerConstants.CODING_SYSTEM_CCW_PRICING_LOCALITY,
					TransformerConstants.CODING_SYSTEM_CCW_PRICING_LOCALITY, claimLine.getLinePricingLocalityCode());

			if (claimLine.getFirstExpenseDate().isPresent() && claimLine.getLastExpenseDate().isPresent()) {
				validatePeriodDates(claimLine.getFirstExpenseDate(), claimLine.getLastExpenseDate());
				item.setServiced(new Period()
						.setStart((convertToDate(claimLine.getFirstExpenseDate().get())), TemporalPrecisionEnum.DAY)
						.setEnd((convertToDate(claimLine.getLastExpenseDate().get())), TemporalPrecisionEnum.DAY));
			}

			if (claimLine.getHcpcsCode().isPresent()) {
				item.setService(createCodeableConcept(TransformerConstants.CODING_SYSTEM_HCPCS,
						"" + claimGroup.getHcpcsYearCode().get(), claimLine.getHcpcsCode().get()));
			}
			if (claimLine.getHcpcsInitialModifierCode().isPresent()) {
				item.addModifier(createCodeableConcept(TransformerConstants.HCPCS_INITIAL_MODIFIER_CODE1,
						"" + claimGroup.getHcpcsYearCode().get(), claimLine.getHcpcsInitialModifierCode().get()));
			}
			if (claimLine.getHcpcsSecondModifierCode().isPresent()) {
				item.addModifier(createCodeableConcept(TransformerConstants.HCPCS_INITIAL_MODIFIER_CODE2,
						"" + claimGroup.getHcpcsYearCode().get(), claimLine.getHcpcsSecondModifierCode().get()));
			}
			if (claimLine.getBetosCode().isPresent()) {
				addExtensionCoding(item, TransformerConstants.CODING_SYSTEM_BETOS,
						TransformerConstants.CODING_SYSTEM_BETOS, claimLine.getBetosCode().get());
			}

			addExtensionCoding(item, TransformerConstants.CODING_SYSTEM_CMS_LINE_DEDUCTIBLE_SWITCH,
					TransformerConstants.CODING_SYSTEM_CMS_LINE_DEDUCTIBLE_SWITCH,
					"" + claimLine.getServiceDeductibleCode().get());

			AdjudicationComponent adjudicationForPayment = item.addAdjudication();
			adjudicationForPayment
					.setCategory(createCodeableConcept(TransformerConstants.CODING_SYSTEM_ADJUDICATION_CMS,
							TransformerConstants.CODED_ADJUDICATION_PAYMENT))
					.getAmount().setSystem(TransformerConstants.CODING_SYSTEM_MONEY)
					.setCode(TransformerConstants.CODING_SYSTEM_MONEY_US).setValue(claimLine.getPaymentAmount());
			addExtensionCoding(adjudicationForPayment,
					TransformerConstants.CODING_SYSTEM_CMS_LINE_PAYMENT_INDICATOR_SWITCH,
					TransformerConstants.CODING_SYSTEM_CMS_LINE_PAYMENT_INDICATOR_SWITCH,
					"" + claimLine.getPaymentCode().get());

			item.addAdjudication()
					.setCategory(createCodeableConcept(TransformerConstants.CODING_SYSTEM_ADJUDICATION_CMS,
							TransformerConstants.CODED_ADJUDICATION_BENEFICIARY_PAYMENT_AMOUNT))
					.getAmount().setSystem(TransformerConstants.CODING_SYSTEM_MONEY)
					.setCode(TransformerConstants.CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.getBeneficiaryPaymentAmount());

			item.addAdjudication()
					.setCategory(createCodeableConcept(TransformerConstants.CODING_SYSTEM_ADJUDICATION_CMS,
							TransformerConstants.CODED_ADJUDICATION_PAYMENT_B))
					.getAmount().setSystem(TransformerConstants.CODING_SYSTEM_MONEY)
					.setCode(TransformerConstants.CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.getProviderPaymentAmount());

			item.addAdjudication()
					.setCategory(createCodeableConcept(TransformerConstants.CODING_SYSTEM_ADJUDICATION_CMS,
							TransformerConstants.CODED_ADJUDICATION_DEDUCTIBLE))
					.getAmount().setSystem(TransformerConstants.CODING_SYSTEM_MONEY)
					.setCode(TransformerConstants.CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.getBeneficiaryPartBDeductAmount());

			if (claimLine.getPrimaryPayerCode().isPresent()) {
				addExtensionCoding(item, TransformerConstants.CODING_SYSTEM_PRIMARY_PAYER_CD,
						TransformerConstants.CODING_SYSTEM_PRIMARY_PAYER_CD,
						String.valueOf(claimLine.getPrimaryPayerCode().get()));
			}

			item.addAdjudication()
					.setCategory(createCodeableConcept(TransformerConstants.CODING_SYSTEM_ADJUDICATION_CMS,
							TransformerConstants.CODED_ADJUDICATION_PRIMARY_PAYER_PAID_AMOUNT))
					.getAmount().setSystem(TransformerConstants.CODING_SYSTEM_MONEY)
					.setCode(TransformerConstants.CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.getPrimaryPayerPaidAmount());

			item.addAdjudication()
					.setCategory(createCodeableConcept(TransformerConstants.CODING_SYSTEM_ADJUDICATION_CMS,
							TransformerConstants.CODED_ADJUDICATION_LINE_COINSURANCE_AMOUNT))
					.getAmount().setSystem(TransformerConstants.CODING_SYSTEM_MONEY)
					.setCode(TransformerConstants.CODING_SYSTEM_MONEY_US).setValue(claimLine.getCoinsuranceAmount());

			item.addAdjudication()
					.setCategory(createCodeableConcept(TransformerConstants.CODING_SYSTEM_ADJUDICATION_CMS,
							TransformerConstants.CODED_ADJUDICATION_SUBMITTED_CHARGE_AMOUNT))
					.getAmount().setSystem(TransformerConstants.CODING_SYSTEM_MONEY)
					.setCode(TransformerConstants.CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.getSubmittedChargeAmount());

			item.addAdjudication()
					.setCategory(createCodeableConcept(TransformerConstants.CODING_SYSTEM_ADJUDICATION_CMS,
							TransformerConstants.CODED_ADJUDICATION_ALLOWED_CHARGE))
					.getAmount().setSystem(TransformerConstants.CODING_SYSTEM_MONEY)
					.setCode(TransformerConstants.CODING_SYSTEM_MONEY_US).setValue(claimLine.getAllowedChargeAmount());

			if (claimLine.getMtusCode().isPresent()) {
				addExtensionCoding(item, TransformerConstants.CODING_SYSTEM_MTUS_CD,
						TransformerConstants.CODING_SYSTEM_MTUS_CD, String.valueOf(claimLine.getMtusCode().get()));
			}

			if (!claimLine.getMtusCount().equals(BigDecimal.ZERO)) {
				addExtensionCoding(item, TransformerConstants.CODING_SYSTEM_MTUS_COUNT,
						TransformerConstants.CODING_SYSTEM_MTUS_COUNT, String.valueOf(claimLine.getMtusCount()));
			}

			item.addAdjudication()
					.setCategory(createCodeableConcept(TransformerConstants.CODING_SYSTEM_ADJUDICATION_CMS,
							TransformerConstants.CODED_ADJUDICATION_LINE_PROCESSING_INDICATOR))
					.setReason(createCodeableConcept(TransformerConstants.CODING_SYSTEM_CMS_LINE_PROCESSING_INDICATOR,
							claimLine.getProcessingIndicatorCode().get()));

			Optional<Diagnosis> lineDiagnosis = extractDiagnosis(claimLine);
			if (lineDiagnosis.isPresent())
				addDiagnosisLink(eob, item, lineDiagnosis.get());

			if (claimLine.getNationalDrugCode().isPresent()) {
				addExtensionCoding(item, TransformerConstants.CODING_SYSTEM_NDC, TransformerConstants.CODING_SYSTEM_NDC,
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
				addExtensionCoding(item.getLocation(), TransformerConstants.CODING_SYSTEM_CLIA_LAB_NUM,
						TransformerConstants.CODING_SYSTEM_CLIA_LAB_NUM, claimLine.getCliaLabNumber().get());
			}
		}

		return eob;
	}

	/**
	 * @param subPlan
	 *            the {@link Coverage#getSubPlan()} value to match
	 * @param beneficiaryPatientId
	 *            the {@link #TransformerConstants.CODING_SYSTEM_CCW_BENE_ID} ID
	 *            value for the {@link Coverage#getBeneficiary()} value to match
	 * @return a {@link Reference} to the {@link Coverage} resource where
	 *         {@link Coverage#getPlan()} matches {@link #COVERAGE_PLAN} and the
	 *         other parameters specified also match
	 */
	private static Reference referenceCoverage(String beneficiaryPatientId, String subPlan) {
		return new Reference(String.format("Coverage?beneficiary.identifier=%s|%s&subplan=%s",
				TransformerConstants.CODING_SYSTEM_CCW_BENE_ID, beneficiaryPatientId, subPlan));
	}

	/**
	 * @param patientId
	 *            the {@link #TransformerConstants.CODING_SYSTEM_CCW_BENE_ID} ID
	 *            value for the beneficiary to match
	 * @return a {@link Reference} to the {@link Patient} resource that matches
	 *         the specified parameters
	 */
	private static Reference referencePatient(String patientId) {
		return new Reference(
				String.format("Patient?identifier=%s|%s", TransformerConstants.CODING_SYSTEM_CCW_BENE_ID, patientId));
	}

	/**
	 * @param practitionerNpi
	 *            the {@link Practitioner#getIdentifier()} value to match (where
	 *            {@link Identifier#getSystem()} is
	 *            {@value #TransformerConstants.CODING_SYSTEM_NPI_US})
	 * @return a {@link Reference} to the {@link Practitioner} resource that
	 *         matches the specified parameters
	 */
	private static Reference referencePractitioner(String practitionerNpi) {
		return createIdentifierReference(TransformerConstants.CODING_SYSTEM_NPI_US, practitionerNpi);
	}

	/**
	 * @param period
	 *            the {@link Period} to adjust
	 * @param date
	 *            the {@link LocalDate} to set the {@link Period#getStart()}
	 *            value with/to
	 */
	private static void setPeriodStart(Period period, LocalDate date) {
		period.setStart(Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()), TemporalPrecisionEnum.DAY);
	}

	/**
	 * @param period
	 *            the {@link Period} to adjust
	 * @param date
	 *            the {@link LocalDate} to set the {@link Period#getEnd()} value
	 *            with/to
	 */
	private static void setPeriodEnd(Period period, LocalDate date) {
		period.setEnd(Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()), TemporalPrecisionEnum.DAY);
	}

	/**
	 * <p>
	 * Adds an {@link Extension} to the specified {@link DomainResource}.
	 * {@link Extension#getValue()} will be set to a {@link CodeableConcept}
	 * containing a single {@link Coding}, with the specified system and code.
	 * </p>
	 * <p>
	 * Data Architecture Note: The {@link CodeableConcept} might seem extraneous
	 * -- why not just add the {@link Coding} directly to the {@link Extension}?
	 * The main reason for doing it this way is consistency: this is what FHIR
	 * seems to do everywhere.
	 * </p>
	 * 
	 * @param fhirElement
	 *            the FHIR element to add the {@link Extension} to
	 * @param extensionUrl
	 *            the {@link Extension#getUrl()} to use
	 * @param codingSystem
	 *            the {@link Coding#getSystem()} to use
	 * @param codingCode
	 *            the {@link Coding#getCode()} to use
	 */
	private static void addExtensionCoding(IBaseHasExtensions fhirElement, String extensionUrl, String codingSystem,
			String codingCode) {
		IBaseExtension<?, ?> extension = fhirElement.addExtension();
		extension.setUrl(extensionUrl);
		extension.setValue(new Coding());

		CodeableConcept codeableConcept = new CodeableConcept();
		extension.setValue(codeableConcept);

		Coding coding = codeableConcept.addCoding();
		coding.setSystem(codingSystem).setCode(codingCode);
	}

	/**
	 * @param eob
	 *            the {@link ExplanationOfBenefit} to (possibly) modify
	 * @param diagnosis
	 *            the {@link Diagnosis} to add, if it's not already present
	 * @return the {@link DiagnosisComponent#getSequence()} of the existing or
	 *         newly-added entry
	 */
	private static int addDiagnosisCode(ExplanationOfBenefit eob, Diagnosis diagnosis) {
		Optional<DiagnosisComponent> existingDiagnosis = eob.getDiagnosis().stream()
				.filter(d -> d.getDiagnosis() instanceof CodeableConcept)
				.filter(d -> diagnosis.isContainedIn((CodeableConcept) d.getDiagnosis())).findAny();
		if (existingDiagnosis.isPresent())
			return existingDiagnosis.get().getSequenceElement().getValue();

		DiagnosisComponent diagnosisComponent = new DiagnosisComponent().setSequence(eob.getDiagnosis().size() + 1);
		diagnosisComponent.setDiagnosis(diagnosis.toCodeableConcept());
		if (diagnosis.getPresentOnAdmission().isPresent()) {
			diagnosisComponent.addType(createCodeableConcept(TransformerConstants.CODING_SYSTEM_CCW_INP_POA_CD,
					diagnosis.getPresentOnAdmission().get()));
		}
		eob.getDiagnosis().add(diagnosisComponent);
		return diagnosisComponent.getSequenceElement().getValue();
	}

	/**
	 * @param eob
	 *            the {@link ExplanationOfBenefit} that the specified
	 *            {@link ItemComponent} is a child of
	 * @param item
	 *            the {@link ItemComponent} to add an
	 *            {@link ItemComponent#getDiagnosisLinkId()} entry to
	 * @param diagnosis
	 *            the {@link Diagnosis} to add a link for
	 */
	private static void addDiagnosisLink(ExplanationOfBenefit eob, ItemComponent item, Diagnosis diagnosis) {
		int diagnosisSequence = addDiagnosisCode(eob, diagnosis);
		item.addDiagnosisLinkId(diagnosisSequence);
	}

	/**
	 * Ensures that the specified {@link ExplanationOfBenefit} has the specified
	 * {@link CareTeamComponent}, and links the specified {@link ItemComponent}
	 * to that {@link CareTeamComponent} (via
	 * {@link ItemComponent#addCareTeamLinkId(int)}).
	 * 
	 * @param eob
	 *            the {@link ExplanationOfBenefit} that the
	 *            {@link CareTeamComponent} should be part of
	 * @param eobItem
	 *            the {@link ItemComponent} that should be linked to the
	 *            {@link CareTeamComponent}
	 * @param practitionerIdSystem
	 *            the {@link Identifier#getSystem()} of the practitioner to
	 *            reference in {@link CareTeamComponent#getProvider()}
	 * @param practitionerIdValue
	 *            the {@link Identifier#getValue()} of the practitioner to
	 *            reference in {@link CareTeamComponent#getProvider()}
	 * @return the {@link CareTeamComponent} that was created/linked
	 */
	private static CareTeamComponent addCareTeamPractitioner(ExplanationOfBenefit eob, ItemComponent eobItem,
			String practitionerIdSystem, String practitionerIdValue, String practitionerRole) {
		// Try to find a matching pre-existing entry.
		CareTeamComponent careTeamEntry = eob.getCareTeam().stream().filter(ctc -> ctc.getProvider().hasIdentifier())
				.filter(ctc -> practitionerIdSystem.equals(ctc.getProvider().getIdentifier().getSystem())
						&& practitionerIdValue.equals(ctc.getProvider().getIdentifier().getValue()))
				.findAny().orElse(null);

		// If no match was found, add one to the EOB.
		if (careTeamEntry == null) {
			careTeamEntry = eob.addCareTeam();
			careTeamEntry.setSequence(eob.getCareTeam().size() + 1);
			careTeamEntry.setProvider(new Reference()
					.setIdentifier(new Identifier().setSystem(practitionerIdSystem).setValue(practitionerIdValue)));
			careTeamEntry.setRole(
					createCodeableConcept(TransformerConstants.CODING_SYSTEM_CARE_TEAM_ROLE, practitionerRole));
		}

		// care team entry is at eob level so no need to create item link id
		if (eobItem == null) {
			return careTeamEntry;
		}

		// Link the EOB.item to the care team entry (if it isn't already).
		if (!eobItem.getCareTeamLinkId().contains(careTeamEntry.getSequence())) {
			eobItem.addCareTeamLinkId(careTeamEntry.getSequence());
		}

		return careTeamEntry;
	}

	/**
	 * @param localDate
	 *            the {@link LocalDate} to convert
	 * @return a {@link Date} version of the specified {@link LocalDate}
	 */
	private static Date convertToDate(LocalDate localDate) {
		/*
		 * We use the system TZ here to ensure that the date doesn't shift at
		 * all, as FHIR will just use this as an unzoned Date (I think, and if
		 * not, it's almost certainly using the same TZ as this system).
		 */
		return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
	}

	/**
	 * @param codingSystem
	 *            the {@link Coding#getSystem()} to use
	 * @param codingCode
	 *            the {@link Coding#getCode()} to use
	 * @return a {@link CodeableConcept} with the specified {@link Coding}
	 */
	private static CodeableConcept createCodeableConcept(String codingSystem, String codingCode) {
		return createCodeableConcept(codingSystem, null, codingCode);
	}

	/**
	 * @param codingSystem
	 *            the {@link Coding#getSystem()} to use
	 * @param codingVersion
	 *            the {@link Coding#getVersion()} to use
	 * @param codingCode
	 *            the {@link Coding#getCode()} to use
	 * @return a {@link CodeableConcept} with the specified {@link Coding}
	 */
	private static CodeableConcept createCodeableConcept(String codingSystem, String codingVersion, String codingCode) {
		CodeableConcept codeableConcept = new CodeableConcept();
		Coding coding = codeableConcept.addCoding().setSystem(codingSystem).setCode(codingCode);
		if (codingVersion != null)
			coding.setVersion(codingVersion);
		return codeableConcept;
	}

	/**
	 * @param identifierSystem
	 *            the {@link Identifier#getSystem()} to use in
	 *            {@link Reference#getIdentifier()}
	 * @param identifierValue
	 *            the {@link Identifier#getValue()} to use in
	 *            {@link Reference#getIdentifier()}
	 * @return a {@link Reference} with the specified {@link Identifier}
	 */
	private static Reference createIdentifierReference(String identifierSystem, String identifierValue) {
		return new Reference().setIdentifier(new Identifier().setSystem(identifierSystem).setValue(identifierValue));
	}

	/**
	 * validate the from/thru dates to ensure the from date is before or the
	 * same as the thru date
	 * 
	 * @param dateFrom
	 *            start date {@link LocalDate}
	 * @param dateThrough
	 *            through date {@link LocalDate} to verify
	 */
	private static void validatePeriodDates(LocalDate dateFrom, LocalDate dateThrough) {
		if (dateFrom == null)
			return;
		if (dateThrough == null)
			return;
		// FIXME see CBBD-236 (ETL service fails on some Hospice claims "From
		// date is after the Through Date")
		// We are seeing this scenario in production where the from date is
		// after the through date so we are just logging the error for now.
		if (dateFrom.isAfter(dateThrough))
			LOGGER.debug(String.format("Error - From Date '%s' is after the Through Date '%s'", dateFrom, dateThrough));
	}

	/**
	 * validate the <Optional>from/<Optional>thru dates to ensure the from date
	 * is before or the same as the thru date
	 * 
	 * @param <Optional>dateFrom
	 *            start date {@link <Optional>LocalDate}
	 * @param <Optional>dateThrough
	 *            through date {@link <Optional>LocalDate} to verify
	 */
	private static void validatePeriodDates(Optional<LocalDate> dateFrom, Optional<LocalDate> dateThrough) {
		if (!dateFrom.isPresent())
			return;
		if (!dateThrough.isPresent())
			return;
		validatePeriodDates(dateFrom.get(), dateThrough.get());
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
