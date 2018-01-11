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
import org.hl7.fhir.dstu3.model.SimpleQuantity;
import org.hl7.fhir.dstu3.model.codesystems.BenefitCategory;
import org.hl7.fhir.dstu3.model.codesystems.ClaimCareteamrole;

import com.justdavis.karl.misc.exceptions.BadCodeMonkeyException;

import gov.hhs.cms.bluebutton.data.model.rif.OutpatientClaim;
import gov.hhs.cms.bluebutton.data.model.rif.OutpatientClaimLine;
import gov.hhs.cms.bluebutton.server.app.stu3.providers.Diagnosis.DiagnosisLabel;

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

		eob.setId(TransformerUtils.buildEobId(ClaimType.OUTPATIENT, claimGroup.getClaimId()));
		eob.addIdentifier().setSystem(TransformerConstants.CODING_CCW_CLAIM_ID)
				.setValue(claimGroup.getClaimId());
		eob.addIdentifier().setSystem(TransformerConstants.CODING_CCW_CLAIM_GROUP_ID)
				.setValue(claimGroup.getClaimGroupId().toPlainString());

		eob.setType(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_CLAIM_TYPE,
				claimGroup.getClaimTypeCode()));

		// map eob type codes into FHIR
		TransformerUtils.mapEobType(eob.getType(), ClaimType.OUTPATIENT, Optional.of(claimGroup.getNearLineRecordIdCode()), 
				Optional.of(claimGroup.getClaimTypeCode()));
		
		eob.getInsurance()
				.setCoverage(TransformerUtils.referenceCoverage(claimGroup.getBeneficiaryId(), MedicareSegment.PART_B));
		eob.setPatient(TransformerUtils.referencePatient(claimGroup.getBeneficiaryId()));
		eob.setStatus(ExplanationOfBenefitStatus.ACTIVE);

		TransformerUtils.validatePeriodDates(claimGroup.getDateFrom(), claimGroup.getDateThrough());
		TransformerUtils.setPeriodStart(eob.getBillablePeriod(), claimGroup.getDateFrom());
		TransformerUtils.setPeriodEnd(eob.getBillablePeriod(), claimGroup.getDateThrough());

		TransformerUtils.addExtensionCoding(eob.getBillablePeriod(), TransformerConstants.EXTENSION_CODING_CLAIM_QUERY,
				TransformerConstants.EXTENSION_CODING_CLAIM_QUERY,
				String.valueOf(claimGroup.getClaimQueryCode()));

		eob.setProvider(TransformerUtils.createIdentifierReference(TransformerConstants.IDENTIFIER_CMS_PROVIDER_NUMBER,
				claimGroup.getProviderNumber()));

		if (claimGroup.getClaimNonPaymentReasonCode().isPresent()) {
			TransformerUtils.addExtensionCoding(eob, TransformerConstants.EXTENSION_CODING_CCW_PAYMENT_DENIAL_REASON,
					TransformerConstants.EXTENSION_CODING_CCW_PAYMENT_DENIAL_REASON,
					claimGroup.getClaimNonPaymentReasonCode().get());
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

		if (claimGroup.getBloodDeductibleLiabilityAmount() != null) {
			BenefitComponent bloodDeductibleLiabilityAmount = new BenefitComponent(
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
							TransformerConstants.CODED_BENEFIT_BALANCE_TYPE_BLOOD_DEDUCTIBLE_LIABILITY));
			bloodDeductibleLiabilityAmount.setAllowed(
					new Money().setSystem(TransformerConstants.CODED_MONEY_USD)
							.setValue(claimGroup.getBloodDeductibleLiabilityAmount()));
			benefitBalances.getFinancial().add(bloodDeductibleLiabilityAmount);
		}

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

		TransformerUtils.addExtensionCoding(eob.getType(),
				TransformerConstants.EXTENSION_CODING_CCW_CLAIM_SERVICE_CLASSIFICATION,
				TransformerConstants.EXTENSION_CODING_CCW_CLAIM_SERVICE_CLASSIFICATION,
				String.valueOf(claimGroup.getClaimServiceClassificationTypeCode()));

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

		if (claimGroup.getOperatingPhysicianNpi().isPresent()) {
			TransformerUtils.addCareTeamPractitioner(eob, null, TransformerConstants.CODING_NPI_US,
					claimGroup.getOperatingPhysicianNpi().get(),
					ClaimCareteamrole.ASSIST.toCode());
		}

		if (claimGroup.getOtherPhysicianNpi().isPresent()) {
			TransformerUtils.addCareTeamPractitioner(eob, null, TransformerConstants.CODING_NPI_US,
					claimGroup.getOtherPhysicianNpi().get(),
					ClaimCareteamrole.OTHER.toCode());
		}

		if (claimGroup.getMcoPaidSw().isPresent()) {
			TransformerUtils.addInformation(eob,
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_MCO_PAID,
							String.valueOf(claimGroup.getMcoPaidSw().get())));
		}

		for (Diagnosis diagnosis : extractDiagnoses(claimGroup))
			TransformerUtils.addDiagnosisCode(eob, diagnosis);

		for (CCWProcedure procedure : extractCCWProcedures(claimGroup))
			TransformerUtils.addProcedureCode(eob, procedure);

		for (OutpatientClaimLine claimLine : claimGroup.getLines()) {
			ItemComponent item = eob.addItem();
			item.setSequence(claimLine.getLineNumber().intValue());

			TransformerUtils.addExtensionCoding(item, TransformerConstants.CODING_FHIR_ACT_INVOICE_GROUP,
					TransformerConstants.CODING_FHIR_ACT_INVOICE_GROUP,
					TransformerConstants.CODED_ACT_INVOICE_GROUP_CLINICAL_SERVICES_AND_PRODUCTS);

			item.setRevenue(
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CMS_REVENUE_CENTER,
							claimLine.getRevenueCenterCode()));

			item.setLocation(new Address().setState((claimGroup.getProviderStateCode())));

			if (claimLine.getNationalDrugCode().isPresent()) {
				item.setService(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_NDC,
						claimLine.getNationalDrugCode().get()));
			}

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

			item.addAdjudication()
					.setCategory(
							TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
							TransformerConstants.CODED_ADJUDICATION_RATE_AMOUNT))
					.getAmount().setSystem(TransformerConstants.CODING_MONEY)
					.setCode(TransformerConstants.CODED_MONEY_USD)
					.setValue(claimLine.getRateAmount());

			if (claimLine.getHcpcsCode().isPresent()) {
				item.addModifier(
						TransformerUtils.createCodeableConcept(TransformerConstants.CODING_HCPCS,
								claimLine.getHcpcsCode().get()));
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
	 *            the {@link OutpatientClaim} to extract the {@link Diagnosis}es
	 *            from
	 * @return the {@link Diagnosis}es that can be extracted from the specified
	 *         {@link OutpatientClaim}
	 */
	private static List<Diagnosis> extractDiagnoses(OutpatientClaim claim) {
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

		diagnosisAdder
				.accept(Diagnosis.from(claim.getDiagnosisAdmission1Code(), claim.getDiagnosisAdmission1CodeVersion()));
		diagnosisAdder
				.accept(Diagnosis.from(claim.getDiagnosisAdmission2Code(), claim.getDiagnosisAdmission2CodeVersion()));
		diagnosisAdder
				.accept(Diagnosis.from(claim.getDiagnosisAdmission3Code(), claim.getDiagnosisAdmission3CodeVersion()));

		return diagnoses;
	}

	/**
	 * @param claim
	 *            the {@link OutpatientClaim} to extract the
	 *            {@link CCWProcedure}es from
	 * @return the {@link CCWProcedure}es that can be extracted from the
	 *         specified {@link OutpatientClaim}
	 */
	private static List<CCWProcedure> extractCCWProcedures(OutpatientClaim claim) {
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

