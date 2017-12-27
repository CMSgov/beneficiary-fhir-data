package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.hl7.fhir.dstu3.model.Address;
import org.hl7.fhir.dstu3.model.DateType;
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

import com.justdavis.karl.misc.exceptions.BadCodeMonkeyException;

import gov.hhs.cms.bluebutton.data.model.rif.InpatientClaim;
import gov.hhs.cms.bluebutton.data.model.rif.InpatientClaimLine;
import gov.hhs.cms.bluebutton.server.app.stu3.providers.Diagnosis.DiagnosisLabel;

/**
 * Transforms CCW {@link InpatientClaim} instances into FHIR
 * {@link ExplanationOfBenefit} resources.
 */
final class InpatientClaimTransformer {
	/**
	 * @param claim
	 *            the CCW {@link InpatientClaim} to transform
	 * @return a FHIR {@link ExplanationOfBenefit} resource that represents the
	 *         specified {@link InpatientClaim}
	 */
	static ExplanationOfBenefit transform(Object claim) {
		if (!(claim instanceof InpatientClaim))
			throw new BadCodeMonkeyException();
		return transformClaim((InpatientClaim) claim);
	}

	/**
	 * @param claimGroup
	 *            the CCW {@link InpatientClaim} to transform
	 * @return a FHIR {@link ExplanationOfBenefit} resource that represents the
	 *         specified {@link InpatientClaim}
	 */
	private static ExplanationOfBenefit transformClaim(InpatientClaim claimGroup) {
		ExplanationOfBenefit eob = new ExplanationOfBenefit();

		eob.setId(TransformerUtils.buildEobId(ClaimType.INPATIENT, claimGroup.getClaimId()));
		eob.addIdentifier().setSystem(TransformerConstants.CODING_SYSTEM_CCW_CLAIM_ID)
				.setValue(claimGroup.getClaimId());
		eob.addIdentifier().setSystem(TransformerConstants.CODING_SYSTEM_CCW_CLAIM_GRP_ID)
				.setValue(claimGroup.getClaimGroupId().toPlainString());
		eob.getInsurance()
				.setCoverage(TransformerUtils.referenceCoverage(claimGroup.getBeneficiaryId(), MedicareSegment.PART_A));
		eob.setPatient(TransformerUtils.referencePatient(claimGroup.getBeneficiaryId()));
		eob.setStatus(ExplanationOfBenefitStatus.ACTIVE);

		eob.setType(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_SYSTEM_CCW_CLAIM_TYPE,
				claimGroup.getClaimTypeCode()));
		TransformerUtils.addExtensionCoding(eob.getType(), TransformerConstants.CODING_SYSTEM_CCW_RECORD_ID_CD,
				TransformerConstants.CODING_SYSTEM_CCW_RECORD_ID_CD,
				String.valueOf(claimGroup.getNearLineRecordIdCode()));
		eob.setStatus(ExplanationOfBenefitStatus.ACTIVE);

		TransformerUtils.validatePeriodDates(claimGroup.getDateFrom(), claimGroup.getDateThrough());
		TransformerUtils.setPeriodStart(eob.getBillablePeriod(), claimGroup.getDateFrom());
		TransformerUtils.setPeriodEnd(eob.getBillablePeriod(), claimGroup.getDateThrough());

		TransformerUtils.addExtensionCoding(eob.getBillablePeriod(), TransformerConstants.CODING_SYSTEM_QUERY_CD,
				TransformerConstants.CODING_SYSTEM_QUERY_CD, String.valueOf(claimGroup.getClaimQueryCode()));

		if (claimGroup.getClaimNonPaymentReasonCode().isPresent()) {
			TransformerUtils.addExtensionCoding(eob, TransformerConstants.CODING_SYSTEM_CCW_INP_PAYMENT_DENIAL_CD,
					TransformerConstants.CODING_SYSTEM_CCW_INP_PAYMENT_DENIAL_CD,
					claimGroup.getClaimNonPaymentReasonCode().get());
		}

		if (!claimGroup.getPatientDischargeStatusCode().isEmpty()) {
			TransformerUtils.addInformation(eob,
					TransformerUtils.createCodeableConcept(
							TransformerConstants.CODING_SYSTEM_PATIENT_DISCHARGE_STATUS_CD,
							claimGroup.getPatientDischargeStatusCode()));
		}

		eob.getPayment().setAmount((Money) new Money().setSystem(TransformerConstants.CODING_SYSTEM_MONEY_US)
				.setValue(claimGroup.getPaymentAmount()));
		eob.setTotalCost((Money) new Money().setSystem(TransformerConstants.CODING_SYSTEM_MONEY_US)
				.setValue(claimGroup.getTotalChargeAmount()));

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

