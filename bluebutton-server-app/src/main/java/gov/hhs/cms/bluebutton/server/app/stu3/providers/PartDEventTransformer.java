package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.math.BigDecimal;

import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DateType;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ExplanationOfBenefitStatus;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.SimpleQuantity;

import com.justdavis.karl.misc.exceptions.BadCodeMonkeyException;

import gov.hhs.cms.bluebutton.data.model.rif.PartDEvent;
import gov.hhs.cms.bluebutton.data.model.rif.parse.InvalidRifValueException;

/**
 * Transforms CCW {@link PartDEvent} instances into FHIR
 * {@link ExplanationOfBenefit} resources.
 */
final class PartDEventTransformer {
	/**
	 * @param claim
	 *            the CCW {@link PartDEvent} to transform
	 * @return a FHIR {@link ExplanationOfBenefit} resource that represents the
	 *         specified {@link PartDEvent}
	 */
	static ExplanationOfBenefit transform(Object claim) {
		if (!(claim instanceof PartDEvent))
			throw new BadCodeMonkeyException();
		return transformClaim((PartDEvent) claim);
	}

	/**
	 * @param claimGroup
	 *            the CCW {@link PartDEvent} to transform
	 * @return a FHIR {@link ExplanationOfBenefit} resource that represents the
	 *         specified {@link PartDEvent}
	 */
	private static ExplanationOfBenefit transformClaim(PartDEvent claimGroup) {
		ExplanationOfBenefit eob = new ExplanationOfBenefit();

		eob.setId(TransformerUtils.buildEobId(ClaimType.PDE, claimGroup.getEventId()));
		eob.addIdentifier().setSystem(TransformerConstants.CODING_SYSTEM_CCW_PDE_ID).setValue(claimGroup.getEventId());
		eob.addIdentifier().setSystem(TransformerConstants.CODING_SYSTEM_CCW_CLAIM_GRP_ID)
				.setValue(claimGroup.getClaimGroupId().toPlainString());
		eob.getInsurance()
				.setCoverage(TransformerUtils.referenceCoverage(claimGroup.getBeneficiaryId(), MedicareSegment.PART_D));
		TransformerUtils.addExtensionCoding(eob.getInsurance().getCoverage(),
				TransformerConstants.CODING_SYSTEM_PDE_PLAN_CONTRACT_ID,
				TransformerConstants.CODING_SYSTEM_PDE_PLAN_CONTRACT_ID, claimGroup.getPlanContractId());
		TransformerUtils.addExtensionCoding(eob.getInsurance().getCoverage(),
				TransformerConstants.CODING_SYSTEM_PDE_PLAN_BENEFIT_PACKAGE_ID,
				TransformerConstants.CODING_SYSTEM_PDE_PLAN_BENEFIT_PACKAGE_ID, claimGroup.getPlanBenefitPackageId());
		eob.setPatient(TransformerUtils.referencePatient(claimGroup.getBeneficiaryId()));
		eob.setStatus(ExplanationOfBenefitStatus.ACTIVE);

		eob.addIdentifier().setSystem(TransformerConstants.CODING_SYSTEM_RX_SRVC_RFRNC_NUM)
				.setValue(String.valueOf(claimGroup.getPrescriptionReferenceNumber()));

		eob.getType().addCoding(
				new Coding().setSystem(TransformerConstants.CODING_SYSTEM_FHIR_CLAIM_TYPE).setCode("pharmacy"));
		eob.setStatus(ExplanationOfBenefitStatus.ACTIVE);

		Reference patientRef = TransformerUtils.referencePatient(claimGroup.getBeneficiaryId());
		eob.setPatient(patientRef);
		if (claimGroup.getPaymentDate().isPresent()) {
			eob.getPayment().setDate(TransformerUtils.convertToDate(claimGroup.getPaymentDate().get()));
		}

		ItemComponent rxItem = eob.addItem();
		rxItem.setSequence(1);

		ExplanationOfBenefit.DetailComponent detail = new ExplanationOfBenefit.DetailComponent();
		switch (claimGroup.getCompoundCode()) {
		// COMPOUNDED
		case 2:
			/* Pharmacy dispense invoice for a compound */
			detail.getType()
					.addCoding(new Coding().setSystem(TransformerConstants.CODING_SYSTEM_FHIR_ACT).setCode("RXCINV"));
			break;
		// NOT_COMPOUNDED
		case 1:
			/*
			 * Pharmacy dispense invoice not involving a compound
			 */
			detail.getType()
					.addCoding(new Coding().setSystem(TransformerConstants.CODING_SYSTEM_FHIR_ACT).setCode("RXDINV"));
			break;
		// NOT_SPECIFIED
		case 0:
			/*
			 * Pharmacy dispense invoice not specified - do not set a value
			 */
			break;
		default:
			/*
			 * Unexpected value encountered - compound code should be either
			 * compounded or not compounded.
			 */
			throw new InvalidRifValueException(
					"Unexpected value encountered - compound code should be either compounded or not compounded: "
							+ claimGroup.getCompoundCode());
		}

		rxItem.addDetail(detail);

		rxItem.setServiced(
				new DateType().setValue(TransformerUtils.convertToDate(claimGroup.getPrescriptionFillDate())));

		Coding rxCategoryCoding;
		BigDecimal rxPaidAmountValue;
		switch (claimGroup.getDrugCoverageStatusCode()) {
		/*
		 * If covered by Part D, use value from partDPlanCoveredPaidAmount
		 */
		// COVERED
		case 'C':
			rxCategoryCoding = new Coding().setSystem(TransformerConstants.CODING_SYSTEM_ADJUDICATION_CMS)
					.setCode(TransformerConstants.CODED_ADJUDICATION_PART_D_COVERED);
			rxPaidAmountValue = claimGroup.getPartDPlanCoveredPaidAmount();
			break;
		/*
		 * If not covered by Part D, use value from
		 * partDPlanNonCoveredPaidAmount. There are 2 categories of non-covered
		 * payment amounts: supplemental drugs covered by enhanced plans, and
		 * over the counter drugs that are covered only under specific
		 * circumstances.
		 */
		// SUPPLEMENTAL
		case 'E':
			rxCategoryCoding = new Coding().setSystem(TransformerConstants.CODING_SYSTEM_ADJUDICATION_CMS)
					.setCode(TransformerConstants.CODED_ADJUDICATION_PART_D_NONCOVERED_SUPPLEMENT);
			rxPaidAmountValue = claimGroup.getPartDPlanNonCoveredPaidAmount();
			break;
		// OVER THE COUNTER
		case '0':
			rxCategoryCoding = new Coding().setSystem(TransformerConstants.CODING_SYSTEM_ADJUDICATION_CMS)
					.setCode(TransformerConstants.CODED_ADJUDICATION_PART_D_NONCOVERED_OTC);
			rxPaidAmountValue = claimGroup.getPartDPlanNonCoveredPaidAmount();
			break;
		default:
			/*
			 * Unexpected value encountered - drug coverage status code should
			 * be one of the three above.
			 */
			throw new InvalidRifValueException("Unexpected value encountered - drug coverage status code is invalid: "
					+ claimGroup.getDrugCoverageStatusCode());
		}
		CodeableConcept rxCategory = new CodeableConcept();
		rxCategory.addCoding(rxCategoryCoding);
		rxItem.addAdjudication().setCategory(rxCategory).getAmount().setSystem(TransformerConstants.CODING_SYSTEM_MONEY)
				.setCode(TransformerConstants.CODING_SYSTEM_MONEY_US).setValue(rxPaidAmountValue);

		rxItem.addAdjudication()
				.setCategory(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_SYSTEM_ADJUDICATION_CMS,
						TransformerConstants.CODED_ADJUDICATION_GDCB))
				.getAmount().setSystem(TransformerConstants.CODING_SYSTEM_MONEY)
				.setCode(TransformerConstants.CODING_SYSTEM_MONEY_US)
				.setValue(claimGroup.getGrossCostBelowOutOfPocketThreshold());

