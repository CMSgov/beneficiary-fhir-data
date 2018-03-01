package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.hl7.fhir.dstu3.model.Address;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.BenefitBalanceComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.BenefitComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.dstu3.model.Money;
import org.hl7.fhir.dstu3.model.codesystems.BenefitCategory;

import com.justdavis.karl.misc.exceptions.BadCodeMonkeyException;

import gov.hhs.cms.bluebutton.data.codebook.data.CcwCodebookVariable;
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

		// Common group level fields between all claim types
		TransformerUtils.mapEobCommonClaimHeaderData(eob, claimGroup.getClaimId(), claimGroup.getBeneficiaryId(),
				ClaimType.INPATIENT, claimGroup.getClaimGroupId().toPlainString(), MedicareSegment.PART_A,
				Optional.of(claimGroup.getDateFrom()), Optional.of(claimGroup.getDateThrough()),
				Optional.of(claimGroup.getPaymentAmount()), claimGroup.getFinalAction());

		// map eob type codes into FHIR
		TransformerUtils.mapEobType(eob, ClaimType.INPATIENT, Optional.of(claimGroup.getNearLineRecordIdCode()), 
				Optional.of(claimGroup.getClaimTypeCode()));

		// set the provider number which is common among several claim types
		TransformerUtils.setProviderNumber(eob, claimGroup.getProviderNumber());

		if (claimGroup.getPatientStatusCd().isPresent()) {
			TransformerUtils.addInformation(eob, TransformerUtils.createCodeableConcept(eob,
					CcwCodebookVariable.NCH_PTNT_STUS_IND_CD, claimGroup.getPatientStatusCd().get()));
		}
		
		// add EOB information to fields that are common between the Inpatient and SNF claim types
		TransformerUtils.addCommonEobInformationInpatientSNF(eob, claimGroup.getAdmissionTypeCd(),
				claimGroup.getSourceAdmissionCd(), claimGroup.getNoncoveredStayFromDate(),
				claimGroup.getNoncoveredStayThroughDate(), claimGroup.getCoveredCareThoughDate(),
				claimGroup.getMedicareBenefitsExhaustedDate(), claimGroup.getDiagnosisRelatedGroupCd());
		
		BenefitBalanceComponent benefitBalances = new BenefitBalanceComponent(
				TransformerUtils.createCodeableConcept(
						TransformerConstants.CODING_FHIR_BENEFIT_BALANCE, BenefitCategory.MEDICAL.toCode()));
		eob.getBenefitBalance().add(benefitBalances);

		// map indirectMedicalEducationAmount to eob.benefitbalance.financial
		if (claimGroup.getIndirectMedicalEducationAmount().isPresent()) {
			BenefitComponent bc = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODED_ADJUDICATION_INDIRECT_MEDICAL_EDUCATION_AMOUNT));
			bc.setAllowed(new Money().setSystem(TransformerConstants.CODED_MONEY_USD)
					.setValue(claimGroup.getIndirectMedicalEducationAmount().get()));
			eob.getBenefitBalanceFirstRep().getFinancial().add(bc);
		}

		// map disproportionateShareAmount to eob.benefitbalance.financial
		if (claimGroup.getDisproportionateShareAmount().isPresent()) {
			BenefitComponent bc = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODED_ADJUDICATION_DISPROPORTIONATE_SHARE_AMOUNT));
			bc.setAllowed(new Money().setSystem(TransformerConstants.CODED_MONEY_USD)
					.setValue(claimGroup.getDisproportionateShareAmount().get()));
			eob.getBenefitBalanceFirstRep().getFinancial().add(bc);
		}

		if (claimGroup.getPassThruPerDiemAmount() != null) {
			BenefitComponent benefitPerDiem = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_PASS_THRU_PER_DIEM));
			benefitPerDiem.setAllowed(new Money().setSystem(TransformerConstants.CODED_MONEY_USD)
					.setValue(claimGroup.getPassThruPerDiemAmount()));
			benefitBalances.getFinancial().add(benefitPerDiem);
		}

		if (claimGroup.getProfessionalComponentCharge() != null) {
			BenefitComponent benefitProfessionComponentAmt = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_PROFFESIONAL_COMPONENT_CHARGE));
			benefitProfessionComponentAmt.setAllowed(new Money().setSystem(TransformerConstants.CODED_MONEY_USD)
					.setValue(claimGroup.getProfessionalComponentCharge()));
			benefitBalances.getFinancial().add(benefitProfessionComponentAmt);
		}

		if (claimGroup.getClaimTotalPPSCapitalAmount() != null) {
			BenefitComponent claimTotalPPSAmt = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_TOTAL_PPS_CAPITAL));
			claimTotalPPSAmt.setAllowed(new Money().setSystem(TransformerConstants.CODED_MONEY_USD)
					.setValue(claimGroup.getClaimTotalPPSCapitalAmount().get()));
			benefitBalances.getFinancial().add(claimTotalPPSAmt);
		}
	
		/*
		 * add field values to the benefit balances that are common between the
		 * Inpatient and SNF claim types
		 */
		TransformerUtils.addCommonBenefitComponentInpatientSNF(benefitBalances, claimGroup.getCoinsuranceDayCount(),
				claimGroup.getNonUtilizationDayCount(), claimGroup.getDeductibleAmount(),
				claimGroup.getPartACoinsuranceLiabilityAmount(), claimGroup.getBloodPintsFurnishedQty(),
				claimGroup.getNoncoveredCharge(), claimGroup.getTotalDeductionAmount(),
				claimGroup.getClaimPPSCapitalDisproportionateShareAmt(), claimGroup.getClaimPPSCapitalExceptionAmount(),
				claimGroup.getClaimPPSCapitalFSPAmount(), claimGroup.getClaimPPSCapitalIMEAmount(),
				claimGroup.getClaimPPSCapitalOutlierAmount(), claimGroup.getClaimPPSOldCapitalHoldHarmlessAmount());

		if (claimGroup.getDrgOutlierApprovedPaymentAmount() != null) {
			BenefitComponent nchDrugOutlierApprovedPaymentAmount = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_DRUG_OUTLIER_APPROVED_PAYMENT));
			nchDrugOutlierApprovedPaymentAmount
					.setAllowed(new Money().setSystem(TransformerConstants.CODED_MONEY_USD)
							.setValue(claimGroup.getDrgOutlierApprovedPaymentAmount().get()));
			benefitBalances.getFinancial().add(nchDrugOutlierApprovedPaymentAmount);
		}

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
				claimGroup.getPrimaryPayerPaidAmount(), claimGroup.getFiscalIntermediaryNumber());
		
		// Common group level fields between Inpatient, HHA, Hospice and SNF
		TransformerUtils.mapEobCommonGroupInpHHAHospiceSNF(eob, claimGroup.getClaimAdmissionDate(),
				claimGroup.getBeneficiaryDischargeDate(), Optional.of(claimGroup.getUtilizationDayCount()),
				benefitBalances);

		for (Diagnosis diagnosis : extractDiagnoses(claimGroup))
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

		for (InpatientClaimLine claimLine : claimGroup.getLines()) {
			ItemComponent item = eob.addItem();
			item.setSequence(claimLine.getLineNumber().intValue());

			// set hcpcs modifier codes for the claim
			TransformerUtils.setHcpcsModifierCodes(item, claimLine.getHcpcsCode(), Optional.empty(), Optional.empty(),
					Optional.empty());

			item.setLocation(new Address().setState((claimGroup.getProviderStateCode())));

			// Common item level fields between Inpatient, Outpatient, HHA, Hospice and SNF
			TransformerUtils.mapEobCommonItemRevenue(item, eob, claimLine.getRevenueCenter(), claimLine.getRateAmount(),
					claimLine.getTotalChargeAmount(), claimLine.getNonCoveredChargeAmount(), claimLine.getUnitCount(),
					claimLine.getNationalDrugCodeQuantity(), claimLine.getNationalDrugCodeQualifierCode(),
					claimLine.getRevenueCenterRenderingPhysicianNPI());
			
			// Common group level field coinsurance between Inpatient, HHA, Hospice and SNF
			TransformerUtils.mapEobCommonGroupInpHHAHospiceSNFCoinsurance(eob, item,
					claimLine.getDeductibleCoinsuranceCd());

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
		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosis1Code(), claim.getDiagnosis1CodeVersion(),
				claim.getDiagnosis1PresentOnAdmissionCode(), DiagnosisLabel.PRINCIPAL));
		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosisPrincipalCode(),
				claim.getDiagnosisPrincipalCodeVersion(), DiagnosisLabel.PRINCIPAL));
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

		diagnosisAdder
				.accept(Diagnosis.from(claim.getDiagnosisExternal1Code(), claim.getDiagnosisExternal1CodeVersion(),
						claim.getDiagnosisExternal1PresentOnAdmissionCode(), DiagnosisLabel.FIRSTEXTERNAL));
		diagnosisAdder.accept(Diagnosis.from(claim.getDiagnosisExternalFirstCode(),
				claim.getDiagnosisExternalFirstCodeVersion(), DiagnosisLabel.FIRSTEXTERNAL));
		diagnosisAdder
				.accept(Diagnosis.from(claim.getDiagnosisExternal2Code(), claim.getDiagnosisExternal2CodeVersion(),
						claim.getDiagnosisExternal2PresentOnAdmissionCode(), DiagnosisLabel.EXTERNAL));
		diagnosisAdder
				.accept(Diagnosis.from(claim.getDiagnosisExternal3Code(), claim.getDiagnosisExternal3CodeVersion(),
						claim.getDiagnosisExternal3PresentOnAdmissionCode(), DiagnosisLabel.EXTERNAL));
		diagnosisAdder
				.accept(Diagnosis.from(claim.getDiagnosisExternal4Code(), claim.getDiagnosisExternal4CodeVersion(),
						claim.getDiagnosisExternal4PresentOnAdmissionCode(), DiagnosisLabel.EXTERNAL));
		diagnosisAdder
				.accept(Diagnosis.from(claim.getDiagnosisExternal5Code(), claim.getDiagnosisExternal5CodeVersion(),
						claim.getDiagnosisExternal5PresentOnAdmissionCode(), DiagnosisLabel.EXTERNAL));
		diagnosisAdder
				.accept(Diagnosis.from(claim.getDiagnosisExternal6Code(), claim.getDiagnosisExternal6CodeVersion(),
						claim.getDiagnosisExternal6PresentOnAdmissionCode(), DiagnosisLabel.EXTERNAL));
		diagnosisAdder
				.accept(Diagnosis.from(claim.getDiagnosisExternal7Code(), claim.getDiagnosisExternal7CodeVersion(),
						claim.getDiagnosisExternal7PresentOnAdmissionCode(), DiagnosisLabel.EXTERNAL));
		diagnosisAdder
				.accept(Diagnosis.from(claim.getDiagnosisExternal8Code(), claim.getDiagnosisExternal8CodeVersion(),
						claim.getDiagnosisExternal8PresentOnAdmissionCode(), DiagnosisLabel.EXTERNAL));
		diagnosisAdder
				.accept(Diagnosis.from(claim.getDiagnosisExternal9Code(), claim.getDiagnosisExternal9CodeVersion(),
						claim.getDiagnosisExternal9PresentOnAdmissionCode(), DiagnosisLabel.EXTERNAL));
		diagnosisAdder
				.accept(Diagnosis.from(claim.getDiagnosisExternal10Code(), claim.getDiagnosisExternal10CodeVersion(),
						claim.getDiagnosisExternal10PresentOnAdmissionCode(), DiagnosisLabel.EXTERNAL));
		diagnosisAdder
				.accept(Diagnosis.from(claim.getDiagnosisExternal11Code(), claim.getDiagnosisExternal11CodeVersion(),
						claim.getDiagnosisExternal11PresentOnAdmissionCode(), DiagnosisLabel.EXTERNAL));
		diagnosisAdder
				.accept(Diagnosis.from(claim.getDiagnosisExternal12Code(), claim.getDiagnosisExternal12CodeVersion(),
						claim.getDiagnosisExternal12PresentOnAdmissionCode(), DiagnosisLabel.EXTERNAL));

		return diagnoses;
	}

}

