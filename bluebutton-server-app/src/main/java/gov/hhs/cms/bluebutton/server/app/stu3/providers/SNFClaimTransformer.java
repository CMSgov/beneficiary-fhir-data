package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.math.BigDecimal;
import java.util.Optional;

import org.hl7.fhir.dstu3.model.Address;
import org.hl7.fhir.dstu3.model.DateType;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.BenefitBalanceComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.BenefitComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ExplanationOfBenefitStatus;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.dstu3.model.Money;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.TemporalPrecisionEnum;
import org.hl7.fhir.dstu3.model.UnsignedIntType;
import org.hl7.fhir.dstu3.model.codesystems.BenefitCategory;

import com.justdavis.karl.misc.exceptions.BadCodeMonkeyException;

import gov.hhs.cms.bluebutton.data.model.rif.SNFClaim;
import gov.hhs.cms.bluebutton.data.model.rif.SNFClaimLine;
import gov.hhs.cms.bluebutton.server.app.stu3.providers.Diagnosis.DiagnosisLabel;

/**
 * Transforms CCW {@link SNFClaim} instances into FHIR
 * {@link ExplanationOfBenefit} resources.
 */
final class SNFClaimTransformer {
	/**
	 * @param claim
	 *            the CCW {@link SNFClaim} to transform
	 * @return a FHIR {@link ExplanationOfBenefit} resource that represents the
	 *         specified {@link SNFClaim}
	 */
	static ExplanationOfBenefit transform(Object claim) {
		if (!(claim instanceof SNFClaim))
			throw new BadCodeMonkeyException();
		return transformClaim((SNFClaim) claim);
	}

	/**
	 * @param claimGroup
	 *            the CCW {@link SNFClaim} to transform
	 * @return a FHIR {@link ExplanationOfBenefit} resource that represents the
	 *         specified {@link SNFClaim}
	 */
	private static ExplanationOfBenefit transformClaim(SNFClaim claimGroup) {
		ExplanationOfBenefit eob = new ExplanationOfBenefit();

		eob.setId(TransformerUtils.buildEobId(ClaimType.SNF, claimGroup.getClaimId()));
		eob.addIdentifier().setSystem(TransformerConstants.CODING_CCW_CLAIM_ID)
				.setValue(claimGroup.getClaimId());
		eob.addIdentifier().setSystem(TransformerConstants.CODING_CCW_CLAIM_GROUP_ID)
				.setValue(claimGroup.getClaimGroupId().toPlainString());

		// map eob type codes into FHIR
		TransformerUtils.mapEobType(eob, ClaimType.SNF, Optional.of(claimGroup.getNearLineRecordIdCode()), 
				Optional.of(claimGroup.getClaimTypeCode()));
		
		eob.getInsurance()
				.setCoverage(TransformerUtils.referenceCoverage(claimGroup.getBeneficiaryId(), MedicareSegment.PART_A));
		eob.setPatient(TransformerUtils.referencePatient(claimGroup.getBeneficiaryId()));
		eob.setStatus(ExplanationOfBenefitStatus.ACTIVE);

		TransformerUtils.validatePeriodDates(claimGroup.getDateFrom(), claimGroup.getDateThrough());
		TransformerUtils.setPeriodStart(eob.getBillablePeriod(), claimGroup.getDateFrom());
		TransformerUtils.setPeriodEnd(eob.getBillablePeriod(), claimGroup.getDateThrough());

		// set the provider number which is common among several claim types
		TransformerUtils.setProviderNumber(eob, claimGroup.getProviderNumber());

		eob.getPayment()
				.setAmount((Money) new Money().setSystem(TransformerConstants.CODED_MONEY_USD)
						.setValue(claimGroup.getPaymentAmount()));

		if (claimGroup.getClaimAdmissionDate().isPresent() || claimGroup.getBeneficiaryDischargeDate().isPresent()) {
			TransformerUtils.validatePeriodDates(claimGroup.getClaimAdmissionDate(),
					claimGroup.getBeneficiaryDischargeDate());
			Period period = new Period();
			if (claimGroup.getClaimAdmissionDate().isPresent()) {
				period.setStart(TransformerUtils.convertToDate(claimGroup.getClaimAdmissionDate().get()),
						TemporalPrecisionEnum.DAY);
			}
			if (claimGroup.getBeneficiaryDischargeDate().isPresent()) {
				period.setEnd(TransformerUtils.convertToDate(claimGroup.getBeneficiaryDischargeDate().get()),
						TemporalPrecisionEnum.DAY);
			}
			eob.setHospitalization(period);
		}

		eob.addInformation().setCategory(
				TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_ADMISSION_TYPE,
						String.valueOf(claimGroup.getAdmissionTypeCd())));

