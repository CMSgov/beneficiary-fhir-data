package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.math.BigDecimal;

import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DateType;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ExplanationOfBenefitStatus;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.dstu3.model.SimpleQuantity;
import org.hl7.fhir.dstu3.model.codesystems.ClaimCareteamrole;

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
		eob.addIdentifier().setSystem(TransformerConstants.CODING_CCW_PARTD_EVENT_ID).setValue(claimGroup.getEventId());
		eob.addIdentifier().setSystem(TransformerConstants.CODING_CCW_CLAIM_GROUP_ID)
				.setValue(claimGroup.getClaimGroupId().toPlainString());
		eob.addIdentifier().setSystem(TransformerConstants.IDENTIFIER_RX_SERVICE_REFERENCE_NUMBER)
				.setValue(String.valueOf(claimGroup.getPrescriptionReferenceNumber()));

		/*
		 * Note: Part D events are the one CCW "claim" type that don't have
		 * NCH_CLM_TYPE_CD or NCH_NEAR_LINE_REC_IDENT_CD codes.
		 */
		eob.getType()
				.addCoding(new Coding().setSystem(TransformerConstants.CODING_FHIR_CLAIM_TYPE)
						.setCode(org.hl7.fhir.dstu3.model.codesystems.ClaimType.PHARMACY.toCode()));

		eob.getInsurance()
				.setCoverage(TransformerUtils.referenceCoverage(claimGroup.getBeneficiaryId(), MedicareSegment.PART_D));
		/*
		 * FIXME this should be mapped as an extension valueIdentifier instead
		 * of as a valueCodeableConcept
		 */
		TransformerUtils.addExtensionCoding(eob.getInsurance().getCoverage(),
				TransformerConstants.EXTENSION_IDENTIFIER_PDE_PLAN_CONTRACT_ID,
				TransformerConstants.EXTENSION_IDENTIFIER_PDE_PLAN_CONTRACT_ID, claimGroup.getPlanContractId());
		/*
		 * FIXME this should be mapped as an extension valueIdentifier instead
		 * of as a valueCodeableConcept
		 */
		TransformerUtils.addExtensionCoding(eob.getInsurance().getCoverage(),
				TransformerConstants.EXTENSION_IDENTIFIER_PDE_PLAN_BENEFIT_PACKAGE_ID,
				TransformerConstants.EXTENSION_IDENTIFIER_PDE_PLAN_BENEFIT_PACKAGE_ID, claimGroup.getPlanBenefitPackageId());
		eob.setPatient(TransformerUtils.referencePatient(claimGroup.getBeneficiaryId()));
		eob.setStatus(ExplanationOfBenefitStatus.ACTIVE);

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
					.addCoding(new Coding().setSystem(TransformerConstants.CODING_FHIR_ACT).setCode("RXCINV"));
			break;
		// NOT_COMPOUNDED
		case 1:
			/*
			 * Pharmacy dispense invoice not involving a compound
			 */
			detail.getType()
					.addCoding(new Coding().setSystem(TransformerConstants.CODING_FHIR_ACT).setCode("RXDINV"));
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
			rxCategoryCoding = new Coding().setSystem(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY)
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
			rxCategoryCoding = new Coding().setSystem(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY)
					.setCode(TransformerConstants.CODED_ADJUDICATION_PART_D_NONCOVERED_SUPPLEMENT);
			rxPaidAmountValue = claimGroup.getPartDPlanNonCoveredPaidAmount();
			break;
		// OVER THE COUNTER
		case '0':
			rxCategoryCoding = new Coding().setSystem(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY)
					.setCode(TransformerConstants.CODED_ADJUDICATION_PART_D_NONCOVERED_OTC);
			rxPaidAmountValue = claimGroup.getPartDPlanNonCoveredPaidAmount();
			break;
		default:
			/*
			 * Unexpected value encountered - drug coverage status code should
			 * be one of the three above.
			 */
			rxCategoryCoding = new Coding().setSystem(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY)
					.setCode("Unknown:" + claimGroup.getDrugCoverageStatusCode());
			rxPaidAmountValue = claimGroup.getPartDPlanCoveredPaidAmount();
		}
		CodeableConcept rxCategory = new CodeableConcept();
		rxCategory.addCoding(rxCategoryCoding);
		rxItem.addAdjudication().setCategory(rxCategory).getAmount().setSystem(TransformerConstants.CODING_MONEY)
				.setCode(TransformerConstants.CODED_MONEY_USD).setValue(rxPaidAmountValue);

		rxItem.addAdjudication()
				.setCategory(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
						TransformerConstants.CODED_ADJUDICATION_GDCB))
				.getAmount().setSystem(TransformerConstants.CODING_MONEY)
				.setCode(TransformerConstants.CODED_MONEY_USD)
				.setValue(claimGroup.getGrossCostBelowOutOfPocketThreshold());

		rxItem.addAdjudication()
				.setCategory(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
						TransformerConstants.CODED_ADJUDICATION_GDCA))
				.getAmount().setSystem(TransformerConstants.CODING_MONEY)
				.setCode(TransformerConstants.CODED_MONEY_USD)
				.setValue(claimGroup.getGrossCostAboveOutOfPocketThreshold());

		rxItem.addAdjudication()
				.setCategory(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
						TransformerConstants.CODED_ADJUDICATION_PATIENT_PAY))
				.getAmount().setSystem(TransformerConstants.CODING_MONEY)
				.setCode(TransformerConstants.CODED_MONEY_USD).setValue(claimGroup.getPatientPaidAmount());

		rxItem.addAdjudication()
				.setCategory(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
						TransformerConstants.CODED_ADJUDICATION_OTHER_TROOP_AMOUNT))
				.getAmount().setSystem(TransformerConstants.CODING_MONEY)
				.setCode(TransformerConstants.CODED_MONEY_USD)
				.setValue(claimGroup.getOtherTrueOutOfPocketPaidAmount());

		rxItem.addAdjudication()
				.setCategory(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
						TransformerConstants.CODED_ADJUDICATION_LOW_INCOME_SUBSIDY_AMOUNT))
				.getAmount().setSystem(TransformerConstants.CODING_MONEY)
				.setCode(TransformerConstants.CODED_MONEY_USD)
				.setValue(claimGroup.getLowIncomeSubsidyPaidAmount());

		rxItem.addAdjudication()
				.setCategory(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
						TransformerConstants.CODED_ADJUDICATION_PATIENT_LIABILITY_REDUCED_AMOUNT))
				.getAmount().setSystem(TransformerConstants.CODING_MONEY)
				.setCode(TransformerConstants.CODED_MONEY_USD)
				.setValue(claimGroup.getPatientLiabilityReductionOtherPaidAmount());

		rxItem.addAdjudication()
				.setCategory(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
						TransformerConstants.CODED_ADJUDICATION_TOTAL_COST))
				.getAmount().setSystem(TransformerConstants.CODING_MONEY)
				.setCode(TransformerConstants.CODED_MONEY_USD).setValue(claimGroup.getTotalPrescriptionCost());

		rxItem.addAdjudication()
				.setCategory(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
						TransformerConstants.CODED_ADJUDICATION_GAP_DISCOUNT_AMOUNT))
				.getAmount().setSystem(TransformerConstants.CODING_MONEY)
				.setCode(TransformerConstants.CODED_MONEY_USD).setValue(claimGroup.getGapDiscountAmount());

		if (claimGroup.getPrescriberIdQualifierCode() == null
				|| !claimGroup.getPrescriberIdQualifierCode().equalsIgnoreCase("01"))
			throw new InvalidRifValueException(
					"Prescriber ID Qualifier Code is invalid: " + claimGroup.getPrescriberIdQualifierCode());

		if (claimGroup.getPrescriberId() != null) {
			TransformerUtils.addCareTeamPractitioner(eob, rxItem, TransformerConstants.CODING_NPI_US,
					claimGroup.getPrescriberId(),
					ClaimCareteamrole.PRIMARY.toCode());
		}

		rxItem.setService(TransformerUtils.createCodeableConcept(TransformerConstants.CODING_NDC,
				claimGroup.getNationalDrugCode()));

		SimpleQuantity quantityDispensed = new SimpleQuantity();
		quantityDispensed.setValue(claimGroup.getQuantityDispensed());
		rxItem.setQuantity(quantityDispensed);

		/*
		 * FIXME this should be mapped as an extension with a valueQuantity, not
		 * as CodeableConcept
		 */
		rxItem.addModifier(TransformerUtils.createCodeableConcept(TransformerConstants.FIELD_PDE_DAYS_SUPPLY,
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
					TransformerUtils.createIdentifierReference(TransformerConstants.CODING_NPI_US,
							claimGroup.getServiceProviderId()));
			eob.setFacility(
					TransformerUtils.createIdentifierReference(TransformerConstants.CODING_NPI_US,
							claimGroup.getServiceProviderId()));
			TransformerUtils.addExtensionCoding(eob.getFacility(),
					TransformerConstants.EXTENSION_CODING_CMS_RX_PHARMACY_TYPE,
					TransformerConstants.EXTENSION_CODING_CMS_RX_PHARMACY_TYPE, claimGroup.getPharmacyTypeCode());
		}

		/*
		 * Storing code values in EOB.information below
		 */

		TransformerUtils.addInformation(eob,
				TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CMS_RX_DISPENSE_AS_WRITTEN,
						String.valueOf(claimGroup.getDispenseAsWrittenProductSelectionCode())));

		if (claimGroup.getDispensingStatusCode().isPresent())
			TransformerUtils.addInformation(eob,
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CMS_DISPENSE_STATUS,
							String.valueOf(claimGroup.getDispensingStatusCode().get())));

		TransformerUtils.addInformation(eob,
				TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CMS_RX_COVERAGE_STATUS,
						String.valueOf(claimGroup.getDrugCoverageStatusCode())));

		if (claimGroup.getAdjustmentDeletionCode().isPresent())
			TransformerUtils.addInformation(eob,
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CMS_RX_ADJUSTMENT_DELETION,
							String.valueOf(claimGroup.getAdjustmentDeletionCode().get())));

		if (claimGroup.getNonstandardFormatCode().isPresent())
			TransformerUtils.addInformation(eob,
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CMS_RX_NON_STANDARD_FORMAT,
							String.valueOf(claimGroup.getNonstandardFormatCode().get())));

		if (claimGroup.getPricingExceptionCode().isPresent())
			TransformerUtils.addInformation(eob,
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CMS_RX_PRICING_EXCEPTION,
							String.valueOf(claimGroup.getPricingExceptionCode().get())));

		if (claimGroup.getCatastrophicCoverageCode().isPresent())
			TransformerUtils.addInformation(eob,
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CMS_RX_CATASTROPHIC_COVERAGE,
							String.valueOf(claimGroup.getCatastrophicCoverageCode().get())));

		if (claimGroup.getPrescriptionOriginationCode().isPresent())
			TransformerUtils.addInformation(eob,
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CMS_RX_PRESCRIPTION_ORIGIN,
							String.valueOf(claimGroup.getPrescriptionOriginationCode().get())));

		if (claimGroup.getBrandGenericCode().isPresent())
			TransformerUtils.addInformation(eob,
					TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CCW_RX_BRAND_GENERIC,
							String.valueOf(claimGroup.getBrandGenericCode().get())));

		TransformerUtils.addInformation(eob,
				TransformerUtils.createCodeableConcept(TransformerConstants.EXTENSION_CODING_CMS_RX_PHARMACY_TYPE,
						claimGroup.getPharmacyTypeCode()));

		TransformerUtils.addInformation(eob,
				TransformerUtils.createCodeableConcept(TransformerConstants.CODING_CMS_RX_PATIENT_RESIDENCE,
						claimGroup.getPatientResidenceCode()));

		if (claimGroup.getSubmissionClarificationCode().isPresent())
			TransformerUtils.addInformation(eob,
					TransformerUtils.createCodeableConcept(
							TransformerConstants.CODING_CMS_RX_SUBMISSION_CLARIFICATION,
							claimGroup.getSubmissionClarificationCode().get()));

		return eob;
	}

}

