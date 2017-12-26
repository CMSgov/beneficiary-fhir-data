package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.hl7.fhir.dstu3.model.Address;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.BenefitBalanceComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.BenefitComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ExplanationOfBenefitStatus;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.dstu3.model.Money;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.SimpleQuantity;
import org.hl7.fhir.dstu3.model.TemporalPrecisionEnum;
import org.hl7.fhir.dstu3.model.UnsignedIntType;
import org.hl7.fhir.dstu3.model.codesystems.BenefitCategory;
import org.hl7.fhir.dstu3.model.codesystems.ClaimCareteamrole;

import com.justdavis.karl.misc.exceptions.BadCodeMonkeyException;

import gov.hhs.cms.bluebutton.data.model.rif.HHAClaim;
import gov.hhs.cms.bluebutton.data.model.rif.HHAClaimLine;
import gov.hhs.cms.bluebutton.server.app.stu3.providers.Diagnosis.DiagnosisLabel;

/**
 * Transforms CCW {@link HHAClaim} instances into FHIR
 * {@link ExplanationOfBenefit} resources.
 */
final class HHAClaimTransformer {
	/**
	 * @param claim
	 *            the CCW {@link HHAClaim} to transform
	 * @return a FHIR {@link ExplanationOfBenefit} resource that represents the
	 *         specified {@link HHAClaim}
	 */
	static ExplanationOfBenefit transform(Object claim) {
		if (!(claim instanceof HHAClaim))
			throw new BadCodeMonkeyException();
		return transformClaim((HHAClaim) claim);
	}

	/**
	 * @param claimGroup
	 *            the CCW {@link HHAClaim} to transform
	 * @return a FHIR {@link ExplanationOfBenefit} resource that represents the
	 *         specified {@link HHAClaim}
	 */
	private static ExplanationOfBenefit transformClaim(HHAClaim claimGroup) {
		ExplanationOfBenefit eob = new ExplanationOfBenefit();

		eob.setId(TransformerUtils.buildEobId(ClaimType.HHA, claimGroup.getClaimId()));
		eob.addIdentifier().setSystem(TransformerConstants.CODING_CCW_CLAIM_ID)
				.setValue(claimGroup.getClaimId());
		eob.addIdentifier().setSystem(TransformerConstants.CODING_CCW_CLAIM_GROUP_ID)
				.setValue(claimGroup.getClaimGroupId().toPlainString());

		eob.setType(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_CLAIM_TYPE,
				claimGroup.getClaimTypeCode()));
		TransformerUtils.addExtensionCoding(eob.getType(), TransformerConstants.CODING_CCW_RECORD_ID_CODE,
				TransformerConstants.CODING_CCW_RECORD_ID_CODE,
				String.valueOf(claimGroup.getNearLineRecordIdCode()));

		eob.setPatient(TransformerUtils.referencePatient(claimGroup.getBeneficiaryId()));
		eob.setStatus(ExplanationOfBenefitStatus.ACTIVE);
		eob.getInsurance()
				.setCoverage(TransformerUtils.referenceCoverage(claimGroup.getBeneficiaryId(), MedicareSegment.PART_B));

		TransformerUtils.validatePeriodDates(claimGroup.getDateFrom(), claimGroup.getDateThrough());
		TransformerUtils.setPeriodStart(eob.getBillablePeriod(), claimGroup.getDateFrom());
		TransformerUtils.setPeriodEnd(eob.getBillablePeriod(), claimGroup.getDateThrough());

		eob.setProvider(TransformerUtils.createIdentifierReference(TransformerConstants.IDENTIFIER_CMS_PROVIDER_NUMBER,
				claimGroup.getProviderNumber()));

		if (claimGroup.getClaimNonPaymentReasonCode().isPresent()) {
			TransformerUtils.addExtensionCoding(eob, TransformerConstants.EXTENSION_CODING_CCW_PAYMENT_DENIAL_REASON,
					TransformerConstants.EXTENSION_CODING_CCW_PAYMENT_DENIAL_REASON,
					claimGroup.getClaimNonPaymentReasonCode().get());
		}

