package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.util.Optional;

import org.hl7.fhir.dstu3.model.Address;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.BenefitBalanceComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.BenefitComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.dstu3.model.Money;
import org.hl7.fhir.dstu3.model.codesystems.BenefitCategory;

import com.justdavis.karl.misc.exceptions.BadCodeMonkeyException;

import gov.hhs.cms.bluebutton.data.model.rif.OutpatientClaim;
import gov.hhs.cms.bluebutton.data.model.rif.OutpatientClaimLine;

/**
 * Transforms CCW {@link OutpatientClaim} instances into FHIR
 * {@link ExplanationOfBenefit} resources.
 */
final class OutpatientClaimTransformer {
	/**
	 * @param claim
	 *            the CCW {@link OutpatientClaim} to transform
	 * @return a FHIR {@link ExplanationOfBenefit} resource that represents the
	 *         specified {@link OutpatientClaim}
	 */
	static ExplanationOfBenefit transform(Object claim) {
		if (!(claim instanceof OutpatientClaim))
			throw new BadCodeMonkeyException();
		return transformClaim((OutpatientClaim) claim);
	}

	/**
	 * @param claimGroup
	 *            the CCW {@link OutpatientClaim} to transform
	 * @return a FHIR {@link ExplanationOfBenefit} resource that represents the
	 *         specified {@link OutpatientClaim}
	 */
	private static ExplanationOfBenefit transformClaim(OutpatientClaim claimGroup) {
		ExplanationOfBenefit eob = new ExplanationOfBenefit();

		// Common group level fields between all claim types
		TransformerUtils.mapEobCommonClaimHeaderData(eob, claimGroup.getClaimId(), claimGroup.getBeneficiaryId(),
				ClaimType.OUTPATIENT, claimGroup.getClaimGroupId().toPlainString(), MedicareSegment.PART_B,
				Optional.of(claimGroup.getDateFrom()), Optional.of(claimGroup.getDateThrough()),
				Optional.of(claimGroup.getPaymentAmount()), claimGroup.getFinalAction());

		// map eob type codes into FHIR
		TransformerUtils.mapEobType(eob, ClaimType.OUTPATIENT, Optional.of(claimGroup.getNearLineRecordIdCode()), 
				Optional.of(claimGroup.getClaimTypeCode()));

		// set the provider number which is common among several claim types
		TransformerUtils.setProviderNumber(eob, claimGroup.getProviderNumber());

		BenefitBalanceComponent benefitBalances = new BenefitBalanceComponent(
				TransformerUtils.createCodeableConcept(TransformerConstants.CODING_FHIR_BENEFIT_BALANCE,
						BenefitCategory.MEDICAL.toCode()));
		eob.getBenefitBalance().add(benefitBalances);

		if (claimGroup.getProfessionalComponentCharge() != null) {
			BenefitComponent benefitProfessionComponentAmt = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_PROFFESIONAL_COMPONENT_CHARGE));
			benefitProfessionComponentAmt.setAllowed(
					new Money().setSystem(TransformerConstants.CODED_MONEY_USD)
							.setValue(claimGroup.getProfessionalComponentCharge()));
			benefitBalances.getFinancial().add(benefitProfessionComponentAmt);
		}

		if (claimGroup.getDeductibleAmount() != null) {
			BenefitComponent deductibleAmount = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_PARTB_DEDUCTIBLE));
			deductibleAmount
					.setAllowed(new Money().setSystem(TransformerConstants.CODED_MONEY_USD)
							.setValue(claimGroup.getDeductibleAmount()));
			benefitBalances.getFinancial().add(deductibleAmount);
		}

		if (claimGroup.getCoinsuranceAmount() != null) {
			BenefitComponent coninsuranceAmount = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_PARTB_COINSURANCE_AMOUNT));
			coninsuranceAmount
					.setAllowed(new Money().setSystem(TransformerConstants.CODED_MONEY_USD)
							.setValue(claimGroup.getCoinsuranceAmount()));
			benefitBalances.getFinancial().add(coninsuranceAmount);
		}

		if (claimGroup.getProviderPaymentAmount() != null) {
			BenefitComponent providerPaymentAmount = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODED_ADJUDICATION_PROVIDER_PAYMENT_AMOUNT));
			providerPaymentAmount.setAllowed(
					new Money().setSystem(TransformerConstants.CODED_MONEY_USD)
							.setValue(claimGroup.getProviderPaymentAmount()));
			benefitBalances.getFinancial().add(providerPaymentAmount);
		}

		if (claimGroup.getBeneficiaryPaymentAmount() != null) {
			BenefitComponent beneficiaryPaymentAmount = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_BENE_PAYMENT));
			beneficiaryPaymentAmount.setAllowed(
					new Money().setSystem(TransformerConstants.CODED_MONEY_USD)
							.setValue(claimGroup.getBeneficiaryPaymentAmount()));
			benefitBalances.getFinancial().add(beneficiaryPaymentAmount);
		}

		// Common group level fields between Inpatient, Outpatient and SNF
		TransformerUtils.mapEobCommonGroupInpOutSNF(eob, claimGroup.getBloodDeductibleLiabilityAmount(),
				claimGroup.getOperatingPhysicianNpi(), claimGroup.getOtherPhysicianNpi(),
				claimGroup.getClaimQueryCode(), claimGroup.getMcoPaidSw());

		// Common group level fields between Inpatient, Outpatient Hospice, HHA and SNF
		TransformerUtils.mapEobCommonGroupInpOutHHAHospiceSNF(eob, claimGroup.getOrganizationNpi(),
				claimGroup.getClaimFacilityTypeCode(), claimGroup.getClaimFrequencyCode(),
				claimGroup.getClaimNonPaymentReasonCode(), claimGroup.getPatientDischargeStatusCode().get(),
				claimGroup.getClaimServiceClassificationTypeCode(), claimGroup.getClaimPrimaryPayerCode(),
				claimGroup.getAttendingPhysicianNpi(), claimGroup.getTotalChargeAmount(),
				claimGroup.getPrimaryPayerPaidAmount(), claimGroup.getFiscalIntermediaryNumber());

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

		for (Diagnosis diagnosis : TransformerUtils.extractDiagnoses13Thru25(claimGroup.getDiagnosis13Code(),
				claimGroup.getDiagnosis13CodeVersion(), claimGroup.getDiagnosis14Code(),
				claimGroup.getDiagnosis14CodeVersion(), claimGroup.getDiagnosis15Code(),
				claimGroup.getDiagnosis15CodeVersion(), claimGroup.getDiagnosis16Code(),
				claimGroup.getDiagnosis16CodeVersion(), claimGroup.getDiagnosis17Code(),
				claimGroup.getDiagnosis17CodeVersion(), claimGroup.getDiagnosis18Code(),
				claimGroup.getDiagnosis18CodeVersion(), claimGroup.getDiagnosis19Code(),
				claimGroup.getDiagnosis19CodeVersion(), claimGroup.getDiagnosis20Code(),
				claimGroup.getDiagnosis20CodeVersion(), claimGroup.getDiagnosis21Code(),
				claimGroup.getDiagnosis21CodeVersion(), claimGroup.getDiagnosis22Code(),
				claimGroup.getDiagnosis22CodeVersion(), claimGroup.getDiagnosis23Code(),
				claimGroup.getDiagnosis23CodeVersion(), claimGroup.getDiagnosis24Code(),
				claimGroup.getDiagnosis24CodeVersion(), claimGroup.getDiagnosis25Code(),
				claimGroup.getDiagnosis25CodeVersion()))
			TransformerUtils.addDiagnosisCode(eob, diagnosis);

		for (Diagnosis diagnosis : TransformerUtils.extractExternalDiagnoses1Thru12(
				claimGroup.getDiagnosisExternalFirstCode(), claimGroup.getDiagnosisExternalFirstCodeVersion(),
				claimGroup.getDiagnosisExternal1Code(), claimGroup.getDiagnosisExternal1CodeVersion(),
				claimGroup.getDiagnosisExternal2Code(), claimGroup.getDiagnosisExternal2CodeVersion(),
				claimGroup.getDiagnosisExternal3Code(), claimGroup.getDiagnosisExternal3CodeVersion(),
				claimGroup.getDiagnosisExternal4Code(), claimGroup.getDiagnosisExternal4CodeVersion(),
				claimGroup.getDiagnosisExternal5Code(), claimGroup.getDiagnosisExternal5CodeVersion(),
				claimGroup.getDiagnosisExternal6Code(), claimGroup.getDiagnosisExternal6CodeVersion(),
				claimGroup.getDiagnosisExternal7Code(), claimGroup.getDiagnosisExternal7CodeVersion(),
				claimGroup.getDiagnosisExternal8Code(), claimGroup.getDiagnosisExternal8CodeVersion(),
				claimGroup.getDiagnosisExternal9Code(), claimGroup.getDiagnosisExternal9CodeVersion(),
				claimGroup.getDiagnosisExternal10Code(), claimGroup.getDiagnosisExternal10CodeVersion(),
				claimGroup.getDiagnosisExternal11Code(), claimGroup.getDiagnosisExternal11CodeVersion(),
				claimGroup.getDiagnosisExternal12Code(), claimGroup.getDiagnosisExternal12CodeVersion()))
			TransformerUtils.addDiagnosisCode(eob, diagnosis);

		if (claimGroup.getDiagnosisAdmission1Code().isPresent())
			TransformerUtils.addDiagnosisCode(eob, Diagnosis
					.from(claimGroup.getDiagnosisAdmission1Code(), claimGroup.getDiagnosisAdmission1CodeVersion())
					.get());
		if (claimGroup.getDiagnosisAdmission2Code().isPresent())
			TransformerUtils.addDiagnosisCode(eob, Diagnosis
					.from(claimGroup.getDiagnosisAdmission2Code(), claimGroup.getDiagnosisAdmission2CodeVersion())
					.get());

		if (claimGroup.getDiagnosisAdmission3Code().isPresent())
			TransformerUtils.addDiagnosisCode(eob, Diagnosis
					.from(claimGroup.getDiagnosisAdmission2Code(), claimGroup.getDiagnosisAdmission3CodeVersion())
					.get());

		for (CCWProcedure procedure : TransformerUtils.extractCCWProcedures(claimGroup.getProcedure1Code(),
				claimGroup.getProcedure1CodeVersion(), claimGroup.getProcedure1Date(), claimGroup.getProcedure2Code(),
				claimGroup.getProcedure2CodeVersion(), claimGroup.getProcedure2Date(), claimGroup.getProcedure3Code(),
				claimGroup.getProcedure3CodeVersion(), claimGroup.getProcedure3Date(), claimGroup.getProcedure4Code(),
				claimGroup.getProcedure4CodeVersion(), claimGroup.getProcedure4Date(), claimGroup.getProcedure5Code(),
				claimGroup.getProcedure5CodeVersion(), claimGroup.getProcedure5Date(), claimGroup.getProcedure6Code(),
				claimGroup.getProcedure6CodeVersion(), claimGroup.getProcedure6Date(), claimGroup.getProcedure7Code(),
				claimGroup.getProcedure7CodeVersion(), claimGroup.getProcedure7Date(), claimGroup.getProcedure8Code(),
				claimGroup.getProcedure8CodeVersion(), claimGroup.getProcedure8Date(), claimGroup.getProcedure9Code(),
				claimGroup.getProcedure9CodeVersion(), claimGroup.getProcedure9Date(), claimGroup.getProcedure10Code(),
				claimGroup.getProcedure10CodeVersion(), claimGroup.getProcedure10Date(),
				claimGroup.getProcedure11Code(), claimGroup.getProcedure11CodeVersion(),
				claimGroup.getProcedure11Date(), claimGroup.getProcedure12Code(),
				claimGroup.getProcedure12CodeVersion(), claimGroup.getProcedure12Date(),
				claimGroup.getProcedure13Code(), claimGroup.getProcedure13CodeVersion(),
				claimGroup.getProcedure13Date(), claimGroup.getProcedure14Code(),
				claimGroup.getProcedure14CodeVersion(), claimGroup.getProcedure14Date(),
				claimGroup.getProcedure15Code(), claimGroup.getProcedure15CodeVersion(),
				claimGroup.getProcedure15Date(), claimGroup.getProcedure16Code(),
				claimGroup.getProcedure16CodeVersion(), claimGroup.getProcedure16Date(),
				claimGroup.getProcedure17Code(), claimGroup.getProcedure17CodeVersion(),
				claimGroup.getProcedure17Date(), claimGroup.getProcedure18Code(),
				claimGroup.getProcedure18CodeVersion(), claimGroup.getProcedure18Date(),
				claimGroup.getProcedure19Code(), claimGroup.getProcedure19CodeVersion(),
				claimGroup.getProcedure19Date(), claimGroup.getProcedure20Code(),
				claimGroup.getProcedure20CodeVersion(), claimGroup.getProcedure20Date(),
				claimGroup.getProcedure21Code(), claimGroup.getProcedure21CodeVersion(),
				claimGroup.getProcedure21Date(), claimGroup.getProcedure22Code(),
				claimGroup.getProcedure22CodeVersion(), claimGroup.getProcedure22Date(),
				claimGroup.getProcedure23Code(), claimGroup.getProcedure23CodeVersion(),
				claimGroup.getProcedure23Date(), claimGroup.getProcedure24Code(),
				claimGroup.getProcedure24CodeVersion(), claimGroup.getProcedure24Date(),
				claimGroup.getProcedure25Code(), claimGroup.getProcedure25CodeVersion(),
				claimGroup.getProcedure25Date()))
			TransformerUtils.addProcedureCode(eob, procedure);

		for (OutpatientClaimLine claimLine : claimGroup.getLines()) {
			ItemComponent item = eob.addItem();
			item.setSequence(claimLine.getLineNumber().intValue());

			TransformerUtils.addExtensionCoding(item, TransformerConstants.CODING_FHIR_ACT_INVOICE_GROUP,
					TransformerConstants.CODING_FHIR_ACT_INVOICE_GROUP,
					TransformerConstants.CODED_ACT_INVOICE_GROUP_CLINICAL_SERVICES_AND_PRODUCTS);

			item.setLocation(new Address().setState((claimGroup.getProviderStateCode())));

			// TODO re-map as described in CBBF-111
			/*
			 * if (claimLine.getNationalDrugCode().isPresent()) {
			 * item.setService(TransformerUtils.createCodeableConcept(TransformerConstants.
			 * CODING_NDC, claimLine.getNationalDrugCode().get())); }
			 */

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
			if (claimLine.getRevCntr2ndAnsiCd().isPresent()) {
				item.addAdjudication()
						.setCategory(
								TransformerUtils.createCodeableConcept(
										TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
										TransformerConstants.CODED_ADJUDICATION_2ND_ANSI_CD))
						.setReason(TransformerUtils.createCodeableConcept(
								TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
								claimLine.getRevCntr2ndAnsiCd().get()));
			}
			if (claimLine.getRevCntr3rdAnsiCd().isPresent()) {
				item.addAdjudication()
						.setCategory(
								TransformerUtils.createCodeableConcept(
										TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
										TransformerConstants.CODED_ADJUDICATION_3RD_ANSI_CD))
						.setReason(TransformerUtils.createCodeableConcept(
								TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
								claimLine.getRevCntr3rdAnsiCd().get()));
			}
			if (claimLine.getRevCntr4thAnsiCd().isPresent()) {
				item.addAdjudication()
						.setCategory(
								TransformerUtils.createCodeableConcept(
										TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
										TransformerConstants.CODED_ADJUDICATION_4TH_ANSI_CD))
						.setReason(TransformerUtils.createCodeableConcept(
								TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
								claimLine.getRevCntr4thAnsiCd().get()));
			}

			// set hcpcs modifier codes for the claim
			TransformerUtils.setHcpcsModifierCodes(item, claimLine.getHcpcsCode(),
					claimLine.getHcpcsInitialModifierCode(), claimLine.getHcpcsSecondModifierCode(), Optional.empty());

			item.addAdjudication()
					.setCategory(
							TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
									TransformerConstants.CODED_ADJUDICATION_BLOOD_DEDUCTIBLE))
					.getAmount().setSystem(TransformerConstants.CODING_MONEY)
					.setCode(TransformerConstants.CODED_MONEY_USD)
					.setValue(claimLine.getBloodDeductibleAmount());

			item.addAdjudication()
					.setCategory(
							TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
									TransformerConstants.CODED_ADJUDICATION_CASH_DEDUCTIBLE))
					.getAmount().setSystem(TransformerConstants.CODING_MONEY)
					.setCode(TransformerConstants.CODED_MONEY_USD)
					.setValue(claimLine.getCashDeductibleAmount());

			item.addAdjudication()
					.setCategory(
							TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
							TransformerConstants.CODED_ADJUDICATION_WAGE_ADJ_COINSURANCE_AMOUNT))
					.getAmount().setSystem(TransformerConstants.CODING_MONEY)
					.setCode(TransformerConstants.CODED_MONEY_USD)
					.setValue(claimLine.getWageAdjustedCoinsuranceAmount());

			item.addAdjudication()
					.setCategory(
							TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
							TransformerConstants.CODED_ADJUDICATION_REDUCED_COINSURANCE_AMOUNT))
					.getAmount().setSystem(TransformerConstants.CODING_MONEY)
					.setCode(TransformerConstants.CODED_MONEY_USD)
					.setValue(claimLine.getReducedCoinsuranceAmount());

			item.addAdjudication()
					.setCategory(
							TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
									TransformerConstants.CODED_ADJUDICATION_1ST_MSP_AMOUNT))
					.getAmount().setSystem(TransformerConstants.CODING_MONEY)
					.setCode(TransformerConstants.CODED_MONEY_USD)
					.setValue(claimLine.getFirstMspPaidAmount());

			item.addAdjudication()
					.setCategory(
							TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
									TransformerConstants.CODED_ADJUDICATION_2ND_MSP_AMOUNT))
					.getAmount().setSystem(TransformerConstants.CODING_MONEY)
					.setCode(TransformerConstants.CODED_MONEY_USD)
					.setValue(claimLine.getSecondMspPaidAmount());

			item.addAdjudication()
					.setCategory(
							TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
							TransformerConstants.CODED_ADJUDICATION_PROVIDER_PAYMENT_AMOUNT))
					.getAmount().setSystem(TransformerConstants.CODING_MONEY)
					.setCode(TransformerConstants.CODED_MONEY_USD)
					.setValue(claimLine.getProviderPaymentAmount());

			item.addAdjudication()
					.setCategory(
							TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
									TransformerConstants.CODED_ADJUDICATION_BENEFICIARY_PAYMENT_AMOUNT))
					.getAmount().setSystem(TransformerConstants.CODING_MONEY)
					.setCode(TransformerConstants.CODED_MONEY_USD)
					.setValue(claimLine.getBenficiaryPaymentAmount());

			item.addAdjudication()
					.setCategory(
							TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
							TransformerConstants.CODED_ADJUDICATION_PATIENT_RESPONSIBILITY_AMOUNT))
					.getAmount().setSystem(TransformerConstants.CODING_MONEY)
					.setCode(TransformerConstants.CODED_MONEY_USD)
					.setValue(claimLine.getPatientResponsibilityAmount());

			item.addAdjudication()
					.setCategory(
							TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
									TransformerConstants.CODED_ADJUDICATION_PAYMENT))
					.getAmount().setSystem(TransformerConstants.CODING_MONEY)
					.setCode(TransformerConstants.CODED_MONEY_USD)
					.setValue(claimLine.getPaymentAmount());

			// Common item level fields between Inpatient, Outpatient, HHA, Hospice and SNF
			TransformerUtils.mapEobCommonItemRevenue(item, eob, claimLine.getRevenueCenterCode(),
					claimLine.getRateAmount(),
					claimLine.getTotalChargeAmount(), claimLine.getNonCoveredChargeAmount(), claimLine.getUnitCount(),
					claimLine.getNationalDrugCodeQuantity(), claimLine.getNationalDrugCodeQualifierCode(),
					claimLine.getRevenueCenterRenderingPhysicianNPI());
		}
		return eob;
	}

}