		if (claimGroup.getSourceAdmissionCd().isPresent()) {
			eob.addInformation().setCategory(
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CMS_SOURCE_ADMISSION,
							String.valueOf(claimGroup.getSourceAdmissionCd().get())));
		}

		BenefitBalanceComponent benefitBalances = new BenefitBalanceComponent(
				TransformerUtils.createCodeableConcept(
						TransformerConstants.CODING_FHIR_BENEFIT_BALANCE, BenefitCategory.MEDICAL.toCode()));
		eob.getBenefitBalance().add(benefitBalances);

		if (claimGroup.getPatientStatusCd().isPresent()) {
			TransformerUtils.addInformation(eob,
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_PATIENT_STATUS,
							String.valueOf(claimGroup.getPatientStatusCd().get())));
		}

		if (claimGroup.getDeductibleAmount() != null) {
			BenefitComponent benefitInpatientDeductible = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_DEDUCTIBLE));
			benefitInpatientDeductible
					.setAllowed(new Money().setSystem(TransformerConstants.CODED_MONEY_USD)
							.setValue(claimGroup.getDeductibleAmount()));
			benefitBalances.getFinancial().add(benefitInpatientDeductible);
		}

		if (claimGroup.getPartACoinsuranceLiabilityAmount() != null) {
			BenefitComponent benefitPartACoinsuranceLiabilityAmt = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_COINSURANCE_LIABILITY));
			benefitPartACoinsuranceLiabilityAmt.setAllowed(
					new Money().setSystem(TransformerConstants.CODED_MONEY_USD)
							.setValue(claimGroup.getPartACoinsuranceLiabilityAmount()));
			benefitBalances.getFinancial().add(benefitPartACoinsuranceLiabilityAmt);
		}

		if (claimGroup.getNoncoveredCharge() != null) {
			BenefitComponent benefitNonCoveredChangeAmt = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_NONCOVERED_CHARGE));
			benefitNonCoveredChangeAmt
					.setAllowed(new Money().setSystem(TransformerConstants.CODED_MONEY_USD)
							.setValue(claimGroup.getNoncoveredCharge()));
			benefitBalances.getFinancial().add(benefitNonCoveredChangeAmt);
		}

		if (claimGroup.getTotalDeductionAmount() != null) {
			BenefitComponent benefitTotalChangeAmt = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_TOTAL_DEDUCTION));
			benefitTotalChangeAmt.setAllowed(
					new Money().setSystem(TransformerConstants.CODED_MONEY_USD)
							.setValue(claimGroup.getTotalDeductionAmount()));
			benefitBalances.getFinancial().add(benefitTotalChangeAmt);
		}

		if (claimGroup.getClaimPPSCapitalFSPAmount() != null) {
			BenefitComponent claimPPSCapitalFSPAmt = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_PPS_CAPITAL_FEDRERAL_PORTION));
			claimPPSCapitalFSPAmt.setAllowed(
					new Money().setSystem(TransformerConstants.CODED_MONEY_USD)
							.setValue(claimGroup.getClaimPPSCapitalFSPAmount().get()));
			benefitBalances.getFinancial().add(claimPPSCapitalFSPAmt);
		}

		if (claimGroup.getClaimPPSCapitalOutlierAmount() != null) {
			BenefitComponent claimPPSCapitalOutlierAmount = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_PPS_CAPITAL_OUTLIER));
			claimPPSCapitalOutlierAmount.setAllowed(new Money().setSystem(TransformerConstants.CODED_MONEY_USD)
					.setValue(claimGroup.getClaimPPSCapitalOutlierAmount().get()));
			benefitBalances.getFinancial().add(claimPPSCapitalOutlierAmount);
		}

		if (claimGroup.getClaimPPSCapitalDisproportionateShareAmt() != null) {
			BenefitComponent claimPPSCapitalDisproportionateShareAmt = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_PPS_CAPITAL_DISPROPORTIONAL_SHARE));
			claimPPSCapitalDisproportionateShareAmt
					.setAllowed(new Money().setSystem(TransformerConstants.CODED_MONEY_USD)
							.setValue(claimGroup.getClaimPPSCapitalDisproportionateShareAmt().get()));
			benefitBalances.getFinancial().add(claimPPSCapitalDisproportionateShareAmt);
		}

		if (claimGroup.getClaimPPSCapitalIMEAmount() != null) {
			BenefitComponent claimPPSCapitalIMEAmount = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_PPS_CAPITAL_INDIRECT_MEDICAL_EDU));
			claimPPSCapitalIMEAmount.setAllowed(
					new Money().setSystem(TransformerConstants.CODED_MONEY_USD)
							.setValue(claimGroup.getClaimPPSCapitalIMEAmount().get()));
			benefitBalances.getFinancial().add(claimPPSCapitalIMEAmount);
		}

		if (claimGroup.getClaimPPSCapitalExceptionAmount() != null) {
			BenefitComponent claimPPSCapitalExceptionAmount = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_PPS_CAPITAL_EXCEPTION));
			claimPPSCapitalExceptionAmount.setAllowed(new Money().setSystem(TransformerConstants.CODED_MONEY_USD)
					.setValue(claimGroup.getClaimPPSCapitalExceptionAmount().get()));
			benefitBalances.getFinancial().add(claimPPSCapitalExceptionAmount);
		}

		if (claimGroup.getClaimPPSOldCapitalHoldHarmlessAmount() != null) {
			BenefitComponent claimPPSOldCapitalHoldHarmlessAmount = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_PPS_OLD_CAPITAL_HOLD_HARMLESS));
			claimPPSOldCapitalHoldHarmlessAmount
					.setAllowed(new Money().setSystem(TransformerConstants.CODED_MONEY_USD)
							.setValue(claimGroup.getClaimPPSOldCapitalHoldHarmlessAmount().get()));
			benefitBalances.getFinancial().add(claimPPSOldCapitalHoldHarmlessAmount);
		}

		BenefitComponent utilizationDayCount = new BenefitComponent(
				TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
						TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_SYSTEM_UTILIZATION_DAY_COUNT));
		utilizationDayCount.setUsed(new UnsignedIntType(claimGroup.getUtilizationDayCount().intValue()));
		benefitBalances.getFinancial().add(utilizationDayCount);

		BenefitComponent coinsuranceDayCount = new BenefitComponent(
				TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
						TransformerConstants.CODING_CCW_COINSURANCE_DAY_COUNT));
		coinsuranceDayCount.setUsed(new UnsignedIntType(claimGroup.getCoinsuranceDayCount().intValue()));
		benefitBalances.getFinancial().add(coinsuranceDayCount);

		BenefitComponent nonUtilizationDayCount = new BenefitComponent(
				TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
						TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_NON_UTILIZATION_DAY_COUNT));
		nonUtilizationDayCount.setAllowed(new UnsignedIntType(claimGroup.getNonUtilizationDayCount().intValue()));
		benefitBalances.getFinancial().add(nonUtilizationDayCount);

		BenefitComponent bloodPintsFurnishedQty = new BenefitComponent(
				TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
						TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_BLOOD_PINTS_FURNISHED));
		bloodPintsFurnishedQty.setUsed(new UnsignedIntType(claimGroup.getBloodPintsFurnishedQty().intValue()));
		benefitBalances.getFinancial().add(bloodPintsFurnishedQty);

		if (claimGroup.getQualifiedStayFromDate().isPresent() && claimGroup.getQualifiedStayThroughDate().isPresent()) {
			TransformerUtils.validatePeriodDates(claimGroup.getQualifiedStayFromDate(),
					claimGroup.getQualifiedStayThroughDate());
			eob.addInformation()
					.setCategory(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_COVERAGE_DATE,
							TransformerConstants.CODED_BENEFIT_COVERAGE_DATE_QUALIFIED))
					.setTiming(new Period()
							.setStart(TransformerUtils.convertToDate((claimGroup.getQualifiedStayFromDate().get())),
									TemporalPrecisionEnum.DAY)
							.setEnd(TransformerUtils.convertToDate((claimGroup.getQualifiedStayThroughDate().get())),
									TemporalPrecisionEnum.DAY));
		}

		if (claimGroup.getNoncoveredStayFromDate().isPresent()
				&& claimGroup.getNoncoveredStayThroughDate().isPresent()) {
			TransformerUtils.validatePeriodDates(claimGroup.getNoncoveredStayFromDate(),
					claimGroup.getNoncoveredStayThroughDate());
			eob.addInformation()
					.setCategory(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_COVERAGE_DATE,
							TransformerConstants.CODED_BENEFIT_COVERAGE_DATE_NONCOVERED))
					.setTiming(new Period()
							.setStart(TransformerUtils.convertToDate((claimGroup.getNoncoveredStayFromDate().get())),
									TemporalPrecisionEnum.DAY)
							.setEnd(TransformerUtils.convertToDate((claimGroup.getNoncoveredStayThroughDate().get())),
									TemporalPrecisionEnum.DAY));
		}

		if (claimGroup.getCoveredCareThroughDate().isPresent()) {
			eob.addInformation()
					.setCategory(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_COVERAGE_DATE,
							TransformerConstants.CODED_BENEFIT_COVERAGE_DATE_STAY))
					.setTiming(
							new DateType(TransformerUtils.convertToDate(claimGroup.getCoveredCareThroughDate().get())));
		}

		if (claimGroup.getMedicareBenefitsExhaustedDate().isPresent()) {
			eob.addInformation()
					.setCategory(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_COVERAGE_DATE,
							TransformerConstants.CODED_BENEFIT_COVERAGE_DATE_EXHAUSTED))
					.setTiming(new DateType(
							TransformerUtils.convertToDate(claimGroup.getMedicareBenefitsExhaustedDate().get())));
		}

		if (claimGroup.getDiagnosisRelatedGroupCd().isPresent()) {
			eob.addInformation().setCategory(TransformerUtils.createCodeableConcept(
					TransformerConstants.CODING_CCW_DIAGNOSIS_RELATED_GROUP,
					claimGroup.getDiagnosisRelatedGroupCd().get()));
		}

		eob.addInformation().setCategory(
				TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_ADMISSION_TYPE,
						String.valueOf(claimGroup.getAdmissionTypeCd())));

		// Common group level fields between Inpatient, Outpatient and SNF
		TransformerUtils.mapEobCommonGroupInpOutSNF(eob, claimGroup.getBloodDeductibleLiabilityAmount(),
				claimGroup.getOperatingPhysicianNpi(), claimGroup.getOtherPhysicianNpi(),
				claimGroup.getClaimQueryCode(), claimGroup.getMcoPaidSw());

		// Common group level fields between Inpatient, Outpatient Hospice, HHA and SNF
		TransformerUtils.mapEobCommonGroupInpOutHHAHospiceSNF(eob, claimGroup.getOrganizationNpi(),
				claimGroup.getClaimFacilityTypeCode(), claimGroup.getClaimFrequencyCode(),
				claimGroup.getClaimNonPaymentReasonCode(), claimGroup.getPatientDischargeStatusCode(),
				claimGroup.getClaimServiceClassificationTypeCode(), claimGroup.getClaimPrimaryPayerCode(),
				claimGroup.getAttendingPhysicianNpi(), claimGroup.getTotalChargeAmount(),
				claimGroup.getPrimaryPayerPaidAmount());

		TransformerUtils.addDiagnosisCode(eob, Diagnosis.from(claimGroup.getDiagnosisAdmittingCode(),
				claimGroup.getDiagnosisAdmittingCodeVersion(), DiagnosisLabel.ADMITTING).get());

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

		for (SNFClaimLine claimLine : claimGroup.getLines()) {
			ItemComponent item = eob.addItem();
			item.setSequence(claimLine.getLineNumber().intValue());

			TransformerUtils.addExtensionCoding(item, TransformerConstants.CODING_FHIR_ACT_INVOICE_GROUP,
					TransformerConstants.CODING_FHIR_ACT_INVOICE_GROUP,
					TransformerConstants.CODED_ACT_INVOICE_GROUP_CLINICAL_SERVICES_AND_PRODUCTS);

			item.setLocation(new Address().setState((claimGroup.getProviderStateCode())));

			if (claimLine.getHcpcsCode().isPresent()) {
				item.setService(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_HCPCS,
						claimLine.getHcpcsCode().get()));
			}

			// Common item level fields between Inpatient, Outpatient, HHA, Hospice and SNF
			TransformerUtils.mapEobCommonItemRevenue(item, eob, claimLine.getRevenueCenter(), claimLine.getRateAmount(),
					claimLine.getTotalChargeAmount(), claimLine.getNonCoveredChargeAmount(),
					BigDecimal.valueOf(claimLine.getUnitCount()),
					claimLine.getNationalDrugCodeQuantity(), claimLine.getNationalDrugCodeQualifierCode(),
					claimLine.getRevenueCenterRenderingPhysicianNPI());

			if (claimLine.getDeductibleCoinsuranceCd().isPresent()) {
				TransformerUtils.addExtensionCoding(item.getRevenue(),
						TransformerConstants.EXTENSION_CODING_CCW_DEDUCTIBLE_COINSURANCE_CODE,
						TransformerConstants.EXTENSION_CODING_CCW_DEDUCTIBLE_COINSURANCE_CODE,
						String.valueOf(claimLine.getDeductibleCoinsuranceCd().get()));
			}

		}
		return eob;
	}

}