		rxItem.addAdjudication()
				.setCategory(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_SYSTEM_ADJUDICATION_CMS,
						TransformerConstants.CODED_ADJUDICATION_GDCA))
				.getAmount().setSystem(TransformerConstants.CODING_SYSTEM_MONEY)
				.setCode(TransformerConstants.CODING_SYSTEM_MONEY_US)
				.setValue(claimGroup.getGrossCostAboveOutOfPocketThreshold());

		rxItem.addAdjudication()
				.setCategory(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_SYSTEM_ADJUDICATION_CMS,
						TransformerConstants.CODED_ADJUDICATION_PATIENT_PAY))
				.getAmount().setSystem(TransformerConstants.CODING_SYSTEM_MONEY)
				.setCode(TransformerConstants.CODING_SYSTEM_MONEY_US).setValue(claimGroup.getPatientPaidAmount());

		rxItem.addAdjudication()
				.setCategory(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_SYSTEM_ADJUDICATION_CMS,
						TransformerConstants.CODED_ADJUDICATION_OTHER_TROOP_AMOUNT))
				.getAmount().setSystem(TransformerConstants.CODING_SYSTEM_MONEY)
				.setCode(TransformerConstants.CODING_SYSTEM_MONEY_US)
				.setValue(claimGroup.getOtherTrueOutOfPocketPaidAmount());

		rxItem.addAdjudication()
				.setCategory(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_SYSTEM_ADJUDICATION_CMS,
						TransformerConstants.CODED_ADJUDICATION_LOW_INCOME_SUBSIDY_AMOUNT))
				.getAmount().setSystem(TransformerConstants.CODING_SYSTEM_MONEY)
				.setCode(TransformerConstants.CODING_SYSTEM_MONEY_US)
				.setValue(claimGroup.getLowIncomeSubsidyPaidAmount());

		rxItem.addAdjudication()
				.setCategory(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_SYSTEM_ADJUDICATION_CMS,
						TransformerConstants.CODED_ADJUDICATION_PATIENT_LIABILITY_REDUCED_AMOUNT))
				.getAmount().setSystem(TransformerConstants.CODING_SYSTEM_MONEY)
				.setCode(TransformerConstants.CODING_SYSTEM_MONEY_US)
				.setValue(claimGroup.getPatientLiabilityReductionOtherPaidAmount());

		rxItem.addAdjudication()
				.setCategory(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_SYSTEM_ADJUDICATION_CMS,
						TransformerConstants.CODED_ADJUDICATION_TOTAL_COST))
				.getAmount().setSystem(TransformerConstants.CODING_SYSTEM_MONEY)
				.setCode(TransformerConstants.CODING_SYSTEM_MONEY_US).setValue(claimGroup.getTotalPrescriptionCost());

		rxItem.addAdjudication()
				.setCategory(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_SYSTEM_ADJUDICATION_CMS,
						TransformerConstants.CODED_ADJUDICATION_GAP_DISCOUNT_AMOUNT))
				.getAmount().setSystem(TransformerConstants.CODING_SYSTEM_MONEY)
				.setCode(TransformerConstants.CODING_SYSTEM_MONEY_US).setValue(claimGroup.getGapDiscountAmount());

		if (claimGroup.getPrescriberIdQualifierCode() == null
				|| !claimGroup.getPrescriberIdQualifierCode().equalsIgnoreCase("01"))
			throw new InvalidRifValueException(
					"Prescriber ID Qualifier Code is invalid: " + claimGroup.getPrescriberIdQualifierCode());

		if (claimGroup.getPrescriberId() != null) {
			TransformerUtils.addCareTeamPractitioner(eob, rxItem, TransformerConstants.CODING_SYSTEM_NPI_US,
					claimGroup.getPrescriberId(),
					TransformerConstants.CARE_TEAM_ROLE_PRIMARY);
		}

		rxItem.setService(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_SYSTEM_NDC,
				claimGroup.getPrescriptionOriginationCode().get().toString()));

		SimpleQuantity quantityDispensed = new SimpleQuantity();
		quantityDispensed.setValue(claimGroup.getQuantityDispensed());
		rxItem.setQuantity(quantityDispensed);

		rxItem.addModifier(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_SYSTEM_PDE_DAYS_SUPPLY,
				String.valueOf(claimGroup.getDaysSupply())));

		// TODO CBBD-241 - This code was commented out because values other than
		// "01"
		// were coming thru
		// such as "07". Need to discuss with Karl if this check needs to be
		// here -

		/*
		 * if (claimGroup.serviceProviderIdQualiferCode == null ||
		 * !claimGroup.serviceProviderIdQualiferCode.equalsIgnoreCase("01"))
		 * throw new InvalidRifValueException(
		 * "Service Provider ID Qualifier Code is invalid: " +
		 * claimGroup.serviceProviderIdQualiferCode);
		 */

		if (!claimGroup.getServiceProviderId().isEmpty()) {
			eob.setOrganization(
					TransformerUtils.createIdentifierReference(TransformerConstants.CODING_SYSTEM_NPI_US,
							claimGroup.getServiceProviderId()));
			eob.setFacility(
					TransformerUtils.createIdentifierReference(TransformerConstants.CODING_SYSTEM_NPI_US,
							claimGroup.getServiceProviderId()));
			TransformerUtils.addExtensionCoding(eob.getFacility(),
					TransformerConstants.CODING_SYSTEM_CCW_PHRMCY_SRVC_TYPE_CD,
					TransformerConstants.CODING_SYSTEM_CCW_PHRMCY_SRVC_TYPE_CD, claimGroup.getPharmacyTypeCode());
		}

		/*
		 * Storing code values in EOB.information below
		 */

		TransformerUtils.addInformation(eob,
				TransformerUtils.createCodeableConcept(TransformerConstants.CODING_SYSTEM_RX_DAW_PRODUCT_CD,
						String.valueOf(claimGroup.getDispenseAsWrittenProductSelectionCode())));

		if (claimGroup.getDispensingStatusCode().isPresent())
			TransformerUtils.addInformation(eob,
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_SYSTEM_RX_DISPENSE_STATUS_CD,
							String.valueOf(claimGroup.getDispensingStatusCode().get())));

		TransformerUtils.addInformation(eob,
				TransformerUtils.createCodeableConcept(TransformerConstants.CODING_SYSTEM_RX_COVERAGE_STATUS_CD,
						String.valueOf(claimGroup.getDrugCoverageStatusCode())));

		if (claimGroup.getAdjustmentDeletionCode().isPresent())
			TransformerUtils.addInformation(eob,
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_SYSTEM_RX_ADJUSTMENT_DEL_CD,
							String.valueOf(claimGroup.getAdjustmentDeletionCode().get())));

		if (claimGroup.getNonstandardFormatCode().isPresent())
			TransformerUtils.addInformation(eob,
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_SYSTEM_RX_NON_STD_FORMAT_CD,
							String.valueOf(claimGroup.getNonstandardFormatCode().get())));

		if (claimGroup.getPricingExceptionCode().isPresent())
			TransformerUtils.addInformation(eob,
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_SYSTEM_RX_PRICING_EXCEPTION_CD,
							String.valueOf(claimGroup.getPricingExceptionCode().get())));

		if (claimGroup.getCatastrophicCoverageCode().isPresent())
			TransformerUtils.addInformation(eob,
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_SYSTEM_RX_CATASTROPHIC_COV_CD,
							String.valueOf(claimGroup.getCatastrophicCoverageCode().get())));

		if (claimGroup.getPrescriptionOriginationCode().isPresent())
			TransformerUtils.addInformation(eob,
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_SYSTEM_RX_PRESCRIPTION_ORIGIN_CD,
							String.valueOf(claimGroup.getPrescriptionOriginationCode().get())));

		if (claimGroup.getBrandGenericCode().isPresent())
			TransformerUtils.addInformation(eob,
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_SYSTEM_RX_BRAND_GENERIC_CD,
							String.valueOf(claimGroup.getBrandGenericCode().get())));

		TransformerUtils.addInformation(eob,
				TransformerUtils.createCodeableConcept(TransformerConstants.CODING_SYSTEM_RX_PHARMACY_SVC_TYPE_CD,
						claimGroup.getPharmacyTypeCode()));

		TransformerUtils.addInformation(eob,
				TransformerUtils.createCodeableConcept(TransformerConstants.CODING_SYSTEM_RX_PATIENT_RESIDENCE_CD,
						claimGroup.getPatientResidenceCode()));

		if (claimGroup.getSubmissionClarificationCode().isPresent())
			TransformerUtils.addInformation(eob,
					TransformerUtils.createCodeableConcept(
							TransformerConstants.CODING_SYSTEM_RX_SUBMISSION_CLARIFICATION_CD,
							claimGroup.getSubmissionClarificationCode().get()));

		return eob;
	}

}