		if (!claimGroup.getPatientDischargeStatusCode().isEmpty()) {
			TransformerUtils.addInformation(eob, TransformerUtils.createCodeableConcept(
					TransformerConstants.CODING_CCW_PATIENT_DISCHARGE_STATUS,
					claimGroup.getPatientDischargeStatusCode()));
		}

		eob.getPayment()
				.setAmount((Money) new Money().setSystem(TransformerConstants.CODED_MONEY_USD)
						.setValue(claimGroup.getPaymentAmount()));
		eob.setTotalCost((Money) new Money().setSystem(TransformerConstants.CODED_MONEY_USD)
				.setValue(claimGroup.getTotalChargeAmount()));

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

		if (claimGroup.getOrganizationNpi().isPresent()) {
			eob.setOrganization(TransformerUtils.createIdentifierReference(TransformerConstants.CODING_NPI_US,
					claimGroup.getOrganizationNpi().get()));
			eob.setFacility(TransformerUtils.createIdentifierReference(TransformerConstants.CODING_NPI_US,
					claimGroup.getOrganizationNpi().get()));
			TransformerUtils.addExtensionCoding(eob.getFacility(),
					TransformerConstants.EXTENSION_CODING_CCW_FACILITY_TYPE,
					TransformerConstants.EXTENSION_CODING_CCW_FACILITY_TYPE,
					String.valueOf(claimGroup.getClaimFacilityTypeCode()));
		}