		eob.addInformation().setCategory(TransformerUtils.createCodeableConcept(
				TransformerConstants.CODING_SYSTEM_ADMISSION_TYPE_CD, String.valueOf(claimGroup.getAdmissionTypeCd())));

		if (claimGroup.getSourceAdmissionCd().isPresent()) {
			eob.addInformation()
					.setCategory(TransformerUtils.createCodeableConcept(
							TransformerConstants.CODING_SYSTEM_SOURCE_ADMISSION_CD,
							String.valueOf(claimGroup.getSourceAdmissionCd().get())));
		}

		if (claimGroup.getPatientStatusCd().isPresent()) {
			TransformerUtils.addInformation(eob,
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_SYSTEM_PATIENT_STATUS_CD,
							String.valueOf(claimGroup.getPatientStatusCd().get())));
		}

		BenefitBalanceComponent benefitBalances = new BenefitBalanceComponent(
				TransformerUtils.createCodeableConcept(
						TransformerConstants.CODING_BENEFIT_BALANCE_URL, "Medical"));
		eob.getBenefitBalance().add(benefitBalances);

		if (claimGroup.getPassThruPerDiemAmount() != null) {
			BenefitComponent benefitPerDiem = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODING_CLAIM_PASS_THRU_PER_DIEM_AMT));
			benefitPerDiem.setAllowed(new Money().setSystem(TransformerConstants.CODING_SYSTEM_MONEY_US)
					.setValue(claimGroup.getPassThruPerDiemAmount()));
			benefitBalances.getFinancial().add(benefitPerDiem);
		}

		if (claimGroup.getDeductibleAmount() != null) {
			BenefitComponent benefitInpatientDeductible = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODING_BENEFIT_DEDUCTIBLE_AMT_URL));
			benefitInpatientDeductible.setAllowed(new Money().setSystem(TransformerConstants.CODING_SYSTEM_MONEY_US)
					.setValue(claimGroup.getDeductibleAmount()));
			benefitBalances.getFinancial().add(benefitInpatientDeductible);
		}

		if (claimGroup.getPrimaryPayerPaidAmount() != null) {
			BenefitComponent benefitInpatientNchPrimaryPayerAmt = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODED_ADJUDICATION_PRIMARY_PAYER_PAID_AMOUNT));
			benefitInpatientNchPrimaryPayerAmt
					.setAllowed(new Money().setSystem(TransformerConstants.CODING_SYSTEM_MONEY_US)
							.setValue(claimGroup.getPrimaryPayerPaidAmount()));
			benefitBalances.getFinancial().add(benefitInpatientNchPrimaryPayerAmt);
		}

		if (claimGroup.getPartACoinsuranceLiabilityAmount() != null) {
			BenefitComponent benefitPartACoinsuranceLiabilityAmt = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODING_NCH_BENEFIT_COIN_AMT_URL));
			benefitPartACoinsuranceLiabilityAmt
					.setAllowed(new Money().setSystem(TransformerConstants.CODING_SYSTEM_MONEY_US)
							.setValue(claimGroup.getPartACoinsuranceLiabilityAmount()));
			benefitBalances.getFinancial().add(benefitPartACoinsuranceLiabilityAmt);
		}

		if (claimGroup.getBloodDeductibleLiabilityAmount() != null) {
			BenefitComponent benefitInpatientNchPrimaryPayerAmt = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODING_NCH_BENEFIT_BLOOD_DED_AMT_URL));
			benefitInpatientNchPrimaryPayerAmt
					.setAllowed(new Money().setSystem(TransformerConstants.CODING_SYSTEM_MONEY_US)
							.setValue(claimGroup.getBloodDeductibleLiabilityAmount()));
			benefitBalances.getFinancial().add(benefitInpatientNchPrimaryPayerAmt);
		}

		if (claimGroup.getProfessionalComponentCharge() != null) {
			BenefitComponent benefitProfessionComponentAmt = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODING_NCH_PROFFESIONAL_CHARGE_URL));
			benefitProfessionComponentAmt.setAllowed(new Money().setSystem(TransformerConstants.CODING_SYSTEM_MONEY_US)
					.setValue(claimGroup.getProfessionalComponentCharge()));
			benefitBalances.getFinancial().add(benefitProfessionComponentAmt);
		}

		if (claimGroup.getNoncoveredCharge() != null) {
			BenefitComponent benefitNonCoveredChangeAmt = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODING_NCH_INPATIENT_NONCOVERED_CHARGE_URL));
			benefitNonCoveredChangeAmt.setAllowed(new Money().setSystem(TransformerConstants.CODING_SYSTEM_MONEY_US)
					.setValue(claimGroup.getNoncoveredCharge()));
			benefitBalances.getFinancial().add(benefitNonCoveredChangeAmt);
		}

		if (claimGroup.getTotalDeductionAmount() != null) {
			BenefitComponent benefitTotalChangeAmt = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODING_NCH_INPATIENT_TOTAL_AMT_URL));
			benefitTotalChangeAmt.setAllowed(new Money().setSystem(TransformerConstants.CODING_SYSTEM_MONEY_US)
					.setValue(claimGroup.getTotalDeductionAmount()));
			benefitBalances.getFinancial().add(benefitTotalChangeAmt);
		}

		if (claimGroup.getClaimTotalPPSCapitalAmount() != null) {
			BenefitComponent claimTotalPPSAmt = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODING_CLAIM_TOTAL_PPS_CAPITAL_AMT_URL));
			claimTotalPPSAmt.setAllowed(new Money().setSystem(TransformerConstants.CODING_SYSTEM_MONEY_US)
					.setValue(claimGroup.getClaimTotalPPSCapitalAmount().get()));
			benefitBalances.getFinancial().add(claimTotalPPSAmt);
		}

		if (claimGroup.getClaimPPSCapitalFSPAmount() != null) {
			BenefitComponent claimPPSCapitalFSPAmt = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODING_CLAIM_PPS_CAPITAL_FEDERAL_PORTION_AMT_URL));
			claimPPSCapitalFSPAmt.setAllowed(new Money().setSystem(TransformerConstants.CODING_SYSTEM_MONEY_US)
					.setValue(claimGroup.getClaimPPSCapitalFSPAmount().get()));
			benefitBalances.getFinancial().add(claimPPSCapitalFSPAmt);
		}

		if (claimGroup.getClaimPPSCapitalOutlierAmount() != null) {
			BenefitComponent claimPPSCapitalOutlierAmount = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODING_CLAIM_PPS_CAPITAL_OUTLIER_AMT_URL));
			claimPPSCapitalOutlierAmount.setAllowed(new Money().setSystem(TransformerConstants.CODING_SYSTEM_MONEY_US)
					.setValue(claimGroup.getClaimPPSCapitalOutlierAmount().get()));
			benefitBalances.getFinancial().add(claimPPSCapitalOutlierAmount);
		}

		if (claimGroup.getClaimPPSCapitalDisproportionateShareAmt() != null) {
			BenefitComponent claimPPSCapitalDisproportionateShareAmt = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODING_CLAIM_PPS_CAPITAL_DISPROPORTIONAL_SHARE_AMT_URL));
			claimPPSCapitalDisproportionateShareAmt
					.setAllowed(new Money().setSystem(TransformerConstants.CODING_SYSTEM_MONEY_US)
							.setValue(claimGroup.getClaimPPSCapitalDisproportionateShareAmt().get()));
			benefitBalances.getFinancial().add(claimPPSCapitalDisproportionateShareAmt);
		}

		if (claimGroup.getClaimPPSCapitalIMEAmount() != null) {
			BenefitComponent claimPPSCapitalIMEAmount = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODING_CLAIM_PPS_CAPITAL_INDIRECT_MEDICAL_EDU_AMT_URL));
			claimPPSCapitalIMEAmount.setAllowed(new Money().setSystem(TransformerConstants.CODING_SYSTEM_MONEY_US)
					.setValue(claimGroup.getClaimPPSCapitalIMEAmount().get()));
			benefitBalances.getFinancial().add(claimPPSCapitalIMEAmount);
		}

		if (claimGroup.getClaimPPSCapitalExceptionAmount() != null) {
			BenefitComponent claimPPSCapitalExceptionAmount = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODING_CLAIM_PPS_CAPITAL_EXCEPTION_AMT_URL));
			claimPPSCapitalExceptionAmount.setAllowed(new Money().setSystem(TransformerConstants.CODING_SYSTEM_MONEY_US)
					.setValue(claimGroup.getClaimPPSCapitalExceptionAmount().get()));
			benefitBalances.getFinancial().add(claimPPSCapitalExceptionAmount);
		}

		if (claimGroup.getClaimPPSOldCapitalHoldHarmlessAmount() != null) {
			BenefitComponent claimPPSOldCapitalHoldHarmlessAmount = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODING_CLAIM_PPS_OLD_CAPITAL_HOLD_HARMLESS_AMT_URL));
			claimPPSOldCapitalHoldHarmlessAmount
					.setAllowed(new Money().setSystem(TransformerConstants.CODING_SYSTEM_MONEY_US)
							.setValue(claimGroup.getClaimPPSOldCapitalHoldHarmlessAmount().get()));
			benefitBalances.getFinancial().add(claimPPSOldCapitalHoldHarmlessAmount);
		}

		BenefitComponent utilizationDayCount = new BenefitComponent(TransformerUtils.createCodeableConcept(
				TransformerConstants.BENEFIT_BALANCE_TYPE, TransformerConstants.CODING_SYSTEM_UTILIZATION_DAY_COUNT));
		utilizationDayCount.setUsed(new UnsignedIntType(claimGroup.getUtilizationDayCount().intValue()));
		benefitBalances.getFinancial().add(utilizationDayCount);

		BenefitComponent coinsuranceDayCount = new BenefitComponent(TransformerUtils.createCodeableConcept(
				TransformerConstants.BENEFIT_BALANCE_TYPE, TransformerConstants.CODING_SYSTEM_COINSURANCE_DAY_COUNT));
		coinsuranceDayCount.setUsed(new UnsignedIntType(claimGroup.getCoinsuranceDayCount().intValue()));
		benefitBalances.getFinancial().add(coinsuranceDayCount);

		BenefitComponent nonUtilizationDayCount = new BenefitComponent(
				TransformerUtils.createCodeableConcept(TransformerConstants.BENEFIT_BALANCE_TYPE,
						TransformerConstants.CODING_SYSTEM_NON_UTILIZATION_DAY_COUNT));
		nonUtilizationDayCount.setAllowed(new UnsignedIntType(claimGroup.getNonUtilizationDayCount().intValue()));
		benefitBalances.getFinancial().add(nonUtilizationDayCount);

		BenefitComponent bloodPintsFurnishedQty = new BenefitComponent(
				TransformerUtils.createCodeableConcept(TransformerConstants.BENEFIT_BALANCE_TYPE,
						TransformerConstants.CODING_SYSTEM_BLOOD_PINTS_FURNISHED_QTY));
		bloodPintsFurnishedQty.setUsed(new UnsignedIntType(claimGroup.getBloodPintsFurnishedQty().intValue()));
		benefitBalances.getFinancial().add(bloodPintsFurnishedQty);

		if (claimGroup.getNoncoveredStayFromDate().isPresent()
				&& claimGroup.getNoncoveredStayThroughDate().isPresent()) {
			TransformerUtils.validatePeriodDates(claimGroup.getNoncoveredStayFromDate(),
					claimGroup.getNoncoveredStayThroughDate());
			eob.addInformation()
					.setCategory(TransformerUtils.createCodeableConcept(TransformerConstants.BENEFIT_COVERAGE_DATE,
							TransformerConstants.CODING_SYSTEM_NONCOVERED_STAY_DATE))
					.setTiming(new Period()
							.setStart(TransformerUtils.convertToDate((claimGroup.getNoncoveredStayFromDate().get())),
									TemporalPrecisionEnum.DAY)
							.setEnd(TransformerUtils.convertToDate((claimGroup.getNoncoveredStayThroughDate().get())),
									TemporalPrecisionEnum.DAY));
		}

		if (claimGroup.getCoveredCareThoughDate().isPresent()) {
			eob.addInformation()
					.setCategory(TransformerUtils.createCodeableConcept(TransformerConstants.BENEFIT_COVERAGE_DATE,
							TransformerConstants.CODING_SYSTEM_COVERED_CARE_DATE))
					.setTiming(
							new DateType(TransformerUtils.convertToDate(claimGroup.getCoveredCareThoughDate().get())));
		}

		if (claimGroup.getMedicareBenefitsExhaustedDate().isPresent()) {
			eob.addInformation()
					.setCategory(TransformerUtils.createCodeableConcept(TransformerConstants.BENEFIT_COVERAGE_DATE,
							TransformerConstants.CODING_SYSTEM_BENEFITS_EXHAUSTED_DATE))
					.setTiming(new DateType(
							TransformerUtils.convertToDate(claimGroup.getMedicareBenefitsExhaustedDate().get())));
		}

		if (claimGroup.getDiagnosisRelatedGroupCd().isPresent()) {
			eob.addInformation()
					.setCategory(TransformerUtils.createCodeableConcept(
							TransformerConstants.CODING_SYSTEM_DIAGNOSIS_RELATED_GROUP_CD,
							claimGroup.getDiagnosisRelatedGroupCd().get()));
		}

		if (claimGroup.getDrgOutlierApprovedPaymentAmount() != null) {
			BenefitComponent nchDrugOutlierApprovedPaymentAmount = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODING_NCH_DRUG_OUTLIER_APPROVED_PAYMENT_AMT_URL));
			nchDrugOutlierApprovedPaymentAmount
					.setAllowed(new Money().setSystem(TransformerConstants.CODING_SYSTEM_MONEY_US)
							.setValue(claimGroup.getDrgOutlierApprovedPaymentAmount().get()));
			benefitBalances.getFinancial().add(nchDrugOutlierApprovedPaymentAmount);
		}

		if (claimGroup.getOrganizationNpi().isPresent()) {
			eob.setOrganization(TransformerUtils.createIdentifierReference(TransformerConstants.CODING_SYSTEM_NPI_US,
					claimGroup.getOrganizationNpi().get()));
			eob.setFacility(TransformerUtils.createIdentifierReference(TransformerConstants.CODING_SYSTEM_NPI_US,
					claimGroup.getOrganizationNpi().get()));
			TransformerUtils.addExtensionCoding(eob.getFacility(),
					TransformerConstants.CODING_SYSTEM_CCW_FACILITY_TYPE_CD,
					TransformerConstants.CODING_SYSTEM_CCW_FACILITY_TYPE_CD,
					String.valueOf(claimGroup.getClaimFacilityTypeCode()));
		}

		TransformerUtils.addExtensionCoding(eob.getType(),
				TransformerConstants.CODING_SYSTEM_CCW_CLAIM_SERVICE_CLASSIFICATION_TYPE_CD,
				TransformerConstants.CODING_SYSTEM_CCW_CLAIM_SERVICE_CLASSIFICATION_TYPE_CD,
				String.valueOf(claimGroup.getClaimServiceClassificationTypeCode()));

		TransformerUtils.addInformation(eob, TransformerUtils.createCodeableConcept(
				TransformerConstants.CODING_SYSTEM_FREQUENCY_CD, String.valueOf(claimGroup.getClaimFrequencyCode())));

		if (claimGroup.getClaimPrimaryPayerCode().isPresent()) {
			TransformerUtils.addInformation(eob,
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_SYSTEM_PRIMARY_PAYER_CD,
							String.valueOf(claimGroup.getClaimPrimaryPayerCode().get())));
		}

		if (claimGroup.getAttendingPhysicianNpi().isPresent()) {
			TransformerUtils.addCareTeamPractitioner(eob, null, TransformerConstants.CODING_SYSTEM_NPI_US,
					claimGroup.getAttendingPhysicianNpi().get(), TransformerConstants.CARE_TEAM_ROLE_PRIMARY);
		}

		if (claimGroup.getOperatingPhysicianNpi().isPresent()) {
			TransformerUtils.addCareTeamPractitioner(eob, null, TransformerConstants.CODING_SYSTEM_NPI_US,
					claimGroup.getOperatingPhysicianNpi().get(), TransformerConstants.CARE_TEAM_ROLE_ASSISTING);
		}

		if (claimGroup.getOtherPhysicianNpi().isPresent()) {
			TransformerUtils.addCareTeamPractitioner(eob, null, TransformerConstants.CODING_SYSTEM_NPI_US,
					claimGroup.getOtherPhysicianNpi().get(), TransformerConstants.CARE_TEAM_ROLE_OTHER);
		}

		if (claimGroup.getMcoPaidSw().isPresent()) {
			TransformerUtils.addInformation(eob, TransformerUtils.createCodeableConcept(
					TransformerConstants.CODING_SYSTEM_MCO_PAID_CD, String.valueOf(claimGroup.getMcoPaidSw().get())));
		}

		for (Diagnosis diagnosis : extractDiagnoses(claimGroup))
			TransformerUtils.addDiagnosisCode(eob, diagnosis);

		for (CCWProcedure procedure : extractCCWProcedures(claimGroup))
			TransformerUtils.addProcedureCode(eob, procedure);

		for (InpatientClaimLine claimLine : claimGroup.getLines()) {
			ItemComponent item = eob.addItem();
			item.setSequence(claimLine.getLineNumber().intValue());

			TransformerUtils.addExtensionCoding(item, TransformerConstants.CODING_SYSTEM_FHIR_EOB_ITEM_TYPE,
					TransformerConstants.CODING_SYSTEM_FHIR_EOB_ITEM_TYPE,
					TransformerConstants.CODED_EOB_ITEM_TYPE_CLINICAL_SERVICES_AND_PRODUCTS);

			item.setRevenue(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_SYSTEM_REVENUE_CENTER,
					claimLine.getRevenueCenter()));

			if (claimLine.getHcpcsCode().isPresent()) {
				item.setService(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_SYSTEM_HCPCS,
						claimLine.getHcpcsCode().get()));
			}

			item.setLocation(new Address().setState((claimGroup.getProviderStateCode())));

			item.addAdjudication()
					.setCategory(
							TransformerUtils.createCodeableConcept(TransformerConstants.CODING_SYSTEM_ADJUDICATION_CMS,
									TransformerConstants.CODED_ADJUDICATION_RATE_AMOUNT))
					.getAmount().setSystem(TransformerConstants.CODING_SYSTEM_MONEY)
					.setCode(TransformerConstants.CODING_SYSTEM_MONEY_US).setValue(claimLine.getRateAmount());

			item.addAdjudication()
					.setCategory(
							TransformerUtils.createCodeableConcept(TransformerConstants.CODING_SYSTEM_ADJUDICATION_CMS,
									TransformerConstants.CODED_ADJUDICATION_TOTAL_CHARGE_AMOUNT))
					.getAmount().setSystem(TransformerConstants.CODING_SYSTEM_MONEY)
					.setCode(TransformerConstants.CODING_SYSTEM_MONEY_US).setValue(claimLine.getTotalChargeAmount());

			item.addAdjudication()
					.setCategory(
							TransformerUtils.createCodeableConcept(TransformerConstants.CODING_SYSTEM_ADJUDICATION_CMS,
									TransformerConstants.CODED_ADJUDICATION_NONCOVERED_CHARGE))
					.getAmount().setSystem(TransformerConstants.CODING_SYSTEM_MONEY)
					.setCode(TransformerConstants.CODING_SYSTEM_MONEY_US)
					.setValue(claimLine.getNonCoveredChargeAmount());

			if (claimLine.getDeductibleCoinsuranceCd().isPresent()) {
				TransformerUtils.addExtensionCoding(item.getRevenue(),
						TransformerConstants.CODING_SYSTEM_DEDUCTIBLE_COINSURANCE_CD,
						TransformerConstants.CODING_SYSTEM_DEDUCTIBLE_COINSURANCE_CD,
						String.valueOf(claimLine.getDeductibleCoinsuranceCd().get()));
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
				item.addModifier(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_SYSTEM_NDC_QLFR_CD,
						claimLine.getNationalDrugCodeQualifierCode().get()));
			}

			if (claimLine.getRevenueCenterRenderingPhysicianNPI().isPresent()) {
				TransformerUtils.addCareTeamPractitioner(eob, item, TransformerConstants.CODING_SYSTEM_NPI_US,
						claimLine.getRevenueCenterRenderingPhysicianNPI().get(),
						TransformerConstants.CARE_TEAM_ROLE_PRIMARY);
			}

		}

		return eob;
	}

	/**
	 * @param claim
	 *            the {@link InpatientClaim} to extract the {@link Diagnosis}es
	 *            from
	 * @return the {@link Diagnosis}es that can be extracted from the specified
	 *         {@link InpatientClaim}
	 */
	private static List<Diagnosis> extractDiagnoses(InpatientClaim claim) {
		List<Diagnosis> diagnoses = new LinkedList<>();

		/*
		 * Seems silly, but allows the block below to be simple one-liners,
		 * rather than requiring if-blocks.
		 */
		Consumer<Optional<Diagnosis>> diagnosisAdder = d -> {
			if (d.isPresent())
				diagnoses.add(d.get());
		};

		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosisAdmittingCode(),
				claim.getDiagnosisAdmittingCodeVersion(), DiagnosisLabel.ADMITTING));
		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosisPrincipalCode(),
				claim.getDiagnosisPrincipalCodeVersion(), DiagnosisLabel.PRINCIPAL));
		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosis1Code(), claim.getDiagnosis1CodeVersion(),
				claim.getDiagnosis1PresentOnAdmissionCode()));
		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosis2Code(), claim.getDiagnosis2CodeVersion(),
				claim.getDiagnosis2PresentOnAdmissionCode()));
		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosis3Code(), claim.getDiagnosis3CodeVersion(),
				claim.getDiagnosis3PresentOnAdmissionCode()));
		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosis4Code(), claim.getDiagnosis4CodeVersion(),
				claim.getDiagnosis4PresentOnAdmissionCode()));
		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosis5Code(), claim.getDiagnosis5CodeVersion(),
				claim.getDiagnosis5PresentOnAdmissionCode()));
		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosis6Code(), claim.getDiagnosis6CodeVersion(),
				claim.getDiagnosis6PresentOnAdmissionCode()));
		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosis7Code(), claim.getDiagnosis7CodeVersion(),
				claim.getDiagnosis7PresentOnAdmissionCode()));
		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosis8Code(), claim.getDiagnosis8CodeVersion(),
				claim.getDiagnosis8PresentOnAdmissionCode()));
		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosis9Code(), claim.getDiagnosis9CodeVersion(),
				claim.getDiagnosis9PresentOnAdmissionCode()));
		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosis10Code(), claim.getDiagnosis10CodeVersion(),
				claim.getDiagnosis10PresentOnAdmissionCode()));
		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosis11Code(), claim.getDiagnosis11CodeVersion(),
				claim.getDiagnosis11PresentOnAdmissionCode()));
		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosis12Code(), claim.getDiagnosis12CodeVersion(),
				claim.getDiagnosis12PresentOnAdmissionCode()));
		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosis13Code(), claim.getDiagnosis13CodeVersion(),
				claim.getDiagnosis13PresentOnAdmissionCode()));
		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosis14Code(), claim.getDiagnosis14CodeVersion(),
				claim.getDiagnosis14PresentOnAdmissionCode()));
		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosis15Code(), claim.getDiagnosis15CodeVersion(),
				claim.getDiagnosis15PresentOnAdmissionCode()));
		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosis16Code(), claim.getDiagnosis16CodeVersion(),
				claim.getDiagnosis16PresentOnAdmissionCode()));
		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosis17Code(), claim.getDiagnosis17CodeVersion(),
				claim.getDiagnosis17PresentOnAdmissionCode()));
		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosis18Code(), claim.getDiagnosis18CodeVersion(),
				claim.getDiagnosis18PresentOnAdmissionCode()));
		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosis19Code(), claim.getDiagnosis19CodeVersion(),
				claim.getDiagnosis19PresentOnAdmissionCode()));
		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosis20Code(), claim.getDiagnosis20CodeVersion(),
				claim.getDiagnosis20PresentOnAdmissionCode()));
		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosis21Code(), claim.getDiagnosis21CodeVersion(),
				claim.getDiagnosis21PresentOnAdmissionCode()));
		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosis22Code(), claim.getDiagnosis22CodeVersion(),
				claim.getDiagnosis22PresentOnAdmissionCode()));
		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosis23Code(), claim.getDiagnosis23CodeVersion(),
				claim.getDiagnosis23PresentOnAdmissionCode()));
		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosis24Code(), claim.getDiagnosis24CodeVersion(),
				claim.getDiagnosis24PresentOnAdmissionCode()));
		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosis25Code(), claim.getDiagnosis25CodeVersion(),
				claim.getDiagnosis25PresentOnAdmissionCode()));

		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosisExternalFirstCode(),
				claim.getDiagnosisExternalFirstCodeVersion(), DiagnosisLabel.FIRSTEXTERNAL));

		diagnosisAdder
				.accept(Diagnosis.from(claim.getDiagnosisExternal1Code(), claim.getDiagnosisExternal1CodeVersion(),
						claim.getDiagnosisExternal1PresentOnAdmissionCode()));
		diagnosisAdder
				.accept(Diagnosis.from(claim.getDiagnosisExternal2Code(), claim.getDiagnosisExternal2CodeVersion(),
						claim.getDiagnosisExternal2PresentOnAdmissionCode()));
		diagnosisAdder
				.accept(Diagnosis.from(claim.getDiagnosisExternal3Code(), claim.getDiagnosisExternal3CodeVersion(),
						claim.getDiagnosisExternal3PresentOnAdmissionCode()));
		diagnosisAdder
				.accept(Diagnosis.from(claim.getDiagnosisExternal4Code(), claim.getDiagnosisExternal4CodeVersion(),
						claim.getDiagnosisExternal4PresentOnAdmissionCode()));
		diagnosisAdder
				.accept(Diagnosis.from(claim.getDiagnosisExternal5Code(), claim.getDiagnosisExternal5CodeVersion(),
						claim.getDiagnosisExternal5PresentOnAdmissionCode()));
		diagnosisAdder
				.accept(Diagnosis.from(claim.getDiagnosisExternal6Code(), claim.getDiagnosisExternal6CodeVersion(),
						claim.getDiagnosisExternal6PresentOnAdmissionCode()));
		diagnosisAdder
				.accept(Diagnosis.from(claim.getDiagnosisExternal7Code(), claim.getDiagnosisExternal7CodeVersion(),
						claim.getDiagnosisExternal7PresentOnAdmissionCode()));
		diagnosisAdder
				.accept(Diagnosis.from(claim.getDiagnosisExternal8Code(), claim.getDiagnosisExternal8CodeVersion(),
						claim.getDiagnosisExternal8PresentOnAdmissionCode()));
		diagnosisAdder
				.accept(Diagnosis.from(claim.getDiagnosisExternal9Code(), claim.getDiagnosisExternal9CodeVersion(),
						claim.getDiagnosisExternal9PresentOnAdmissionCode()));
		diagnosisAdder
				.accept(Diagnosis.from(claim.getDiagnosisExternal10Code(), claim.getDiagnosisExternal10CodeVersion(),
						claim.getDiagnosisExternal10PresentOnAdmissionCode()));
		diagnosisAdder
				.accept(Diagnosis.from(claim.getDiagnosisExternal11Code(), claim.getDiagnosisExternal11CodeVersion(),
						claim.getDiagnosisExternal11PresentOnAdmissionCode()));
		diagnosisAdder
				.accept(Diagnosis.from(claim.getDiagnosisExternal12Code(), claim.getDiagnosisExternal12CodeVersion(),
						claim.getDiagnosisExternal12PresentOnAdmissionCode()));

		return diagnoses;
	}

	/**
	 * @param claim
	 *            the {@link InpatientClaim} to extract the {@link CCWProcedure}es
	 *            from
	 * @return the {@link CCWProcedure}es that can be extracted from the specified
	 *         {@link InpatientClaim}
	 */
	private static List<CCWProcedure> extractCCWProcedures(InpatientClaim claim) {
		List<CCWProcedure> ccwProcedures = new LinkedList<>();

		/*
		 * Seems silly, but allows the block below to be simple one-liners,
		 * rather than requiring if-blocks.
		 */
		Consumer<Optional<CCWProcedure>> ccwProcedureAdder = p -> {
			if (p.isPresent())
				ccwProcedures.add(p.get());
		};

		
		ccwProcedureAdder.accept(CCWProcedure.from(claim.getProcedure1Code(), claim.getProcedure1CodeVersion(),
				claim.getProcedure1Date()));
		ccwProcedureAdder.accept(CCWProcedure.from(claim.getProcedure2Code(), claim.getProcedure2CodeVersion(),
				claim.getProcedure2Date()));
		ccwProcedureAdder.accept(CCWProcedure.from(claim.getProcedure3Code(), claim.getProcedure3CodeVersion(),
				claim.getProcedure3Date()));
		ccwProcedureAdder.accept(CCWProcedure.from(claim.getProcedure4Code(), claim.getProcedure4CodeVersion(),
				claim.getProcedure4Date()));
		ccwProcedureAdder.accept(CCWProcedure.from(claim.getProcedure5Code(), claim.getProcedure5CodeVersion(),
				claim.getProcedure5Date()));
		ccwProcedureAdder.accept(CCWProcedure.from(claim.getProcedure6Code(), claim.getProcedure6CodeVersion(),
				claim.getProcedure6Date()));
		ccwProcedureAdder.accept(CCWProcedure.from(claim.getProcedure7Code(), claim.getProcedure7CodeVersion(),
				claim.getProcedure7Date()));
		ccwProcedureAdder.accept(CCWProcedure.from(claim.getProcedure8Code(), claim.getProcedure8CodeVersion(),
				claim.getProcedure8Date()));
		ccwProcedureAdder.accept(CCWProcedure.from(claim.getProcedure9Code(), claim.getProcedure9CodeVersion(),
				claim.getProcedure9Date()));
		ccwProcedureAdder.accept(CCWProcedure.from(claim.getProcedure10Code(), claim.getProcedure10CodeVersion(),
				claim.getProcedure10Date()));
		ccwProcedureAdder.accept(CCWProcedure.from(claim.getProcedure11Code(), claim.getProcedure11CodeVersion(),
				claim.getProcedure11Date()));
		ccwProcedureAdder.accept(CCWProcedure.from(claim.getProcedure12Code(), claim.getProcedure12CodeVersion(),
				claim.getProcedure12Date()));
		ccwProcedureAdder.accept(CCWProcedure.from(claim.getProcedure13Code(), claim.getProcedure13CodeVersion(),
				claim.getProcedure13Date()));
		ccwProcedureAdder.accept(CCWProcedure.from(claim.getProcedure14Code(), claim.getProcedure14CodeVersion(),
				claim.getProcedure14Date()));
		ccwProcedureAdder.accept(CCWProcedure.from(claim.getProcedure15Code(), claim.getProcedure15CodeVersion(),
				claim.getProcedure15Date()));
		ccwProcedureAdder.accept(CCWProcedure.from(claim.getProcedure16Code(), claim.getProcedure16CodeVersion(),
				claim.getProcedure16Date()));
		ccwProcedureAdder.accept(CCWProcedure.from(claim.getProcedure17Code(), claim.getProcedure17CodeVersion(),
				claim.getProcedure17Date()));
		ccwProcedureAdder.accept(CCWProcedure.from(claim.getProcedure18Code(), claim.getProcedure18CodeVersion(),
				claim.getProcedure18Date()));
		ccwProcedureAdder.accept(CCWProcedure.from(claim.getProcedure19Code(), claim.getProcedure19CodeVersion(),
				claim.getProcedure19Date()));
		ccwProcedureAdder.accept(CCWProcedure.from(claim.getProcedure20Code(), claim.getProcedure20CodeVersion(),
				claim.getProcedure20Date()));
		ccwProcedureAdder.accept(CCWProcedure.from(claim.getProcedure21Code(), claim.getProcedure21CodeVersion(),
				claim.getProcedure21Date()));
		ccwProcedureAdder.accept(CCWProcedure.from(claim.getProcedure22Code(), claim.getProcedure22CodeVersion(),
				claim.getProcedure22Date()));
		ccwProcedureAdder.accept(CCWProcedure.from(claim.getProcedure23Code(), claim.getProcedure23CodeVersion(),
				claim.getProcedure23Date()));
		ccwProcedureAdder.accept(CCWProcedure.from(claim.getProcedure24Code(), claim.getProcedure24CodeVersion(),
				claim.getProcedure24Date()));
		ccwProcedureAdder.accept(CCWProcedure.from(claim.getProcedure25Code(), claim.getProcedure25CodeVersion(),
				claim.getProcedure25Date()));

		return ccwProcedures;
	}

}