		TransformerUtils.addInformation(eob,
				TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_CLAIM_FREQUENCY,
						String.valueOf(claimGroup.getClaimFrequencyCode())));

		if (claimGroup.getClaimPrimaryPayerCode().isPresent()) {
			TransformerUtils.addInformation(eob,
					TransformerUtils.createCodeableConcept(TransformerConstants.EXTENSION_CODING_PRIMARY_PAYER,
							String.valueOf(claimGroup.getClaimPrimaryPayerCode().get())));
		}

		if (claimGroup.getAttendingPhysicianNpi().isPresent()) {
			TransformerUtils.addCareTeamPractitioner(eob, null, TransformerConstants.CODING_NPI_US,
					claimGroup.getAttendingPhysicianNpi().get(), ClaimCareteamrole.PRIMARY.toCode());
		}

		TransformerUtils.addExtensionCoding(eob.getType(),
				TransformerConstants.EXTENSION_CODING_CCW_CLAIM_SERVICE_CLASSIFICATION,
				TransformerConstants.EXTENSION_CODING_CCW_CLAIM_SERVICE_CLASSIFICATION,
				String.valueOf(claimGroup.getClaimServiceClassificationTypeCode()));

		for (Diagnosis diagnosis : extractDiagnoses(claimGroup))
			TransformerUtils.addDiagnosisCode(eob, diagnosis);

		if (claimGroup.getClaimLUPACode().isPresent()) {
			TransformerUtils.addInformation(eob,
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_LOW_UTILIZATION_PAYMENT_ADJUSTMENT,
							String.valueOf(claimGroup.getClaimLUPACode().get())));
		}
		if (claimGroup.getClaimReferralCode().isPresent()) {
			TransformerUtils.addInformation(eob,
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_HHA_REFERRAL,
							String.valueOf(claimGroup.getClaimReferralCode().get())));
		}

		BenefitComponent totalVisitCount = new BenefitComponent(
				TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
						TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_VISIT_COUNT));
		totalVisitCount.setUsed(new UnsignedIntType(claimGroup.getTotalVisitCount().intValue()));
		benefitBalances.getFinancial().add(totalVisitCount);

		if (claimGroup.getCareStartDate().isPresent()) {
			eob.setHospitalization(
					new Period().setStart(TransformerUtils.convertToDate(claimGroup.getCareStartDate().get()),
							TemporalPrecisionEnum.DAY));
		}

		for (HHAClaimLine claimLine : claimGroup.getLines()) {
			ItemComponent item = eob.addItem();
			item.setSequence(claimLine.getLineNumber().intValue());

			item.setRevenue(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CMS_REVENUE_CENTER,
					claimLine.getRevenueCenterCode()));

			TransformerUtils.addExtensionCoding(item, TransformerConstants.CODING_FHIR_ACT_INVOICE_GROUP,
					TransformerConstants.CODING_FHIR_ACT_INVOICE_GROUP,
					TransformerConstants.CODED_ACT_INVOICE_GROUP_CLINICAL_SERVICES_AND_PRODUCTS);

			item.setLocation(new Address().setState((claimGroup.getProviderStateCode())));

			item.addAdjudication()
					.setCategory(
							TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
									TransformerConstants.CODED_ADJUDICATION_RATE_AMOUNT))
					.getAmount().setSystem(TransformerConstants.CODING_MONEY)
					.setCode(TransformerConstants.CODED_MONEY_USD)
					.setValue(claimLine.getRateAmount());

			if (claimLine.getRevCntr1stAnsiCd().isPresent()) {
				item.addAdjudication()
						.setCategory(
								TransformerUtils.createCodeableConcept(
										TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
										TransformerConstants.CODED_ADJUDICATION_1ST_ANSI_CD))
						.setReason(TransformerUtils.createCodeableConcept(
								TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
								claimLine.getRevCntr1stAnsiCd().get()));
			}

			if (claimLine.getHcpcsCode().isPresent()) {
				item.setService(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_HCPCS,
						claimLine.getHcpcsCode().get()));
			}

			item.addAdjudication()
					.setCategory(
							TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
									TransformerConstants.CODED_ADJUDICATION_PAYMENT))
					.getAmount().setSystem(TransformerConstants.CODING_MONEY)
					.setCode(TransformerConstants.CODED_MONEY_USD)
					.setValue(claimLine.getPaymentAmount());

			item.addAdjudication()
					.setCategory(
							TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
									TransformerConstants.CODED_ADJUDICATION_TOTAL_CHARGE_AMOUNT))
					.getAmount().setSystem(TransformerConstants.CODING_MONEY)
					.setCode(TransformerConstants.CODED_MONEY_USD)
					.setValue(claimLine.getTotalChargeAmount());

			item.addAdjudication()
					.setCategory(
							TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
									TransformerConstants.CODED_ADJUDICATION_NONCOVERED_CHARGE))
					.getAmount().setSystem(TransformerConstants.CODING_MONEY)
					.setCode(TransformerConstants.CODED_MONEY_USD)
					.setValue(claimLine.getNonCoveredChargeAmount());

			if (claimLine.getDeductibleCoinsuranceCd().isPresent()) {
				TransformerUtils.addExtensionCoding(item.getRevenue(),
						TransformerConstants.EXTENSION_CODING_CCW_DEDUCTIBLE_COINSURANCE_CODE,
						TransformerConstants.EXTENSION_CODING_CCW_DEDUCTIBLE_COINSURANCE_CODE,
						String.valueOf(claimLine.getDeductibleCoinsuranceCd().get()));
			}

			if (claimLine.getHcpcsInitialModifierCode().isPresent()) {
				item.addModifier(
						TransformerUtils.createCodeableConcept(TransformerConstants.CODING_HCPCS,
								claimLine.getHcpcsInitialModifierCode().get()));
			}

			if (claimLine.getHcpcsSecondModifierCode().isPresent()) {
				item.addModifier(
						TransformerUtils.createCodeableConcept(TransformerConstants.CODING_HCPCS,
								claimLine.getHcpcsSecondModifierCode().get()));
			}

			/*
			 * Set item quantity to Unit Count first if > 0; NDC quantity next
			 * if present; otherwise set to 0
			 */
			SimpleQuantity qty = new SimpleQuantity();
			if (!claimLine.getUnitCount().equals(new BigDecimal(0))) {
				qty.setValue(claimLine.getUnitCount());
			} else if (claimLine.getNationalDrugCodeQuantity().isPresent()) {
				qty.setValue(claimLine.getNationalDrugCodeQuantity().get());
			} else {
				qty.setValue(0);
			}
			item.setQuantity(qty);

			if (claimLine.getNationalDrugCodeQualifierCode().isPresent()) {
				item.addModifier(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_NDC_UNIT,
						claimLine.getNationalDrugCodeQualifierCode().get()));
			}

			if (claimLine.getRevenueCenterRenderingPhysicianNPI().isPresent()) {
				TransformerUtils.addCareTeamPractitioner(eob, item, TransformerConstants.CODING_NPI_US,
						claimLine.getRevenueCenterRenderingPhysicianNPI().get(),
						ClaimCareteamrole.PRIMARY.toCode());
			}

		}
		return eob;
	}

	/**
	 * @param claim
	 *            the {@link HHAClaim} to extract the {@link Diagnosis}es from
	 * @return the {@link Diagnosis}es that can be extracted from the specified
	 *         {@link HHAClaim}
	 */
	private static List<Diagnosis> extractDiagnoses(HHAClaim claim) {
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
		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosis13Code(), claim.getDiagnosis13CodeVersion()));
		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosis14Code(), claim.getDiagnosis14CodeVersion()));
		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosis15Code(), claim.getDiagnosis15CodeVersion()));
		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosis16Code(), claim.getDiagnosis16CodeVersion()));
		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosis17Code(), claim.getDiagnosis17CodeVersion()));
		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosis18Code(), claim.getDiagnosis18CodeVersion()));
		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosis19Code(), claim.getDiagnosis19CodeVersion()));
		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosis20Code(), claim.getDiagnosis20CodeVersion()));
		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosis21Code(), claim.getDiagnosis21CodeVersion()));
		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosis22Code(), claim.getDiagnosis22CodeVersion()));
		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosis23Code(), claim.getDiagnosis23CodeVersion()));
		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosis24Code(), claim.getDiagnosis24CodeVersion()));
		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosis25Code(), claim.getDiagnosis25CodeVersion()));

		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosisExternalFirstCode(),
				claim.getDiagnosisExternalFirstCodeVersion(), DiagnosisLabel.FIRSTEXTERNAL));
		diagnosisAdder
				.accept(Diagnosis.from(claim.getDiagnosisExternal1Code(), claim.getDiagnosisExternal1CodeVersion()));
		diagnosisAdder
				.accept(Diagnosis.from(claim.getDiagnosisExternal2Code(), claim.getDiagnosisExternal2CodeVersion()));
		diagnosisAdder
				.accept(Diagnosis.from(claim.getDiagnosisExternal3Code(), claim.getDiagnosisExternal3CodeVersion()));
		diagnosisAdder
				.accept(Diagnosis.from(claim.getDiagnosisExternal4Code(), claim.getDiagnosisExternal4CodeVersion()));
		diagnosisAdder
				.accept(Diagnosis.from(claim.getDiagnosisExternal5Code(), claim.getDiagnosisExternal5CodeVersion()));
		diagnosisAdder
				.accept(Diagnosis.from(claim.getDiagnosisExternal6Code(), claim.getDiagnosisExternal6CodeVersion()));
		diagnosisAdder
				.accept(Diagnosis.from(claim.getDiagnosisExternal7Code(), claim.getDiagnosisExternal7CodeVersion()));
		diagnosisAdder
				.accept(Diagnosis.from(claim.getDiagnosisExternal8Code(), claim.getDiagnosisExternal8CodeVersion()));
		diagnosisAdder
				.accept(Diagnosis.from(claim.getDiagnosisExternal9Code(), claim.getDiagnosisExternal9CodeVersion()));
		diagnosisAdder
				.accept(Diagnosis.from(claim.getDiagnosisExternal10Code(), claim.getDiagnosisExternal10CodeVersion()));
		diagnosisAdder
				.accept(Diagnosis.from(claim.getDiagnosisExternal11Code(), claim.getDiagnosisExternal11CodeVersion()));
		diagnosisAdder
				.accept(Diagnosis.from(claim.getDiagnosisExternal12Code(), claim.getDiagnosisExternal12CodeVersion()));

		return diagnoses;
	}

}
